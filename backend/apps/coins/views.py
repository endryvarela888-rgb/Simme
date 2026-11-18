import json
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from django.conf import settings
from django.db import transaction
from django.utils import timezone
from google.auth.transport.requests import AuthorizedSession
from google.oauth2 import service_account
from rest_framework import permissions, status
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import CoinTransaction, Purchase, SubscriptionEntitlement, Wallet
from .serializers import CoinTransactionSerializer

PLAY_SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def get_wallet(user, lock=False):
    wallet, _ = Wallet.objects.get_or_create(user=user)
    return Wallet.objects.select_for_update().get(pk=wallet.pk) if lock else wallet


def play_session():
    if not settings.GOOGLE_PLAY_PRODUCTS_ENABLED or not settings.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON:
        raise RuntimeError("Google Play purchases are not configured on the Simmer server yet.")
    info = json.loads(settings.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON)
    credentials = service_account.Credentials.from_service_account_info(info, scopes=[PLAY_SCOPE])
    return AuthorizedSession(credentials)


class BalanceView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        wallet = get_wallet(request.user)
        entitlement = SubscriptionEntitlement.objects.filter(user=request.user).first()
        active = bool(entitlement and entitlement.active_until > timezone.now())
        return Response({"balance": wallet.balance, "subscription": {
            "tier": entitlement.tier if entitlement else "",
            "active": active,
            "active_until": entitlement.active_until.isoformat() if active else None,
        }})


class TransactionsView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        wallet = get_wallet(request.user)
        rows = wallet.transactions.all()[:100]
        return Response({"results": CoinTransactionSerializer(rows, many=True).data, "balance": wallet.balance})


class ProductsView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, _request):
        return Response({
            "enabled": settings.GOOGLE_PLAY_PRODUCTS_ENABLED,
            "currency": "Google Play local price",
            "coin_packs": settings.SIMMER_COIN_PRODUCTS,
            "subscriptions": settings.SIMMER_SUBSCRIPTION_PRODUCTS,
            "tiers": {"basic": {"daily_coins": 0}, "premium": {"daily_coins": 10}, "chef_elite": {"daily_coins": 20, "monthly_bonus": 100}},
        })


class PurchaseVerifyView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        product_id = str(request.data.get("product_id", ""))[:100]
        token = str(request.data.get("purchase_token", ""))
        if not product_id or not token or len(token) > 500:
            return Response({"detail": "A valid Google Play product and purchase token are required."}, status=status.HTTP_400_BAD_REQUEST)
        product = next((p for p in settings.SIMMER_COIN_PRODUCTS if p["product_id"] == product_id), None)
        if product is None:
            return Response({"detail": "Unknown coin product."}, status=status.HTTP_400_BAD_REQUEST)
        existing = Purchase.objects.filter(purchase_token=token).first()
        if existing:
            if existing.user_id != request.user.id or existing.product_id != product_id:
                return Response({"detail": "This purchase token is already linked to another account."}, status=status.HTTP_409_CONFLICT)
            if existing.status == Purchase.VERIFIED:
                return Response({"balance": get_wallet(request.user).balance, "credited": False, "coins": existing.coins})

        base = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{settings.GOOGLE_PLAY_PACKAGE_NAME}/purchases/products/{product_id}/tokens/{token}"
        try:
            google = play_session()
            verified = google.get(base, timeout=15)
            verified.raise_for_status()
            purchase_data = verified.json()
            if purchase_data.get("purchaseState") != 0 or purchase_data.get("consumptionState") == 1:
                return Response({"detail": "Google Play has not confirmed an unconsumed purchase."}, status=status.HTTP_400_BAD_REQUEST)
        except Exception as error:
            return Response({"detail": str(error) if isinstance(error, RuntimeError) else "Google Play could not verify this purchase. It has not been credited."}, status=status.HTTP_503_SERVICE_UNAVAILABLE)

        with transaction.atomic():
            purchase, _ = Purchase.objects.select_for_update().get_or_create(
                purchase_token=token,
                defaults={"user": request.user, "product_id": product_id, "coins": product["coins"]},
            )
            if purchase.user_id != request.user.id or purchase.product_id != product_id:
                return Response({"detail": "This purchase token is already linked to another account."}, status=status.HTTP_409_CONFLICT)
            if purchase.status != Purchase.VERIFIED:
                wallet = get_wallet(request.user, lock=True)
                wallet.balance += product["coins"]
                wallet.save(update_fields=("balance", "updated_at"))
                order_id = str(purchase_data.get("orderId", ""))
                CoinTransaction.objects.create(wallet=wallet, type=CoinTransaction.PURCHASE, amount=product["coins"], balance_after=wallet.balance,
                    reference=f"purchase:{token[:130]}", metadata={"product_id": product_id, "order_id": order_id})
                purchase.status = Purchase.VERIFIED
                purchase.order_id = order_id
                purchase.raw_response = purchase_data
                purchase.verified_at = timezone.now()
                purchase.save(update_fields=("status", "order_id", "raw_response", "verified_at"))
            balance = get_wallet(request.user).balance

        try:
            play_session().post(base + ":consume", timeout=15).raise_for_status()
        except Exception:
            # The purchase remains credited and the app can safely retry verification; tokens are idempotent.
            pass
        return Response({"balance": balance, "credited": True, "coins": product["coins"]})


class SubscriptionVerifyView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        product_id = str(request.data.get("product_id", ""))[:100]
        token = str(request.data.get("purchase_token", ""))
        product = next((p for p in settings.SIMMER_SUBSCRIPTION_PRODUCTS if p["product_id"] == product_id), None)
        if not product or not token or len(token) > 500:
            return Response({"detail": "A valid Google Play subscription and purchase token are required."}, status=status.HTTP_400_BAD_REQUEST)
        try:
            base = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{settings.GOOGLE_PLAY_PACKAGE_NAME}/purchases/subscriptionsv2/tokens/{token}"
            google = play_session()
            verified = google.get(base, timeout=15)
            verified.raise_for_status()
            data = verified.json()
            items = data.get("lineItems", [])
            line = next((item for item in items if item.get("productId") == product_id), None)
            if data.get("subscriptionState") not in ("SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD") or not line:
                return Response({"detail": "Google Play does not show this subscription as active."}, status=status.HTTP_400_BAD_REQUEST)
            expiry = datetime.fromisoformat(line["expiryTime"].replace("Z", "+00:00"))
            if expiry <= timezone.now():
                return Response({"detail": "This subscription has expired."}, status=status.HTTP_400_BAD_REQUEST)
        except Exception as error:
            return Response({"detail": str(error) if isinstance(error, RuntimeError) else "Google Play could not verify this subscription."}, status=status.HTTP_503_SERVICE_UNAVAILABLE)

        latest_order = str(line.get("latestSuccessfulOrderId", ""))
        with transaction.atomic():
            entitlement, _ = SubscriptionEntitlement.objects.select_for_update().get_or_create(
                user=request.user,
                defaults={"tier": product["tier"], "product_id": product_id, "purchase_token": token, "active_until": expiry},
            )
            if entitlement.purchase_token != token and entitlement.active_until > timezone.now():
                return Response({"detail": "Another active subscription is already linked to this account."}, status=status.HTTP_409_CONFLICT)
            entitlement.tier = product["tier"]
            entitlement.product_id = product_id
            entitlement.purchase_token = token
            entitlement.active_until = expiry
            entitlement.latest_order_id = latest_order
            entitlement.save()

            bonus = int(product.get("renewal_bonus", 0))
            credited = False
            if product["tier"] == "chef_elite" and bonus and latest_order:
                ref = f"subscription_bonus:{latest_order}"
                if not CoinTransaction.objects.filter(reference=ref).exists():
                    wallet = get_wallet(request.user, lock=True)
                    wallet.balance += bonus
                    wallet.save(update_fields=("balance", "updated_at"))
                    CoinTransaction.objects.create(wallet=wallet, type=CoinTransaction.PURCHASE, amount=bonus, balance_after=wallet.balance,
                        reference=ref, metadata={"product_id": product_id, "order_id": latest_order, "kind": "monthly_subscription_bonus"})
                    credited = True
            balance = get_wallet(request.user).balance

        # Acknowledge only a newly purchased active subscription; the API call is safe to retry.
        try:
            body = {"developerPayload": "simmer-verified"}
            ack = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{settings.GOOGLE_PLAY_PACKAGE_NAME}/purchases/subscriptions/{product_id}/tokens/{token}:acknowledge"
            play_session().post(ack, json=body, timeout=15)
        except Exception:
            pass
        return Response({"active": True, "tier": product["tier"], "active_until": expiry.isoformat(), "monthly_bonus_credited": credited, "balance": balance})


class ClaimDailyView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        from .models import DailyCoinClaim, SubscriptionEntitlement
        now = timezone.now()
        local_day = timezone.localtime(now, ZoneInfo(settings.SIMMER_REWARD_TIME_ZONE)).date()
        entitlement = SubscriptionEntitlement.objects.filter(user=request.user, active_until__gt=now).first()
        if not entitlement or entitlement.tier not in ("premium", "chef_elite"):
            return Response({"detail": "An active Premium or Chef Elite subscription is required."}, status=status.HTTP_403_FORBIDDEN)
        coins = next((p["daily_coins"] for p in settings.SIMMER_SUBSCRIPTION_PRODUCTS if p["tier"] == entitlement.tier), 0)
        if not coins:
            return Response({"detail": "This subscription does not include daily coins."}, status=status.HTTP_400_BAD_REQUEST)
        with transaction.atomic():
            wallet = get_wallet(request.user, lock=True)
            recent_claims = DailyCoinClaim.objects.filter(
                user=request.user,
                claimed_on__gte=local_day - timedelta(days=29),
                claimed_on__lte=local_day,
            ).count()
            if recent_claims >= 30:
                return Response({"detail": "The 30-day daily coin allowance has already been claimed.", "balance": wallet.balance}, status=status.HTTP_429_TOO_MANY_REQUESTS)
            claim, created = DailyCoinClaim.objects.get_or_create(user=request.user, claimed_on=local_day, defaults={"coins": coins})
            if not created:
                return Response({"detail": "Today's coins have already been claimed.", "balance": wallet.balance, "claimed": False}, status=status.HTTP_409_CONFLICT)
            wallet.balance += coins
            wallet.save(update_fields=("balance", "updated_at"))
            CoinTransaction.objects.create(wallet=wallet, type=CoinTransaction.DAILY_REWARD, amount=coins, balance_after=wallet.balance,
                reference=f"daily:{request.user.pk}:{local_day.isoformat()}", metadata={"tier": entitlement.tier})
        return Response({"balance": wallet.balance, "coins": coins, "claimed": True})
