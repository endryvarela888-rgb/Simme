# Simmer

Simmer es una app Android de recetas que requiere iniciar sesión. La app consultará el catálogo guardado en un backend propio.

## Estado

Base funcional desplegada: backend Django/DRF y PostgreSQL en Docker, con API detrás de Caddy/HTTPS en `https://simmer-api.duckdns.org/api/v1/health/`. El cliente Android Compose incluye búsqueda, filtros, paginación, favoritos, detalle, modo cocinar, cuentas, recetas comunitarias, comentarios, votos y borrado de cuenta.

El código local prepara además el monedero de monedas, verificación de pagos de Google Play, suscripciones y Chef IA con conversaciones de tres días. Estas funciones permanecen desactivadas hasta configurar Play Console, las credenciales de Google Play y la cuenta/límites del proveedor de IA. El sistema de anuncios y consentimiento está integrado, pero los anuncios de producción necesitan IDs reales y configuración legal de privacidad.

La publicación todavía requiere crear y custodiar la clave de subida, cargar los secretos de publicación y generar el AAB firmado con el workflow manual, revisar y completar los textos legales, configurar la ficha y declaraciones de Play Console, confirmar los derechos de TheMealDB y realizar pruebas desde una pista de prueba. La CI normal genera el APK de depuración; el workflow de AAB firmado espera las credenciales privadas de publicación. Consulta [docs/SETUP-EXTERNAL-ACCOUNTS.md](docs/SETUP-EXTERNAL-ACCOUNTS.md) y [docs/activar-monedas-y-chef-ia.md](docs/activar-monedas-y-chef-ia.md).

## Desarrollo local

Requisitos: Docker Desktop arrancado; Android Studio con Android SDK y emulador; Python 3.12 si se ejecuta Django fuera de Docker.

1. Opcionalmente crear `.env` en la raíz para cambiar `DJANGO_SECRET_KEY`, `POSTGRES_PASSWORD` y `THEMEALDB_API_KEY`.
2. En la raíz: `docker compose up --build`.
3. API: `http://localhost:8000/api/v1/health/`.
4. Abrir `android/` en Android Studio, sincronizar Gradle y ejecutar en emulador.

La clave pública `1` sirve para desarrollo; antes de publicar, confirma la licencia comercial y atribución vigentes de TheMealDB en `docs/THEMEALDB.md`.
