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
    source_name = models.CharField(max_length=120, default="TheMealDB")
    contributor_name = models.CharField(max_length=80, blank=True)
    creator = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.CASCADE, related_name="uploaded_recipes")
    uploaded_image = models.ImageField(upload_to="recipe_images/%Y/%m/", null=True, blank=True)
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

class RecipeVote(models.Model):
    LIKE = 1
    DISLIKE = -1
    VALUES = [(LIKE, "Like"), (DISLIKE, "Dislike")]
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="recipe_votes")
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name="votes")
    value = models.SmallIntegerField(choices=VALUES)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [models.UniqueConstraint(fields=("user", "recipe"), name="unique_vote_per_user_recipe")]

class RecipeComment(models.Model):
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name="comments")
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="recipe_comments")
    author_name = models.CharField(max_length=80)
    parent = models.ForeignKey("self", null=True, blank=True, on_delete=models.CASCADE, related_name="replies")
    text = models.TextField(max_length=2000)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ("created_at",)

class ContentReport(models.Model):
    PENDING = "pending"
    REVIEWED = "reviewed"
    DISMISSED = "dismissed"
    STATUS_CHOICES = [(PENDING, "Pending"), (REVIEWED, "Reviewed"), (DISMISSED, "Dismissed")]
    reporter = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="content_reports")
    recipe = models.ForeignKey(Recipe, null=True, blank=True, on_delete=models.CASCADE, related_name="reports")
    comment = models.ForeignKey(RecipeComment, null=True, blank=True, on_delete=models.CASCADE, related_name="reports")
    reason = models.CharField(max_length=40)
    details = models.CharField(max_length=500, blank=True)
    status = models.CharField(max_length=12, choices=STATUS_CHOICES, default=PENDING)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
        models.CheckConstraint(
            condition=(models.Q(recipe__isnull=False, comment__isnull=True) | models.Q(recipe__isnull=True, comment__isnull=False)),
            name="report_has_exactly_one_target",
        ),
        models.UniqueConstraint(fields=("reporter", "recipe"), condition=models.Q(recipe__isnull=False), name="unique_recipe_report_per_user"),
        models.UniqueConstraint(fields=("reporter", "comment"), condition=models.Q(comment__isnull=False), name="unique_comment_report_per_user"),
        ]
