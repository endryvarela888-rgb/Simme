from django.contrib import admin
from .models import CoinTransaction, Purchase, Wallet

admin.site.register(Wallet)
admin.site.register(CoinTransaction)
admin.site.register(Purchase)
