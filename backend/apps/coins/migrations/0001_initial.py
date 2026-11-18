from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    initial = True
    dependencies = [migrations.swappable_dependency(settings.AUTH_USER_MODEL)]
    operations = [
        migrations.CreateModel(
            name="Wallet",
            fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                    ("balance", models.PositiveIntegerField(default=0)), ("updated_at", models.DateTimeField(auto_now=True)),
                    ("user", models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name="wallet", to=settings.AUTH_USER_MODEL))],
        ),
        migrations.CreateModel(
            name="Purchase",
            fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                    ("product_id", models.CharField(max_length=100)), ("purchase_token", models.CharField(max_length=500, unique=True)),
                    ("order_id", models.CharField(blank=True, max_length=180)),
                    ("status", models.CharField(choices=[("pending", "Pending"), ("verified", "Verified"), ("refunded", "Refunded"), ("invalid", "Invalid")], default="pending", max_length=12)),
                    ("coins", models.PositiveIntegerField(default=0)), ("raw_response", models.JSONField(blank=True, default=dict)),
                    ("created_at", models.DateTimeField(auto_now_add=True)), ("verified_at", models.DateTimeField(blank=True, null=True)),
                    ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="coin_purchases", to=settings.AUTH_USER_MODEL))],
        ),
        migrations.CreateModel(
            name="SubscriptionEntitlement",
            fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                    ("tier", models.CharField(default="basic", max_length=20)), ("product_id", models.CharField(max_length=100)),
                    ("purchase_token", models.CharField(max_length=500)), ("active_until", models.DateTimeField()),
                    ("latest_order_id", models.CharField(blank=True, max_length=180)), ("updated_at", models.DateTimeField(auto_now=True)),
                    ("user", models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name="simmer_subscription", to=settings.AUTH_USER_MODEL))],
        ),
        migrations.CreateModel(
            name="DailyCoinClaim",
            fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                    ("claimed_on", models.DateField()), ("coins", models.PositiveIntegerField()), ("created_at", models.DateTimeField(auto_now_add=True)),
                    ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="daily_coin_claims", to=settings.AUTH_USER_MODEL))],
        ),
        migrations.CreateModel(
            name="CoinTransaction",
            fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                    ("type", models.CharField(choices=[("purchase", "Purchase"), ("daily_reward", "Daily reward"), ("chat", "Chat"), ("refund", "Refund"), ("adjustment", "Adjustment")], max_length=20)),
                    ("amount", models.IntegerField()), ("balance_after", models.PositiveIntegerField()), ("reference", models.CharField(blank=True, max_length=180)),
                    ("metadata", models.JSONField(blank=True, default=dict)), ("created_at", models.DateTimeField(auto_now_add=True)),
                    ("wallet", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="transactions", to="coins.wallet"))],
            options={"ordering": ("-created_at",)},
        ),
        migrations.AddConstraint(model_name="dailycoinclaim", constraint=models.UniqueConstraint(fields=("user", "claimed_on"), name="unique_daily_coin_claim")),
        migrations.AddConstraint(model_name="cointransaction", constraint=models.UniqueConstraint(condition=~models.Q(reference=""), fields=("reference",), name="unique_coin_transaction_reference")),
    ]
