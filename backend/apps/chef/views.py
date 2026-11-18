from datetime import timedelta
from datetime import timedelta
from decimal import Decimal

import requests
from django.conf import settings
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import permissions, status
from rest_framework.response import Response
from rest_framework.throttling import ScopedRateThrottle
from rest_framework.views import APIView

from apps.coins.models import CoinTransaction, Wallet
from apps.coins.views import get_wallet
from apps.recipes.models import Recipe
from .models import ChefConversation, ChefMessage, ChefMessageReport, MonthlyAIBudget

RETENTION = timedelta(days=3)
MESSAGE_COST = 1
MAX_USER_TEXT = 2000
MAX_CONTEXT_CHARS = 16000
def maximum_request_cost():
    # Reserve a conservative input-token estimate plus the full configured output limit.
    input_tokens = (MAX_CONTEXT_CHARS + 1) // 2 + 1000
    output_tokens = max(1, int(settings.AI_MAX_OUTPUT_TOKENS))
    return (
        Decimal(input_tokens) * Decimal(str(settings.AI_INPUT_PRICE_PER_MILLION_USD))
        + Decimal(output_tokens) * Decimal(str(settings.AI_OUTPUT_PRICE_PER_MILLION_USD))
    ) / Decimal(1_000_000)


def message_json(message):
    return {"id": message.id, "role": message.role, "text": message.text, "created_at": message.created_at.isoformat(),
            "cost_coins": message.cost_coins}


def conversation_json(conversation, include_messages=True):
    result = {"id": conversation.id, "title": conversation.title, "persona": conversation.persona,
              "recipe_id": conversation.recipe_id, "recipe_title": conversation.recipe.title if conversation.recipe_id else "",
              "updated_at": conversation.updated_at.isoformat(), "expires_at": (conversation.updated_at + RETENTION).isoformat()}
    if include_messages:
        result["messages"] = [message_json(m) for m in conversation.messages.all()]
    return result


class ChefConversationList(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        ChefConversation.objects.filter(user=request.user, updated_at__lt=timezone.now() - RETENTION).delete()
        conversations = ChefConversation.objects.filter(user=request.user).select_related("recipe")[:50]
        return Response({"results": [conversation_json(c, include_messages=False) for c in conversations]})

    def post(self, request):
        persona = request.data.get("persona", "female")
        if persona not in ("female", "male"):
            return Response({"detail": "Choose a valid chef persona."}, status=status.HTTP_400_BAD_REQUEST)
        recipe = None
        recipe_id = request.data.get("recipe_id")
        if recipe_id:
            recipe = get_object_or_404(Recipe, pk=recipe_id, is_published=True)
        conversation = ChefConversation.objects.create(user=request.user, recipe=recipe, persona=persona,
            title=(recipe.title[:180] if recipe else "Cooking ideas"))
        return Response(conversation_json(conversation), status=status.HTTP_201_CREATED)


class ChefConversationDetail(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, pk):
        conversation = get_object_or_404(ChefConversation.objects.select_related("recipe"), pk=pk, user=request.user)
        if conversation.updated_at < timezone.now() - RETENTION:
            conversation.delete()
            return Response({"detail": "This chat has expired."}, status=status.HTTP_404_NOT_FOUND)
        return Response(conversation_json(conversation))


class ChefMessageCreate(APIView):
    permission_classes = [permissions.IsAuthenticated]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "chef_message"

    def post(self, request):
        text = str(request.data.get("text", "")).strip()
        if not text or len(text) > MAX_USER_TEXT:
            return Response({"detail": "Write a cooking question of up to 2,000 characters."}, status=status.HTTP_400_BAD_REQUEST)
        if (not settings.AI_API_KEY or not settings.AI_MODEL or settings.AI_MONTHLY_BUDGET_USD <= 0
                or settings.AI_INPUT_PRICE_PER_MILLION_USD <= 0 or settings.AI_OUTPUT_PRICE_PER_MILLION_USD <= 0):
            return Response({"detail": "The Simmer chef is not configured yet. Set the AI key, supported model, current input/output prices, and a monthly spending limit on the server."}, status=status.HTTP_503_SERVICE_UNAVAILABLE)
        now = timezone.now()
        conversation_id = request.data.get("conversation_id")
        persona = request.data.get("persona", "female")
        if persona not in ("female", "male"):
            persona = "female"
        recipe = None
        recipe_id = request.data.get("recipe_id")
        if recipe_id:
            recipe = get_object_or_404(Recipe, pk=recipe_id, is_published=True)
        if conversation_id:
            conversation = get_object_or_404(ChefConversation, pk=conversation_id, user=request.user)
            if conversation.updated_at < now - RETENTION:
                conversation.delete()
                return Response({"detail": "This chat has expired. Start a new cooking chat."}, status=status.HTTP_410_GONE)
            if recipe_id and conversation.recipe_id != recipe.id:
                return Response({"detail": "The selected recipe does not match this chat."}, status=status.HTTP_400_BAD_REQUEST)
        else:
            conversation = ChefConversation.objects.create(user=request.user, recipe=recipe, persona=persona,
                title=(text[:70] + ("…" if len(text) > 70 else "")))

        month_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        request_reserve = maximum_request_cost()
        day_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
        today_count = ChefMessage.objects.filter(conversation__user=request.user, role=ChefMessage.ASSISTANT, created_at__gte=day_start).count()
        if today_count >= settings.AI_MAX_DAILY_MESSAGES:
            return Response({"detail": "You have reached today's chef chat limit."}, status=status.HTTP_429_TOO_MANY_REQUESTS)

        with transaction.atomic():
            budget_row, _ = MonthlyAIBudget.objects.get_or_create(month_start=month_start.date())
            budget_row = MonthlyAIBudget.objects.select_for_update().get(pk=budget_row.pk)
            budget_limit = Decimal(str(settings.AI_MONTHLY_BUDGET_USD))
            if budget_row.spent_usd + budget_row.reserved_usd + request_reserve > budget_limit:
                return Response({"detail": "The Simmer chef has reached its monthly service budget. Please try again next month."}, status=status.HTTP_429_TOO_MANY_REQUESTS)
            wallet = get_wallet(request.user, lock=True)
            if wallet.balance < MESSAGE_COST:
                return Response({"detail": "You need one coin to ask the chef.", "code": "insufficient_coins", "balance": wallet.balance}, status=status.HTTP_402_PAYMENT_REQUIRED)
            budget_row.reserved_usd += request_reserve
            budget_row.save(update_fields=("reserved_usd", "updated_at"))
            wallet.balance -= MESSAGE_COST
            wallet.save(update_fields=("balance", "updated_at"))
            CoinTransaction.objects.create(wallet=wallet, type=CoinTransaction.CHAT, amount=-MESSAGE_COST, balance_after=wallet.balance,
                reference=f"chat:{conversation.pk}:{now.timestamp()}", metadata={"conversation_id": conversation.pk})
            user_message = ChefMessage.objects.create(conversation=conversation, role=ChefMessage.USER, text=text)
            conversation.persona = persona
            conversation.updated_at = now
            conversation.save(update_fields=("persona", "updated_at"))

        try:
            system = (
                "You are Simmer's cooking-only chef assistant. Reply in the user's language. Be warm, safe, practical and concise. "
                "Only help with cooking, food, beverages, kitchen techniques, utensils and meal planning. If asked about anything else, gently say you can help only with cooking. "
                "Never give medical advice. Respect allergies and dietary restrictions. For food safety, choose cautious guidance. "
                + ("Your style is sweet, cute, encouraging and affectionate, without being childish. " if persona == "female" else "Your style is composed, professional and trustworthy, while remaining kind.")
            )
            prompt_messages = [{"role": "system", "content": system}]
            if conversation.recipe_id:
                r = conversation.recipe
                ingredients = ", ".join(f"{i.measure} {i.name}".strip() for i in r.ingredients.all())[:3500]
                steps = "\n".join(r.steps or [])[:3500]
                prompt_messages.append({"role": "system", "content": f"Recipe context (stay grounded in this recipe): {r.title}\nIngredients: {ingredients}\nSteps: {steps}"})
            past = list(conversation.messages.exclude(pk=user_message.pk).order_by("-created_at")[:8])
            for previous in reversed(past):
                prompt_messages.append({"role": previous.role, "content": previous.text[:1200]})
            prompt_messages.append({"role": "user", "content": text})
            while sum(len(m["content"]) for m in prompt_messages) > MAX_CONTEXT_CHARS and len(prompt_messages) > 3:
                prompt_messages.pop(2 if len(prompt_messages) > 3 else 1)

            response = requests.post(
                settings.AI_API_BASE_URL.rstrip("/") + "/responses",
                headers={"Authorization": f"Bearer {settings.AI_API_KEY}", "Content-Type": "application/json"},
                json={"model": settings.AI_MODEL, "input": prompt_messages, "max_output_tokens": settings.AI_MAX_OUTPUT_TOKENS, "store": False},
                timeout=(10, 45),
            )
            response.raise_for_status()
            payload = response.json()
            output = "\n".join(item.get("text", "") for row in payload.get("output", []) for item in row.get("content", []) if item.get("type") == "output_text").strip()
            if not output:
                raise RuntimeError("The chef returned an empty answer. Please try again.")
            input_tokens = int(payload.get("usage", {}).get("input_tokens", 0))
            output_tokens = int(payload.get("usage", {}).get("output_tokens", 0))
            cost = Decimal(input_tokens) * Decimal(str(settings.AI_INPUT_PRICE_PER_MILLION_USD)) / Decimal(1_000_000) + Decimal(output_tokens) * Decimal(str(settings.AI_OUTPUT_PRICE_PER_MILLION_USD)) / Decimal(1_000_000)
            answer = ChefMessage.objects.create(conversation=conversation, role=ChefMessage.ASSISTANT, text=output[:8000],
                tokens_in=input_tokens, tokens_out=output_tokens, cost_usd=cost, provider=settings.AI_PROVIDER, model=settings.AI_MODEL, cost_coins=MESSAGE_COST)
            with transaction.atomic():
                budget_row = MonthlyAIBudget.objects.select_for_update().get(month_start=month_start.date())
                budget_row.reserved_usd = max(Decimal("0"), budget_row.reserved_usd - request_reserve)
                budget_row.spent_usd += cost
                budget_row.save(update_fields=("spent_usd", "reserved_usd", "updated_at"))
            conversation.save(update_fields=("updated_at",))
            return Response({"conversation": conversation_json(conversation), "user_message": message_json(user_message),
                             "assistant_message": message_json(answer), "balance": get_wallet(request.user).balance,
                             "usage": {"input_tokens": input_tokens, "output_tokens": output_tokens, "cost_usd": str(cost)}})
        except Exception as error:
            with transaction.atomic():
                budget_row = MonthlyAIBudget.objects.select_for_update().filter(month_start=month_start.date()).first()
                if budget_row:
                    budget_row.reserved_usd = max(Decimal("0"), budget_row.reserved_usd - request_reserve)
                    budget_row.save(update_fields=("reserved_usd", "updated_at"))
                wallet = get_wallet(request.user, lock=True)
                wallet.balance += MESSAGE_COST
                wallet.save(update_fields=("balance", "updated_at"))
                CoinTransaction.objects.create(wallet=wallet, type=CoinTransaction.REFUND, amount=MESSAGE_COST, balance_after=wallet.balance,
                    reference=f"chat-refund:{conversation.pk}:{user_message.pk}", metadata={"reason": "provider_failure"})
                user_message.delete()
            detail = str(error) if isinstance(error, RuntimeError) else "The chef is temporarily unavailable. Your coin was returned; please try again."
            return Response({"detail": detail}, status=status.HTTP_502_BAD_GATEWAY)


class ChefMessageReportView(APIView):
    permission_classes = [permissions.IsAuthenticated]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "report_write"

    def post(self, request, pk):
        message = get_object_or_404(ChefMessage, pk=pk, role=ChefMessage.ASSISTANT, conversation__user=request.user)
        reason = str(request.data.get("reason", "inappropriate"))[:32]
        allowed = {"inappropriate", "unsafe", "off_topic", "other"}
        if reason not in allowed:
            return Response({"detail": "Choose a valid report reason."}, status=status.HTTP_400_BAD_REQUEST)
        details = str(request.data.get("details", ""))[:500]
        report, created = ChefMessageReport.objects.get_or_create(
            reporter=request.user, message=message, defaults={"reason": reason, "details": details, "message_text": message.text},
        )
        return Response({"detail": "Thanks. The AI response was sent to the moderation queue."}, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)
