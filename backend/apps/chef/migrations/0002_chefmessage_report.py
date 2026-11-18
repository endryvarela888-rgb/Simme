from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("chef", "0001_initial"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="ChefMessageReport",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("reason", models.CharField(max_length=32)),
                ("details", models.CharField(blank=True, max_length=500)),
                ("message_text", models.TextField(max_length=8000)),
                ("status", models.CharField(choices=[("pending", "Pending"), ("reviewed", "Reviewed"), ("dismissed", "Dismissed")], default="pending", max_length=12)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("message", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="reports", to="chef.chefmessage")),
                ("reporter", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="chef_message_reports", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ("-created_at",)},
        ),
        migrations.AddConstraint(
            model_name="chefmessagereport",
            constraint=models.UniqueConstraint(fields=("reporter", "message"), name="unique_chef_message_report_per_user"),
        ),
    ]
