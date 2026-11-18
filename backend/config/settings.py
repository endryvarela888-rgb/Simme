from pathlib import Path
from datetime import timedelta
import environ

BASE_DIR = Path(__file__).resolve().parent.parent
env = environ.Env(DEBUG=(bool, False))
environ.Env.read_env(BASE_DIR / ".env")
SECRET_KEY = env("DJANGO_SECRET_KEY", default="unsafe-local-key-change-me")
DEBUG = env("DJANGO_DEBUG", default=False)
ALLOWED_HOSTS = env.list("DJANGO_ALLOWED_HOSTS", default=["localhost", "127.0.0.1"])
CSRF_TRUSTED_ORIGINS = env.list("DJANGO_CSRF_TRUSTED_ORIGINS", default=[])
INSTALLED_APPS = [
    "django.contrib.admin", "django.contrib.auth", "django.contrib.contenttypes", "django.contrib.sessions",
    "django.contrib.messages", "django.contrib.staticfiles", "rest_framework", "django_filters",
    "rest_framework_simplejwt.token_blacklist", "apps.accounts", "apps.recipes", "apps.coins", "apps.chef",
]
MIDDLEWARE = ["django.middleware.security.SecurityMiddleware", "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware", "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware", "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware"]
ROOT_URLCONF = "config.urls"
TEMPLATES = [{"BACKEND": "django.template.backends.django.DjangoTemplates", "DIRS": [], "APP_DIRS": True,
    "OPTIONS": {"context_processors": ["django.template.context_processors.request", "django.contrib.auth.context_processors.auth", "django.contrib.messages.context_processors.messages"]}}]
WSGI_APPLICATION = "config.wsgi.application"
DATABASES = {"default": env.db("DATABASE_URL", default=f"sqlite:///{BASE_DIR / 'db.sqlite3'}")}
AUTH_USER_MODEL = "accounts.User"
AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"}]
LANGUAGE_CODE = "en-us"
TIME_ZONE = "UTC"
USE_I18N = True
USE_TZ = True
STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"
MEDIA_URL = "/media/"
MEDIA_ROOT = env("MEDIA_ROOT", default=str(BASE_DIR / "media"))
DATA_UPLOAD_MAX_MEMORY_SIZE = 10 * 1024 * 1024
DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"
EMAIL_BACKEND = env("EMAIL_BACKEND", default="django.core.mail.backends.console.EmailBackend" if DEBUG else "django.core.mail.backends.smtp.EmailBackend")
EMAIL_HOST = env("EMAIL_HOST", default="")
EMAIL_PORT = env.int("EMAIL_PORT", default=587)
EMAIL_USE_TLS = env.bool("EMAIL_USE_TLS", default=True)
EMAIL_HOST_USER = env("EMAIL_HOST_USER", default="")
EMAIL_HOST_PASSWORD = env("EMAIL_HOST_PASSWORD", default="")
DEFAULT_FROM_EMAIL = env("DEFAULT_FROM_EMAIL", default="")
SIMMER_PUBLIC_URL = env("SIMMER_PUBLIC_URL", default="https://simmer-api.duckdns.org")
SIMMER_CONTACT_EMAIL = env("SIMMER_CONTACT_EMAIL", default="replace-with-support-email@example.com")
GOOGLE_OAUTH_CLIENT_IDS = env.list("GOOGLE_OAUTH_CLIENT_IDS", default=[])
SIMMER_ADS_ENABLED = env.bool("SIMMER_ADS_ENABLED", default=DEBUG)
SIMMER_AD_INTERVAL = env.int("SIMMER_AD_INTERVAL", default=5)
AI_PROVIDER = env("AI_PROVIDER", default="openai")
AI_API_KEY = env("AI_API_KEY", default="")
AI_MODEL = env("AI_MODEL", default="")
AI_API_BASE_URL = env("AI_API_BASE_URL", default="https://api.openai.com/v1")
AI_MAX_OUTPUT_TOKENS = env.int("AI_MAX_OUTPUT_TOKENS", default=2000)
AI_MAX_DAILY_MESSAGES = env.int("AI_MAX_DAILY_MESSAGES", default=50)
AI_MONTHLY_BUDGET_USD = env.float("AI_MONTHLY_BUDGET_USD", default=0.0)
AI_INPUT_PRICE_PER_MILLION_USD = env.float("AI_INPUT_PRICE_PER_MILLION_USD", default=0.0)
AI_OUTPUT_PRICE_PER_MILLION_USD = env.float("AI_OUTPUT_PRICE_PER_MILLION_USD", default=0.0)
GOOGLE_PLAY_PACKAGE_NAME = env("GOOGLE_PLAY_PACKAGE_NAME", default="com.simmer.app")
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON = env("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", default="")
GOOGLE_PLAY_PRODUCTS_ENABLED = env.bool("GOOGLE_PLAY_PRODUCTS_ENABLED", default=False)
SIMMER_REWARD_TIME_ZONE = env("SIMMER_REWARD_TIME_ZONE", default="America/Sao_Paulo")
SIMMER_COIN_PRODUCTS = [
    {"product_id": "coins_098", "coins": 20, "price_usd": "0.98"},
    {"product_id": "coins_199", "coins": 45, "price_usd": "1.99"},
    {"product_id": "coins_399", "coins": 100, "price_usd": "3.99"},
    {"product_id": "coins_999", "coins": 260, "price_usd": "9.99"},
    {"product_id": "coins_1999", "coins": 550, "price_usd": "19.99"},
]
SIMMER_SUBSCRIPTION_PRODUCTS = [
    {"product_id": "simmer_basic_monthly", "tier": "basic", "price_usd": "1.98", "daily_coins": 0, "renewal_bonus": 0},
    {"product_id": "simmer_premium_monthly", "tier": "premium", "price_usd": "5.99", "daily_coins": 10, "renewal_bonus": 0},
    {"product_id": "simmer_chef_elite_monthly", "tier": "chef_elite", "price_usd": "11.99", "daily_coins": 20, "renewal_bonus": 100},
]
REST_FRAMEWORK = {"DEFAULT_AUTHENTICATION_CLASSES": ["rest_framework_simplejwt.authentication.JWTAuthentication"],
    "DEFAULT_PERMISSION_CLASSES": ["rest_framework.permissions.AllowAny"],
    "DEFAULT_FILTER_BACKENDS": ["django_filters.rest_framework.DjangoFilterBackend", "rest_framework.filters.SearchFilter", "rest_framework.filters.OrderingFilter"],
    "DEFAULT_PAGINATION_CLASS": "rest_framework.pagination.PageNumberPagination", "PAGE_SIZE": 20,
    "DEFAULT_THROTTLE_CLASSES": ["rest_framework.throttling.ScopedRateThrottle"],
    "DEFAULT_THROTTLE_RATES": {"email_code": "6/hour", "register": "5/hour", "login": "10/minute",
        "recipe_create": "10/day", "recipe_vote": "120/hour", "comment_write": "30/hour", "report_write": "10/hour", "chef_message": "10/minute"}}
SIMPLE_JWT = {"ACCESS_TOKEN_LIFETIME": timedelta(minutes=15), "REFRESH_TOKEN_LIFETIME": timedelta(days=30),
    "ROTATE_REFRESH_TOKENS": True, "BLACKLIST_AFTER_ROTATION": True}
SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")
SECURE_SSL_REDIRECT = env.bool("DJANGO_SECURE_SSL_REDIRECT", default=False)
SESSION_COOKIE_SECURE = env.bool("DJANGO_SESSION_COOKIE_SECURE", default=not DEBUG)
CSRF_COOKIE_SECURE = env.bool("DJANGO_CSRF_COOKIE_SECURE", default=not DEBUG)
SECURE_HSTS_SECONDS = env.int("DJANGO_SECURE_HSTS_SECONDS", default=0)
