import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


def add_unspecified_category(apps, schema_editor):
    Category = apps.get_model("recipes", "Category")
    Category.objects.using(schema_editor.connection.alias).get_or_create(
        slug="unspecified",
        defaults={"external_id": "simmer-unspecified", "name": "Unspecified"},
    )


class Migration(migrations.Migration):
    dependencies = [
        ("recipes", "0003_alter_recipe_youtube_url"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AddField(
            model_name="recipe", name="source_name",
            field=models.CharField(default="TheMealDB", max_length=120),
        ),
        migrations.AddField(
            model_name="recipe", name="contributor_name",
            field=models.CharField(blank=True, max_length=80),
        ),
        migrations.AddField(
            model_name="recipe", name="creator",
            field=models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.CASCADE,
                                    related_name="uploaded_recipes", to=settings.AUTH_USER_MODEL),
        ),
        migrations.AddField(
            model_name="recipe", name="uploaded_image",
            field=models.ImageField(blank=True, null=True, upload_to="recipe_images/%Y/%m/"),
        ),
        migrations.RunPython(add_unspecified_category, migrations.RunPython.noop),
        migrations.CreateModel(
            name="RecipeVote",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("value", models.SmallIntegerField(choices=[(1, "Like"), (-1, "Dislike")])),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("recipe", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="votes", to="recipes.recipe")),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="recipe_votes", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.AddConstraint(
            model_name="recipevote",
            constraint=models.UniqueConstraint(fields=("user", "recipe"), name="unique_vote_per_user_recipe"),
        ),
        migrations.CreateModel(
            name="RecipeComment",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("author_name", models.CharField(max_length=80)),
                ("text", models.TextField(max_length=2000)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("author", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="recipe_comments", to=settings.AUTH_USER_MODEL)),
                ("parent", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.CASCADE, related_name="replies", to="recipes.recipecomment")),
                ("recipe", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="comments", to="recipes.recipe")),
            ],
            options={"ordering": ("created_at",)},
        ),
        migrations.CreateModel(
            name="ContentReport",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("reason", models.CharField(max_length=40)),
                ("details", models.CharField(blank=True, max_length=500)),
                ("status", models.CharField(choices=[("pending", "Pending"), ("reviewed", "Reviewed"), ("dismissed", "Dismissed")], default="pending", max_length=12)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("comment", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.CASCADE, related_name="reports", to="recipes.recipecomment")),
                ("recipe", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.CASCADE, related_name="reports", to="recipes.recipe")),
                ("reporter", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="content_reports", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.AddConstraint(
            model_name="contentreport",
            constraint=models.CheckConstraint(
                condition=(models.Q(recipe__isnull=False, comment__isnull=True) | models.Q(recipe__isnull=True, comment__isnull=False)),
                name="report_has_exactly_one_target",
            ),
        ),
        migrations.AddConstraint(
            model_name="contentreport",
            constraint=models.UniqueConstraint(condition=models.Q(recipe__isnull=False), fields=("reporter", "recipe"), name="unique_recipe_report_per_user"),
        ),
        migrations.AddConstraint(
            model_name="contentreport",
            constraint=models.UniqueConstraint(condition=models.Q(comment__isnull=False), fields=("reporter", "comment"), name="unique_comment_report_per_user"),
        ),
    ]
