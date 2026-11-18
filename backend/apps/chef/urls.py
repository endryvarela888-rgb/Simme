from django.urls import path
from .views import ChefConversationDetail, ChefConversationList, ChefMessageCreate, ChefMessageReportView

urlpatterns = [
    path("conversations/", ChefConversationList.as_view()),
    path("conversations/<int:pk>/", ChefConversationDetail.as_view()),
    path("messages/", ChefMessageCreate.as_view()),
    path("messages/<int:pk>/report/", ChefMessageReportView.as_view()),
]
