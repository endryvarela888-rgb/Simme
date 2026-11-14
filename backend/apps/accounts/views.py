import secrets
from datetime import timedelta

from django.conf import settings
from django.contrib.auth.hashers import check_password, make_password
from django.core.mail import send_mail
from django.db import IntegrityError, transaction
from django.utils import timezone
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token as google_id_token
from rest_framework import generics, permissions, status
from rest_framework.response import Response
from rest_framework.throttling import ScopedRateThrottle
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView, TokenBlacklistView

from .models import EmailCode, User
from .serializers import EmailAddressSerializer, EmailCodeSerializer, PasswordResetSerializer, RegisterSerializer

CODE_LIFETIME = timedelta(minutes=15)
RESEND_INTERVAL = timedelta(seconds=60)
MAX_CODE_ATTEMPTS = 5


def email_delivery_ready():
    if settings.EMAIL_BACKEND.endswith("console.EmailBackend"):
        return settings.DEBUG
    values = (settings.EMAIL_HOST, settings.EMAIL_HOST_USER, settings.EMAIL_HOST_PASSWORD,
              settings.DEFAULT_FROM_EMAIL)
    placeholders = ("replace", "example.com", "changeme")
    return all(value and not any(marker in value.lower() for marker in placeholders) for value in values)


def send_email_code(user, purpose):
    if not email_delivery_ready():
        return False, "Email delivery is not configured yet."
    now = timezone.now()
    code_record, _ = EmailCode.objects.get_or_create(
        user=user, purpose=purpose,
        defaults={"code_hash": "", "expires_at": now, "sent_at": now - RESEND_INTERVAL, "attempts": 0},
    )
    if now - code_record.sent_at < RESEND_INTERVAL:
        return False, "Please wait before requesting another code."
    code = f"{secrets.randbelow(1_000_000):06d}"
    code_record.code_hash = make_password(code)
    code_record.expires_at = now + CODE_LIFETIME
    code_record.sent_at = now
    code_record.attempts = 0
    code_record.save(update_fields=("code_hash", "expires_at", "sent_at", "attempts"))
    title = {EmailCode.VERIFY_EMAIL: "Verify your Simmer email",
             EmailCode.RESET_PASSWORD: "Reset your Simmer password",
             EmailCode.DELETE_ACCOUNT: "Confirm Simmer account deletion"}[purpose]
    message = (
        f"Your Simmer verification code is {code}. It expires in 15 minutes.\n\n"
        "If you did not request this, you can ignore this email."
    )
    try:
        send_mail(title, message, settings.DEFAULT_FROM_EMAIL, [user.email], fail_silently=False)
    except Exception:
        return False, "We could not send the email. Please try again later."
    return True, ""


def issue_tokens(user):
    refresh = RefreshToken.for_user(user)
    return {"access": str(refresh.access_token), "refresh": str(refresh)}


class RegisterView(generics.GenericAPIView):
    queryset = User.objects.all()
    serializer_class = RegisterSerializer
    permission_classes = [permissions.AllowAny]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "register"

    def post(self, request):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        if not email_delivery_ready():
            return Response({"detail": "Account verification email is not configured yet."}, status=503)
        user = serializer.save(is_active=False, email_verified=False)
        sent, error = send_email_code(user, EmailCode.VERIFY_EMAIL)
        if not sent:
            return Response({"detail": error, "email": user.email}, status=503)
        return Response({"detail": "A verification code was sent to your email.", "email": user.email}, status=201)


class VerifyEmailView(APIView):
    permission_classes = [permissions.AllowAny]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "email_code"

    def post(self, request):
        serializer = EmailCodeSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        email, code = serializer.validated_data["email"], serializer.validated_data["code"]
        user = User.objects.filter(email=email, is_active=False).first()
        record = EmailCode.objects.filter(user=user, purpose=EmailCode.VERIFY_EMAIL).first() if user else None
        if not record or record.expires_at <= timezone.now() or record.attempts >= MAX_CODE_ATTEMPTS:
            return Response({"detail": "The code is invalid or expired. Request a new one."}, status=400)
        if not check_password(code, record.code_hash):
            record.attempts += 1
            record.save(update_fields=("attempts",))
            return Response({"detail": "The code is invalid or expired. Request a new one."}, status=400)
        user.email_verified = True
        user.is_active = True
        user.save(update_fields=("email_verified", "is_active"))
        EmailCode.objects.filter(user=user, purpose=EmailCode.VERIFY_EMAIL).delete()
        return Response({"detail": "Email verified.", **issue_tokens(user)})


class ResendVerificationView(APIView):
    permission_classes = [permissions.AllowAny]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "email_code"

    def post(self, request):
        serializer = EmailAddressSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = User.objects.filter(email=serializer.validated_data["email"], is_active=False).first()
        if user:
            sent, error = send_email_code(user, EmailCode.VERIFY_EMAIL)
            if not sent and error == "Please wait before requesting another code.":
                return Response({"detail": error}, status=429)
        return Response({"detail": "If an unverified account exists, a new code will be sent."})


class EmailTokenView(TokenObtainPairView):
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "login"


class GoogleSignInView(APIView):
    permission_classes = [permissions.AllowAny]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "login"

    def post(self, request):
        token = request.data.get("id_token", "")
        if not isinstance(token, str) or not token:
            return Response({"detail": "A Google ID token is required."}, status=400)
        if not settings.GOOGLE_OAUTH_CLIENT_IDS:
            return Response({"detail": "Google sign-in is not configured yet."}, status=503)
        try:
            claims = google_id_token.verify_oauth2_token(token, google_requests.Request(), audience=None)
        except (ValueError, KeyError):
            return Response({"detail": "The Google ID token is invalid or expired."}, status=401)
        audience = claims.get("aud")
        audiences = {audience} if isinstance(audience, str) else set(audience or [])
        if not audiences.intersection(settings.GOOGLE_OAUTH_CLIENT_IDS):
            return Response({"detail": "The Google ID token was issued for another app."}, status=401)
        if claims.get("iss") not in ("accounts.google.com", "https://accounts.google.com") or claims.get("email_verified") is not True:
            return Response({"detail": "Google must provide a verified email address."}, status=401)
        email = str(claims.get("email", "")).strip().lower()
        subject = claims.get("sub")
        if not email or not subject:
            return Response({"detail": "The Google account did not provide the required identity."}, status=401)
        try:
            with transaction.atomic():
                user = User.objects.select_for_update().filter(google_sub=subject).first()
                if user is None:
                    user = User.objects.select_for_update().filter(email=email).first()
                if user is None:
                    user = User.objects.create_user(email=email, display_name=claims.get("name", "")[:80],
                                                    auth_provider="google", google_sub=subject,
                                                    email_verified=True, is_active=True)
                else:
                    if user.google_sub and user.google_sub != subject:
                        return Response({"detail": "This email is linked to a different Google account."}, status=409)
                    user.google_sub = subject
                    user.email_verified = True
                    user.is_active = True
                    if not user.display_name:
                        user.display_name = claims.get("name", "")[:80]
                    user.save(update_fields=("google_sub", "email_verified", "is_active", "display_name"))
        except IntegrityError:
            return Response({"detail": "Could not link this Google account. Please try again."}, status=409)
        return Response(issue_tokens(user))


class PasswordResetRequestView(APIView):
    permission_classes = [permissions.AllowAny]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "email_code"

    def post(self, request):
        serializer = EmailAddressSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = User.objects.filter(email=serializer.validated_data["email"], is_active=True).first()
        if user:
            send_email_code(user, EmailCode.RESET_PASSWORD)
        return Response({"detail": "If the account exists, password reset instructions will be sent."})


class PasswordResetConfirmView(APIView):
    permission_classes = [permissions.AllowAny]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "email_code"

    def post(self, request):
        serializer = PasswordResetSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        user = User.objects.filter(email=data["email"], is_active=True).first()
        record = EmailCode.objects.filter(user=user, purpose=EmailCode.RESET_PASSWORD).first() if user else None
        if not record or record.expires_at <= timezone.now() or record.attempts >= MAX_CODE_ATTEMPTS:
            return Response({"detail": "The code is invalid or expired. Request a new one."}, status=400)
        if not check_password(data["code"], record.code_hash):
            record.attempts += 1
            record.save(update_fields=("attempts",))
            return Response({"detail": "The code is invalid or expired. Request a new one."}, status=400)
        user.set_password(data["new_password"])
        user.save(update_fields=("password",))
        EmailCode.objects.filter(user=user, purpose=EmailCode.RESET_PASSWORD).delete()
        return Response({"detail": "Password changed. You can sign in now."})


class AccountDeletionRequestView(PasswordResetRequestView):
    def post(self, request):
        serializer = EmailAddressSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = User.objects.filter(email=serializer.validated_data["email"], is_active=True).first()
        if user:
            send_email_code(user, EmailCode.DELETE_ACCOUNT)
        return Response({"detail": "If the account exists, deletion instructions will be sent."})


class AccountDeletionConfirmView(APIView):
    permission_classes = [permissions.AllowAny]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "email_code"

    def post(self, request):
        serializer = EmailCodeSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        user = User.objects.filter(email=data["email"], is_active=True).first()
        record = EmailCode.objects.filter(user=user, purpose=EmailCode.DELETE_ACCOUNT).first() if user else None
        if not record or record.expires_at <= timezone.now() or record.attempts >= MAX_CODE_ATTEMPTS:
            return Response({"detail": "The code is invalid or expired. Request a new one."}, status=400)
        if not check_password(data["code"], record.code_hash):
            record.attempts += 1
            record.save(update_fields=("attempts",))
            return Response({"detail": "The code is invalid or expired. Request a new one."}, status=400)
        user.delete()
        return Response({"detail": "Account and associated data deleted."})


class MeView(generics.GenericAPIView):
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request):
        user = request.user
        return Response({"id": user.id, "email": user.email, "display_name": user.display_name,
                         "email_verified": user.email_verified})

    def delete(self, request):
        password = request.data.get("password", "")
        if not password or not request.user.check_password(password):
            return Response({"detail": "Enter your password to confirm account deletion."}, status=400)
        request.user.delete()
        return Response(status=204)


class LogoutView(TokenBlacklistView):
    permission_classes = [permissions.IsAuthenticated]
