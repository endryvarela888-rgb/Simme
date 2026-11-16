from django.contrib.auth.models import AbstractUser, BaseUserManager
from django.conf import settings
from django.db import models

class UserManager(BaseUserManager):
    use_in_migrations = True
    def _create_user(self, email, password, **extra_fields):
        if not email: raise ValueError("Email is required")
        user = self.model(email=self.normalize_email(email).lower(), **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user
    def create_user(self, email, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", False)
        extra_fields.setdefault("is_superuser", False)
        return self._create_user(email, password, **extra_fields)
    def create_superuser(self, email, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)
        return self._create_user(email, password, **extra_fields)

class User(AbstractUser):
    username = None
    email = models.EmailField(unique=True)
    email_verified = models.BooleanField(default=False)
    display_name = models.CharField(max_length=80, blank=True)
    avatar = models.ImageField(upload_to="avatars/", blank=True)
    auth_provider = models.CharField(max_length=12, default="email")
    google_sub = models.CharField(max_length=255, unique=True, null=True, blank=True)
    preferred_language = models.CharField(max_length=5, default="en")
    created_at = models.DateTimeField(auto_now_add=True)
    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = []
    objects = UserManager()


class EmailCode(models.Model):
    VERIFY_EMAIL = "verify_email"
    RESET_PASSWORD = "reset_password"
    DELETE_ACCOUNT = "delete_account"
    PURPOSES = [(VERIFY_EMAIL, "Verify email"), (RESET_PASSWORD, "Reset password"), (DELETE_ACCOUNT, "Delete account")]

    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="email_codes")
    purpose = models.CharField(max_length=20, choices=PURPOSES)
    code_hash = models.CharField(max_length=128)
    expires_at = models.DateTimeField()
    sent_at = models.DateTimeField()
    attempts = models.PositiveSmallIntegerField(default=0)

    class Meta:
        constraints = [models.UniqueConstraint(fields=("user", "purpose"), name="unique_email_code_per_purpose")]
