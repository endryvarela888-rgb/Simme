from django.conf import settings
from django.db import models


class ChefConversation(models.Model):
    PERSONAS = [("female", "Chef Sazón"), ("male", "Chef Mateo")]
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="chef_conversations")
    recipe = models.ForeignKey("recipes.Recipe", null=True, blank=True, on_delete=models.SET_NULL, related_name="chef_conversations")
    persona = models.CharField(max_length=12, choices=PERSONAS, default="female")
    title = models.CharField(max_length=180, default="New cooking chat")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ("-updated_at",)


class ChefMessage(models.Model):
    USER = "user"
    ASSISTANT = "assistant"
    ROLES = [(USER, "User"), (ASSISTANT, "Chef")]
    conversation = models.ForeignKey(ChefConversation, on_delete=models.CASCADE, related_name="messages")
    role = models.CharField(max_length=12, choices=ROLES)
    text = models.TextField(max_length=8000)
    tokens_in = models.PositiveIntegerField(default=0)
    tokens_out = models.PositiveIntegerField(default=0)
    cost_usd = models.DecimalField(max_digits=12, decimal_places=8, default=0)
    provider = models.CharField(max_length=32, default="openai")
    model = models.CharField(max_length=100, blank=True)
    cost_coins = models.PositiveSmallIntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ("created_at",)


class ChefMessageReport(models.Model):
    PENDING = "pending"
    REVIEWED = "reviewed"
    DISMISSED = "dismissed"
    STATUSES = [(PENDING, "Pending"), (REVIEWED, "Reviewed"), (DISMISSED, "Dismissed")]
    reporter = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="chef_message_reports")
    message = models.ForeignKey(ChefMessage, null=True, blank=True, on_delete=models.SET_NULL, related_name="reports")
    message_text = models.TextField(max_length=8000)
    reason = models.CharField(max_length=32)
    details = models.CharField(max_length=500, blank=True)
    status = models.CharField(max_length=12, choices=STATUSES, default=PENDING)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=("reporter", "message"), name="unique_chef_message_report_per_user")]
        ordering = ("-created_at",)


class MonthlyAIBudget(models.Model):
    month_start = models.DateField(unique=True)
    spent_usd = models.DecimalField(max_digits=12, decimal_places=8, default=0)
    reserved_usd = models.DecimalField(max_digits=12, decimal_places=8, default=0)
    updated_at = models.DateTimeField(auto_now=True)
