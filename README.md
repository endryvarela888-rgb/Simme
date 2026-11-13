# Simmer

Simmer es una app Android de recetas que requiere iniciar sesión. La app consultará el catálogo guardado en un backend propio.

## Estado

Este primer corte prepara el backend Django/DRF, PostgreSQL en Docker, un importador de TheMealDB y un cliente Android Compose. El catálogo móvil, las cuentas integradas, los favoritos sincronizados, los anuncios y la publicación aún requieren configurar servicios externos y ejecutar la app en el SDK/emulador. No se deben considerar verificados hasta arrancar el entorno.

## Desarrollo local

Requisitos: Docker Desktop arrancado; Android Studio con Android SDK y emulador; Python 3.12 si se ejecuta Django fuera de Docker.

1. Opcionalmente crear `.env` en la raíz para cambiar `DJANGO_SECRET_KEY`, `POSTGRES_PASSWORD` y `THEMEALDB_API_KEY`.
2. En la raíz: `docker compose up --build`.
3. API: `http://localhost:8000/api/v1/health/`.
4. Abrir `android/` en Android Studio, sincronizar Gradle y ejecutar en emulador.

La clave pública `1` sirve para desarrollo; para publicar, TheMealDB exige una suscripción de pago y atribución. Véase `docs/THEMEALDB.md`.
