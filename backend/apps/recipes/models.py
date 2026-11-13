from django.conf import settings
from django.db import models

class Category(models.Model):
    external_id = models.CharField(max_length=40, unique=True)
    name = models.CharField(max_length=100, unique=True)
    slug = models.SlugField(unique=True)
    thumbnail = models.URLField(blank=True)
    description = models.TextField(blank=True)
    def __str__(self): return self.name

class Area(models.Model):
    name = models.CharField(max_length=100, unique=True)
    slug = models.SlugField(unique=True)
    def __str__(self): return self.name

class Recipe(models.Model):
    external_id = models.CharField(max_length=40, unique=True)
    title = models.CharField(max_length=300, db_index=True)
    slug = models.SlugField(max_length=220)
    category = models.ForeignKey(Category, on_delete=models.PROTECT, related_name="recipes")
    area = models.ForeignKey(Area, null=True, blank=True, on_delete=models.SET_NULL)
    instructions = models.TextField()
    steps = models.JSONField(default=list)
    image = models.URLField()
    thumbnail = models.URLField()
    tags = models.JSONField(default=list, blank=True)
    youtube_url = models.URLField(max_length=500, blank=True)
    source_url = models.URLField(blank=True)
    is_published = models.BooleanField(default=True)
    updated_at = models.DateTimeField(auto_now=True)
    def __str__(self): return self.title

class RecipeIngredient(models.Model):
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name="ingredients")
    position = models.PositiveSmallIntegerField()
    name = models.CharField(max_length=120, db_index=True)
    measure = models.CharField(max_length=80, blank=True)
    class Meta:
        ordering = ["position"]
        unique_together = [("recipe", "position")]

class Favorite(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE)
    created_at = models.DateTimeField(auto_now_add=True)
    class Meta: unique_together = [("user", "recipe")]
