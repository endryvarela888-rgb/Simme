# Simmer

Simmer es una app Android de recetas que requiere iniciar sesión. La app consultará el catálogo guardado en un backend propio.

## Estado

Primer corte funcional: backend Django/DRF y PostgreSQL en Docker, 790 recetas importadas (757 publicadas tras la revisión de pasos), endpoints de salud/catálogo/cuentas/favoritos y cliente Android Compose con acceso obligatorio por email, búsqueda, categorías, favoritos, detalle y modo cocinar. La API está desplegada en `https://simmer-api.duckdns.org/api/v1/health/` detrás de Caddy/HTTPS.

Aún falta generar y abrir el APK en Android SDK/emulador; la máquina local no tiene Android SDK configurado. Google login, anuncios, compras de monedas, chef de IA, i18n, la publicación y la activación del CI/CD en GitHub siguen pendientes. Véase [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) para el estado del VPS y los requisitos del repositorio.

## Desarrollo local

Requisitos: Docker Desktop arrancado; Android Studio con Android SDK y emulador; Python 3.12 si se ejecuta Django fuera de Docker.

1. Opcionalmente crear `.env` en la raíz para cambiar `DJANGO_SECRET_KEY`, `POSTGRES_PASSWORD` y `THEMEALDB_API_KEY`.
2. En la raíz: `docker compose up --build`.
3. API: `http://localhost:8000/api/v1/health/`.
4. Abrir `android/` en Android Studio, sincronizar Gradle y ejecutar en emulador.

La clave pública `1` sirve para desarrollo; para publicar, TheMealDB exige una suscripción de pago y atribución. Véase `docs/THEMEALDB.md`.
