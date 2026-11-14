from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


def mark_existing_accounts_verified(apps, schema_editor):
    User = apps.get_model("accounts", "User")
    User.objects.filter(is_active=True).update(email_verified=True)


class Migration(migrations.Migration):
    dependencies = [("accounts", "0001_initial")]

    operations = [
        migrations.AddField(
            model_name="user",
            name="email_verified",
            field=models.BooleanField(default=False),
        ),
        migrations.RunPython(mark_existing_accounts_verified, migrations.RunPython.noop),
        migrations.CreateModel(
            name="EmailCode",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("purpose", models.CharField(choices=[("verify_email", "Verify email"), ("reset_password", "Reset password"), ("delete_account", "Delete account")], max_length=20)),
                ("code_hash", models.CharField(max_length=128)),
                ("expires_at", models.DateTimeField()),
                ("sent_at", models.DateTimeField()),
                ("attempts", models.PositiveSmallIntegerField(default=0)),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="email_codes", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.AddConstraint(
            model_name="emailcode",
            constraint=models.UniqueConstraint(fields=("user", "purpose"), name="unique_email_code_per_purpose"),
        ),
    ]
