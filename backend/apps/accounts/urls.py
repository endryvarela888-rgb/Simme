from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from .views import (EmailTokenView, LogoutView, MeView, PasswordResetConfirmView,
                    PasswordResetRequestView, RegisterView, ResendVerificationView, VerifyEmailView,
                    AccountDeletionConfirmView, AccountDeletionRequestView, GoogleSignInView,
                    UGCBlockListView, UGCBlockView, UGCUserReportView, UGCTermsAcceptanceView)
urlpatterns = [path("register/", RegisterView.as_view()), path("login/", EmailTokenView.as_view()),
    path("google/", GoogleSignInView.as_view()),
    path("verify-email/", VerifyEmailView.as_view()), path("resend-verification/", ResendVerificationView.as_view()),
    path("password/forgot/", PasswordResetRequestView.as_view()), path("password/reset/", PasswordResetConfirmView.as_view()),
    path("account-deletion/request/", AccountDeletionRequestView.as_view()), path("account-deletion/confirm/", AccountDeletionConfirmView.as_view()),
    path("blocks/", UGCBlockListView.as_view()), path("blocks/<int:pk>/", UGCBlockView.as_view()),
    path("users/<int:pk>/report/", UGCUserReportView.as_view()), path("accept-ugc-terms/", UGCTermsAcceptanceView.as_view()),
    path("refresh/", TokenRefreshView.as_view()), path("logout/", LogoutView.as_view()), path("me/", MeView.as_view())]
