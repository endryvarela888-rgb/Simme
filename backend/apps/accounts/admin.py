from django.contrib import admin

from .models import UserBlock, UserReport


@admin.register(UserBlock)
class UserBlockAdmin(admin.ModelAdmin):
    list_display = ("blocker", "blocked", "created_at")
    search_fields = ("blocker__email", "blocked__email", "blocker__display_name", "blocked__display_name")


@admin.register(UserReport)
class UserReportAdmin(admin.ModelAdmin):
    list_display = ("target", "reporter", "reason", "status", "created_at")
    list_filter = ("status", "reason", "created_at")
    search_fields = ("target__email", "reporter__email", "details")
    readonly_fields = ("reporter", "target", "reason", "details", "created_at")
