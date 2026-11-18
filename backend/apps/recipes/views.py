from django.db import transaction
from django.db.models import Count, Q
from rest_framework import generics, parsers, permissions, status
from rest_framework.pagination import PageNumberPagination
from rest_framework.response import Response
from rest_framework.throttling import ScopedRateThrottle
from rest_framework.views import APIView
from apps.accounts.models import UserBlock
from .models import Area, Category, ContentReport, Favorite, Recipe, RecipeComment, RecipeVote
from .serializers import (AreaSerializer, CategorySerializer, ContentReportSerializer, RecipeCommentSerializer,
                          RecipeCreateSerializer, RecipeSerializer)

class CatalogPagination(PageNumberPagination):
    # The catalog is a small reference list (unlike recipes, which stay paginated).
    # Keep every category/region available to the mobile filter in the first response.
    page_size = 1000

class CommentPagination(PageNumberPagination):
    page_size = 100

class CategoryList(generics.ListAPIView):
    queryset = Category.objects.order_by("name")
    serializer_class = CategorySerializer
    pagination_class = CatalogPagination
class AreaList(generics.ListAPIView):
    queryset = Area.objects.order_by("name")
    serializer_class = AreaSerializer
    pagination_class = CatalogPagination

def recipe_queryset():
    return Recipe.objects.filter(is_published=True).select_related("category", "area", "creator").prefetch_related("ingredients").annotate(
        likes_count=Count("votes", filter=Q(votes__value=RecipeVote.LIKE), distinct=True),
        dislikes_count=Count("votes", filter=Q(votes__value=RecipeVote.DISLIKE), distinct=True),
    )

def blocked_peer_ids(user):
    if not user or not user.is_authenticated:
        return set()
    pairs = UserBlock.objects.filter(Q(blocker=user) | Q(blocked=user)).values_list("blocker_id", "blocked_id")
    return {blocked if blocker == user.id else blocker for blocker, blocked in pairs}

def apply_block_filter(queryset, user):
    blocked_ids = blocked_peer_ids(user)
    return queryset.exclude(creator_id__in=blocked_ids) if blocked_ids else queryset

class RecipeList(generics.ListAPIView):
    serializer_class = RecipeSerializer
    def get_queryset(self):
        qs = apply_block_filter(recipe_queryset(), self.request.user)
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
    serializer_class = RecipeSerializer
    def get_queryset(self): return apply_block_filter(recipe_queryset(), self.request.user)

class RecipeCreate(generics.CreateAPIView):
    serializer_class = RecipeCreateSerializer
    permission_classes = [permissions.IsAuthenticated]
    parser_classes = [parsers.MultiPartParser, parsers.FormParser]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "recipe_create"

    def perform_create(self, serializer):
        self.recipe = serializer.save()

    def create(self, request, *args, **kwargs):
        response = super().create(request, *args, **kwargs)
        response.data = RecipeSerializer(self.recipe, context=self.get_serializer_context()).data
        response.status_code = status.HTTP_201_CREATED
        return response

class RecipeVoteView(APIView):
    permission_classes = [permissions.IsAuthenticated]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "recipe_vote"
    def post(self, request, pk):
        from django.shortcuts import get_object_or_404
        recipe = get_object_or_404(Recipe, pk=pk, is_published=True)
        try:
            value = int(request.data.get("value"))
        except (TypeError, ValueError):
            return Response({"detail": "Vote must be 1 (like) or -1 (dislike)."}, status=status.HTTP_400_BAD_REQUEST)
        if value not in (RecipeVote.LIKE, RecipeVote.DISLIKE):
            return Response({"detail": "Vote must be 1 (like) or -1 (dislike)."}, status=status.HTTP_400_BAD_REQUEST)
        with transaction.atomic():
            RecipeVote.objects.update_or_create(user=request.user, recipe=recipe, defaults={"value": value})
        return Response(vote_summary(recipe, request.user))

class RecipeCommentListCreate(generics.ListCreateAPIView):
    serializer_class = RecipeCommentSerializer
    pagination_class = CommentPagination
    throttle_scope = "comment_write"
    def get_throttles(self):
        return [ScopedRateThrottle()] if self.request.method == "POST" else []
    def get_permissions(self):
        return [permissions.IsAuthenticated()] if self.request.method == "POST" else [permissions.AllowAny()]
    def get_recipe(self):
        from django.shortcuts import get_object_or_404
        return get_object_or_404(Recipe, pk=self.kwargs["pk"], is_published=True)
    def get_queryset(self):
        hidden_ids = blocked_peer_ids(self.request.user)
        queryset = RecipeComment.objects.filter(recipe=self.get_recipe(), parent__isnull=True).select_related("author").prefetch_related("replies__author")
        if hidden_ids:
            queryset = queryset.exclude(author_id__in=hidden_ids)
        return queryset.order_by("created_at").distinct()

    def perform_create(self, serializer):
        serializer.save()
    def get_serializer_context(self):
        context = super().get_serializer_context()
        context["recipe"] = self.get_recipe()
        return context

class RecipeReportView(APIView):
    permission_classes = [permissions.IsAuthenticated]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "report_write"
    def post(self, request, pk):
        from django.shortcuts import get_object_or_404
        recipe = get_object_or_404(Recipe, pk=pk, is_published=True)
        serializer = ContentReportSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        report, created = ContentReport.objects.get_or_create(
            reporter=request.user, recipe=recipe, comment=None, defaults=serializer.validated_data,
        )
        if not created and report.status == ContentReport.DISMISSED:
            for field, value in serializer.validated_data.items():
                setattr(report, field, value)
            report.status = ContentReport.PENDING
            report.save(update_fields=(*serializer.validated_data.keys(), "status"))
        return Response({"detail": "Thanks. The recipe was sent to the moderation queue."}, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)

class CommentReportView(APIView):
    permission_classes = [permissions.IsAuthenticated]
    throttle_classes = [ScopedRateThrottle]
    throttle_scope = "report_write"
    def post(self, request, pk):
        from django.shortcuts import get_object_or_404
        comment = get_object_or_404(RecipeComment, pk=pk, recipe__is_published=True)
        serializer = ContentReportSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        report, created = ContentReport.objects.get_or_create(
            reporter=request.user, comment=comment, recipe=None, defaults=serializer.validated_data,
        )
        if not created and report.status == ContentReport.DISMISSED:
            for field, value in serializer.validated_data.items():
                setattr(report, field, value)
            report.status = ContentReport.PENDING
            report.save(update_fields=(*serializer.validated_data.keys(), "status"))
        return Response({"detail": "Thanks. The comment was sent to the moderation queue."}, status=status.HTTP_201_CREATED if created else status.HTTP_200_OK)
class FavoriteList(generics.ListAPIView):
    permission_classes = [permissions.IsAuthenticated]
    serializer_class = RecipeSerializer
    def get_queryset(self): return apply_block_filter(recipe_queryset(), self.request.user).filter(favorite__user=self.request.user)
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

def vote_summary(recipe, user):
    counts = recipe.votes.aggregate(
        likes_count=Count("id", filter=Q(value=RecipeVote.LIKE)),
        dislikes_count=Count("id", filter=Q(value=RecipeVote.DISLIKE)),
    )
    vote = recipe.votes.filter(user=user).values_list("value", flat=True).first() or 0
    return {**counts, "my_vote": vote}
