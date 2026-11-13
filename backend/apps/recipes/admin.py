from django.contrib import admin
from .models import Area, Category, Favorite, Recipe, RecipeIngredient
admin.site.register([Area, Category, Recipe, RecipeIngredient, Favorite])
