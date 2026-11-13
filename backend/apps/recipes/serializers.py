from rest_framework import serializers
from .models import Area, Category, Favorite, Recipe, RecipeIngredient

class CategorySerializer(serializers.ModelSerializer):
    class Meta: model = Category; fields = ("id", "name", "slug", "thumbnail", "description")
class AreaSerializer(serializers.ModelSerializer):
    class Meta: model = Area; fields = ("id", "name", "slug")
class IngredientSerializer(serializers.ModelSerializer):
    class Meta: model = RecipeIngredient; fields = ("name", "measure", "position")
class RecipeSerializer(serializers.ModelSerializer):
    category = serializers.CharField(source="category.name", read_only=True)
    area = serializers.CharField(source="area.name", read_only=True, allow_null=True)
    ingredients = IngredientSerializer(many=True, read_only=True)
    is_favorite = serializers.SerializerMethodField()
    class Meta:
        model = Recipe
        fields = ("id", "title", "slug", "category", "area", "instructions", "steps", "image", "thumbnail", "tags", "youtube_url", "ingredients", "is_favorite")
    def get_is_favorite(self, obj):
        user = self.context["request"].user
        return user.is_authenticated and Favorite.objects.filter(user=user, recipe=obj).exists()
