from django.urls import path
from .views import BalanceView, ClaimDailyView, ProductsView, PurchaseVerifyView, SubscriptionVerifyView, TransactionsView

urlpatterns = [
    path("balance/", BalanceView.as_view()),
    path("transactions/", TransactionsView.as_view()),
    path("products/", ProductsView.as_view()),
    path("purchase/verify/", PurchaseVerifyView.as_view()),
    path("subscriptions/verify/", SubscriptionVerifyView.as_view()),
    path("claim-daily/", ClaimDailyView.as_view()),
]
