from rest_framework import generics, permissions, status
from rest_framework.pagination import PageNumberPagination
from rest_framework.response import Response
from rest_framework.views import APIView
from .models import Area, Category, Favorite, Recipe
from .serializers import AreaSerializer, CategorySerializer, RecipeSerializer

class CatalogPagination(PageNumberPagination):
    # The catalog is a small reference list (unlike recipes, which stay paginated).
    # Keep every category/region available to the mobile filter in the first response.
    page_size = 1000

class CategoryList(generics.ListAPIView):
    queryset = Category.objects.order_by("name")
    serializer_class = CategorySerializer
    pagination_class = CatalogPagination
class AreaList(generics.ListAPIView):
    queryset = Area.objects.order_by("name")
    serializer_class = AreaSerializer
    pagination_class = CatalogPagination
class RecipeList(generics.ListAPIView):
    serializer_class = RecipeSerializer
    def get_queryset(self):
        qs = Recipe.objects.filter(is_published=True).select_related("category", "area").prefetch_related("ingredients")
        p = self.request.query_params
        if p.get("category"): qs = qs.filter(category__slug=p["category"])
        if p.get("area"): qs = qs.filter(area__slug=p["area"])
        if p.get("ingredient"): qs = qs.filter(ingredients__name__icontains=p["ingredient"])
        if p.get("search"): qs = qs.filter(title__icontains=p["search"])
        return qs.distinct().order_by("title")

class PublicConfig(APIView):
    permission_classes = [permissions.AllowAny]
    def get(self, _request):
        from django.conf import settings
        return Response({"ads_enabled": settings.SIMMER_ADS_ENABLED, "ad_interval": settings.SIMMER_AD_INTERVAL})
class RecipeDetail(generics.RetrieveAPIView):
    queryset = Recipe.objects.filter(is_published=True).select_related("category", "area").prefetch_related("ingredients")
    serializer_class = RecipeSerializer
class FavoriteList(generics.ListAPIView):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = RecipeSerializer
    def get_queryset(self): return Recipe.objects.filter(favorite__user=self.request.user).select_related("category", "area").prefetch_related("ingredients")
    def post(self, request, *args, **kwargs):
        from django.shortcuts import get_object_or_404
        recipe = get_object_or_404(Recipe, pk=request.data.get("recipe_id"), is_published=True)
        Favorite.objects.get_or_create(user=request.user, recipe=recipe)
        return Response({"saved": True}, status=status.HTTP_200_OK)
class FavoriteCreate(APIView):
    permission_classes = [permissions.IsAuthenticated]
    def post(self, request):
        from django.shortcuts import get_object_or_404
        recipe = get_object_or_404(Recipe, pk=request.data.get("recipe_id"), is_published=True)
        Favorite.objects.get_or_create(user=request.user, recipe=recipe)
        return Response({"saved": True}, status=status.HTTP_200_OK)
class FavoriteDelete(APIView):
    permission_classes = [permissions.IsAuthenticated]
    def delete(self, request, recipe_id):
        Favorite.objects.filter(user=request.user, recipe_id=recipe_id).delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
