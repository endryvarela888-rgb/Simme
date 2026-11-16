from django.contrib import admin
from .models import Area, Category, ContentReport, Favorite, Recipe, RecipeComment, RecipeIngredient, RecipeVote
admin.site.register([Area, Category, Recipe, RecipeIngredient, Favorite, RecipeVote, RecipeComment, ContentReport])
