from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    initial = True
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("recipes", "0004_community_recipes"),
    ]
    operations = [
        migrations.CreateModel(
            name="ChefConversation",
            fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                    ("persona", models.CharField(choices=[("female", "Chef Sazón"), ("male", "Chef Mateo")], default="female", max_length=12)),
                    ("title", models.CharField(default="New cooking chat", max_length=180)), ("created_at", models.DateTimeField(auto_now_add=True)),
                    ("updated_at", models.DateTimeField(auto_now=True)),
                    ("recipe", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="chef_conversations", to="recipes.recipe")),
                    ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="chef_conversations", to=settings.AUTH_USER_MODEL))],
            options={"ordering": ("-updated_at",)},
        ),
        migrations.CreateModel(
            name="ChefMessage",
            fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                    ("role", models.CharField(choices=[("user", "User"), ("assistant", "Chef")], max_length=12)),
                    ("text", models.TextField(max_length=8000)), ("tokens_in", models.PositiveIntegerField(default=0)),
                    ("tokens_out", models.PositiveIntegerField(default=0)), ("cost_usd", models.DecimalField(decimal_places=8, default=0, max_digits=12)),
                    ("provider", models.CharField(default="openai", max_length=32)), ("model", models.CharField(blank=True, max_length=100)),
                    ("cost_coins", models.PositiveSmallIntegerField(default=0)), ("created_at", models.DateTimeField(auto_now_add=True)),
                    ("conversation", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="messages", to="chef.chefconversation"))],
            options={"ordering": ("created_at",)},
        ),
        migrations.CreateModel(
            name="MonthlyAIBudget",
            fields=[("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                    ("month_start", models.DateField(unique=True)), ("spent_usd", models.DecimalField(decimal_places=8, default=0, max_digits=12)),
                    ("reserved_usd", models.DecimalField(decimal_places=8, default=0, max_digits=12)), ("updated_at", models.DateTimeField(auto_now=True))],
        ),
    ]
