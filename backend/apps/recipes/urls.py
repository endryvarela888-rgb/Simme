from django.urls import path
from .views import (AreaList, CategoryList, CommentReportView, FavoriteCreate, FavoriteDelete, FavoriteList,
                    PublicConfig, RecipeCommentListCreate, RecipeCreate, RecipeDetail, RecipeList, RecipeReportView,
                    RecipeVoteView)
urlpatterns = [path("config/", PublicConfig.as_view()), path("categories/", CategoryList.as_view()), path("areas/", AreaList.as_view()),
    path("recipes/", RecipeList.as_view()), path("recipes/create/", RecipeCreate.as_view()),
    path("recipes/<int:pk>/", RecipeDetail.as_view()), path("recipes/<int:pk>/vote/", RecipeVoteView.as_view()),
    path("recipes/<int:pk>/comments/", RecipeCommentListCreate.as_view()), path("recipes/<int:pk>/report/", RecipeReportView.as_view()),
    path("comments/<int:pk>/report/", CommentReportView.as_view()),
    path("favorites/", FavoriteList.as_view()), path("favorites/add/", FavoriteCreate.as_view()),
    path("favorites/<int:recipe_id>/", FavoriteDelete.as_view())]
