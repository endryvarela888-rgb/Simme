from django.contrib import admin
from .models import ChefConversation, ChefMessage, ChefMessageReport

admin.site.register(ChefConversation)
admin.site.register(ChefMessage)
admin.site.register(ChefMessageReport)
