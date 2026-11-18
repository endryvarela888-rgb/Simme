from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0003_user_avatar"),
    ]

    operations = [
        migrations.AddField(
            model_name="user",
            name="ugc_terms_accepted_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.CreateModel(
            name="UserBlock",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("blocked", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="blocked_by_users", to=settings.AUTH_USER_MODEL)),
                ("blocker", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="blocked_users", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.AddConstraint(
            model_name="userblock",
            constraint=models.UniqueConstraint(fields=("blocker", "blocked"), name="unique_user_block"),
        ),
        migrations.CreateModel(
            name="UserReport",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("reason", models.CharField(max_length=40)),
                ("details", models.CharField(blank=True, max_length=500)),
                ("status", models.CharField(choices=[("pending", "Pending"), ("reviewed", "Reviewed"), ("dismissed", "Dismissed")], default="pending", max_length=12)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("reporter", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="user_reports_sent", to=settings.AUTH_USER_MODEL)),
                ("target", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="user_reports_received", to=settings.AUTH_USER_MODEL)),
            ],
            options={"ordering": ("-created_at",)},
        ),
        migrations.AddConstraint(
            model_name="userreport",
            constraint=models.UniqueConstraint(fields=("reporter", "target"), name="unique_user_report"),
        ),
    ]
