# Simmer

Simmer es una app Android de recetas que requiere iniciar sesión. La app consultará el catálogo guardado en un backend propio.

## Estado

Base funcional desplegada: backend Django/DRF y PostgreSQL en Docker, 790 recetas importadas (757 publicadas tras la revisión de pasos), y API detrás de Caddy/HTTPS en `https://simmer-api.duckdns.org/api/v1/health/`. El cliente Android Compose incluye búsqueda, filtros, paginación, favoritos, detalle, modo cocinar, verificación de email, recuperación y borrado de cuenta. Las variantes debug y release apuntan por defecto a la API HTTPS de Simmer.

La API de producción aún debe recibir esta última versión del backend. El registro por email permanecerá desactivado hasta configurar SMTP. Google Sign-In, los anuncios reales, CI/CD automático, pruebas manuales en dispositivo y publicación requieren los datos/cuentas externos listados en [docs/SETUP-EXTERNAL-ACCOUNTS.md](docs/SETUP-EXTERNAL-ACCOUNTS.md). El chef IA, las monedas y las traducciones están fuera del MVP.

## Desarrollo local

Requisitos: Docker Desktop arrancado; Android Studio con Android SDK y emulador; Python 3.12 si se ejecuta Django fuera de Docker.

1. Opcionalmente crear `.env` en la raíz para cambiar `DJANGO_SECRET_KEY`, `POSTGRES_PASSWORD` y `THEMEALDB_API_KEY`.
2. En la raíz: `docker compose up --build`.
3. API: `http://localhost:8000/api/v1/health/`.
4. Abrir `android/` en Android Studio, sincronizar Gradle y ejecutar en emulador.

La clave pública `1` sirve para desarrollo; para publicar, TheMealDB exige una suscripción de pago y atribución. Véase `docs/THEMEALDB.md`.
