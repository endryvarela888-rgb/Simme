from django.conf import settings
from django.db import models


class Wallet(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="wallet")
    balance = models.PositiveIntegerField(default=0)
    updated_at = models.DateTimeField(auto_now=True)


class CoinTransaction(models.Model):
    PURCHASE = "purchase"
    DAILY_REWARD = "daily_reward"
    CHAT = "chat"
    REFUND = "refund"
    ADJUSTMENT = "adjustment"
    TYPES = [(value, value.replace("_", " ").title()) for value in (PURCHASE, DAILY_REWARD, CHAT, REFUND, ADJUSTMENT)]
    wallet = models.ForeignKey(Wallet, on_delete=models.CASCADE, related_name="transactions")
    type = models.CharField(max_length=20, choices=TYPES)
    amount = models.IntegerField()
    balance_after = models.PositiveIntegerField()
    reference = models.CharField(max_length=180, blank=True)
    metadata = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("-created_at",)
        constraints = [models.UniqueConstraint(fields=("reference",), condition=~models.Q(reference=""), name="unique_coin_transaction_reference")]


class Purchase(models.Model):
    PENDING = "pending"
    VERIFIED = "verified"
    REFUNDED = "refunded"
    INVALID = "invalid"
    STATUS = [(value, value.title()) for value in (PENDING, VERIFIED, REFUNDED, INVALID)]
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="coin_purchases")
    product_id = models.CharField(max_length=100)
    purchase_token = models.CharField(max_length=500, unique=True)
    order_id = models.CharField(max_length=180, blank=True)
    status = models.CharField(max_length=12, choices=STATUS, default=PENDING)
    coins = models.PositiveIntegerField(default=0)
    raw_response = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    verified_at = models.DateTimeField(null=True, blank=True)


class SubscriptionEntitlement(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="simmer_subscription")
    tier = models.CharField(max_length=20, default="basic")
    product_id = models.CharField(max_length=100)
    purchase_token = models.CharField(max_length=500)
    active_until = models.DateTimeField()
    latest_order_id = models.CharField(max_length=180, blank=True)
    updated_at = models.DateTimeField(auto_now=True)


class DailyCoinClaim(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="daily_coin_claims")
    claimed_on = models.DateField()
    coins = models.PositiveIntegerField()
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=("user", "claimed_on"), name="unique_daily_coin_claim")]
