from django.urls import path
from .views import AreaList, CategoryList, FavoriteCreate, FavoriteDelete, FavoriteList, RecipeDetail, RecipeList
urlpatterns = [path("categories/", CategoryList.as_view()), path("areas/", AreaList.as_view()),
    path("recipes/", RecipeList.as_view()), path("recipes/<int:pk>/", RecipeDetail.as_view()),
    path("favorites/", FavoriteList.as_view()), path("favorites/add/", FavoriteCreate.as_view()),
    path("favorites/<int:recipe_id>/", FavoriteDelete.as_view())]
