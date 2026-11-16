import uuid

from django.utils.text import slugify
from rest_framework import serializers

from .models import Area, Category, ContentReport, Favorite, Recipe, RecipeComment, RecipeIngredient


class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = Category
        fields = ("id", "name", "slug", "thumbnail", "description")


class AreaSerializer(serializers.ModelSerializer):
    class Meta:
        model = Area
        fields = ("id", "name", "slug")


class IngredientSerializer(serializers.ModelSerializer):
    class Meta:
        model = RecipeIngredient
        fields = ("name", "measure", "position")


class RecipeSerializer(serializers.ModelSerializer):
    category = serializers.CharField(source="category.name", read_only=True)
    area = serializers.CharField(source="area.name", read_only=True, allow_null=True)
    ingredients = IngredientSerializer(many=True, read_only=True)
    image = serializers.SerializerMethodField()
    thumbnail = serializers.SerializerMethodField()
    author_name = serializers.SerializerMethodField()
    likes_count = serializers.SerializerMethodField()
    dislikes_count = serializers.SerializerMethodField()
    my_vote = serializers.SerializerMethodField()
    is_favorite = serializers.SerializerMethodField()

    class Meta:
        model = Recipe
        fields = ("id", "title", "slug", "category", "area", "instructions", "steps", "image", "thumbnail",
                  "tags", "youtube_url", "ingredients", "author_name", "likes_count", "dislikes_count",
                  "my_vote", "is_favorite")

    def _request(self):
        return self.context.get("request")

    def get_image(self, obj):
        if obj.uploaded_image:
            request = self._request()
            url = obj.uploaded_image.url
            return request.build_absolute_uri(url) if request else url
        return obj.image

    def get_thumbnail(self, obj):
        if obj.uploaded_image:
            return self.get_image(obj)
        return obj.thumbnail

    def get_author_name(self, obj):
        if obj.creator_id:
            return obj.creator.display_name or obj.contributor_name or "Simmer cook"
        return obj.contributor_name or obj.source_name

    def get_likes_count(self, obj):
        count = getattr(obj, "likes_count", None)
        return count if count is not None else obj.votes.filter(value=1).count()

    def get_dislikes_count(self, obj):
        count = getattr(obj, "dislikes_count", None)
        return count if count is not None else obj.votes.filter(value=-1).count()

    def get_my_vote(self, obj):
        request = self._request()
        if not request or not request.user.is_authenticated:
            return 0
        return getattr(obj, "my_vote", None) or obj.votes.filter(user=request.user).values_list("value", flat=True).first() or 0

    def get_is_favorite(self, obj):
        request = self._request()
        return bool(request and request.user.is_authenticated and Favorite.objects.filter(user=request.user, recipe=obj).exists())


class RecipeCreateSerializer(serializers.Serializer):
    title = serializers.CharField(max_length=300, trim_whitespace=True)
    image = serializers.ImageField(write_only=True)
    ingredients = serializers.JSONField()
    steps = serializers.JSONField()
    category = serializers.CharField(required=False, allow_blank=True, max_length=100)
    area = serializers.CharField(required=False, allow_blank=True, max_length=100)

    def validate_image(self, image):
        if image.size > 8 * 1024 * 1024:
            raise serializers.ValidationError("The recipe photo must be 8 MB or smaller.")
        return image

    def validate_ingredients(self, value):
        if not isinstance(value, list) or not value or len(value) > 50:
            raise serializers.ValidationError("Add between 1 and 50 ingredients.")
        cleaned = []
        for item in value:
            if isinstance(item, str):
                name, measure = item.strip(), ""
            elif isinstance(item, dict):
                name = str(item.get("name", "")).strip()
                measure = str(item.get("measure", "")).strip()
            else:
                raise serializers.ValidationError("Each ingredient must have a name.")
            if not name or len(name) > 120 or len(measure) > 80:
                raise serializers.ValidationError("Ingredient names are required (120 characters max); measures may be 80 characters max.")
            cleaned.append({"name": name, "measure": measure})
        return cleaned

    def validate_steps(self, value):
        if not isinstance(value, list) or not 1 <= len(value) <= 30:
            raise serializers.ValidationError("Add between 1 and 30 cooking steps.")
        steps = [step.strip() if isinstance(step, str) else "" for step in value]
        if any(not step or len(step) > 2000 for step in steps):
            raise serializers.ValidationError("Each cooking step must contain 1 to 2,000 characters.")
        return steps

    def validate(self, attrs):
        category_name = attrs.get("category", "").strip()
        area_name = attrs.get("area", "").strip()
        if category_name:
            category = Category.objects.filter(slug__iexact=category_name).first() or Category.objects.filter(name__iexact=category_name).first()
            if not category:
                raise serializers.ValidationError({"category": "Choose a category from Simmer's list or leave it blank."})
            attrs["category_object"] = category
        else:
            attrs["category_object"] = Category.objects.get(slug="unspecified")
        if area_name:
            area = Area.objects.filter(slug__iexact=area_name).first() or Area.objects.filter(name__iexact=area_name).first()
            if not area:
                raise serializers.ValidationError({"area": "Choose a region from Simmer's list or leave it blank."})
            attrs["area_object"] = area
        else:
            attrs["area_object"] = None
        return attrs

    def create(self, validated_data):
        ingredients = validated_data.pop("ingredients")
        steps = validated_data.pop("steps")
        image = validated_data.pop("image")
        category = validated_data.pop("category_object")
        area = validated_data.pop("area_object")
        validated_data.pop("category", None)
        validated_data.pop("area", None)
        user = self.context["request"].user
        display_name = user.display_name.strip() or "Simmer cook"
        recipe = Recipe.objects.create(
            external_id=f"simmer-{uuid.uuid4().hex}", slug=f"{slugify(validated_data['title'])[:200]}-{uuid.uuid4().hex[:8]}",
            title=validated_data["title"], category=category, area=area, instructions="\n".join(steps), steps=steps,
            image="", thumbnail="", uploaded_image=image, creator=user, contributor_name=display_name,
            source_name="Simmer community", source_url="", is_published=True,
        )
        RecipeIngredient.objects.bulk_create([
            RecipeIngredient(recipe=recipe, position=index, name=item["name"], measure=item["measure"])
            for index, item in enumerate(ingredients, start=1)
        ])
        return recipe


class RecipeCommentSerializer(serializers.ModelSerializer):
    author_name = serializers.SerializerMethodField()
    replies = serializers.SerializerMethodField()
    parent = serializers.PrimaryKeyRelatedField(queryset=RecipeComment.objects.all(), required=False, allow_null=True, write_only=True)

    class Meta:
        model = RecipeComment
        fields = ("id", "author_name", "text", "created_at", "parent", "replies")
        read_only_fields = ("id", "author_name", "created_at", "replies")

    def get_author_name(self, obj):
        return (obj.author.display_name if obj.author_id else "") or obj.author_name or "Simmer cook"

    def get_replies(self, obj):
        replies = obj.replies.select_related("author").order_by("created_at")
        return RecipeCommentReplySerializer(replies, many=True, context=self.context).data

    def validate_text(self, value):
        value = value.strip()
        if not value:
            raise serializers.ValidationError("Write a comment first.")
        return value

    def validate_parent(self, parent):
        if parent and parent.recipe_id != self.context["recipe"].id:
            raise serializers.ValidationError("You can only reply to a comment on this recipe.")
        if parent and parent.parent_id:
            raise serializers.ValidationError("Replies can be one level deep.")
        return parent

    def create(self, validated_data):
        user = self.context["request"].user
        return RecipeComment.objects.create(
            recipe=self.context["recipe"], author=user,
            author_name=user.display_name.strip() or "Simmer cook", **validated_data,
        )


class RecipeCommentReplySerializer(serializers.ModelSerializer):
    author_name = serializers.SerializerMethodField()

    class Meta:
        model = RecipeComment
        fields = ("id", "author_name", "text", "created_at")

    def get_author_name(self, obj):
        return (obj.author.display_name if obj.author_id else "") or obj.author_name or "Simmer cook"


class ContentReportSerializer(serializers.ModelSerializer):
    reason = serializers.ChoiceField(choices=("inappropriate", "spam", "misinformation", "copyright", "other"))
    class Meta:
        model = ContentReport
        fields = ("reason", "details")

    def validate_details(self, value):
        return value.strip()
