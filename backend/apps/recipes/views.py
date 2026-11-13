from rest_framework import generics, permissions, status
from rest_framework.response import Response
from rest_framework.views import APIView
from .models import Area, Category, Favorite, Recipe
from .serializers import AreaSerializer, CategorySerializer, RecipeSerializer

class CategoryList(generics.ListAPIView):
    queryset = Category.objects.order_by("name")
    serializer_class = CategorySerializer
class AreaList(generics.ListAPIView):
    queryset = Area.objects.order_by("name")
    serializer_class = AreaSerializer
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
