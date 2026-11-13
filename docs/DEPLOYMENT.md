# Repositorio y despliegue

## CI

`.github/workflows/ci.yml` construye la imagen de producción, ejecuta `manage.py check` y compila un APK debug con Android SDK 36 en cada push o pull request a `main`.

## Despliegue continuo

`.github/workflows/deploy.yml` queda preparado, pero apagado hasta configurar el acceso seguro al VPS. Requiere:

- Variable de repositorio `SIMMER_DEPLOY_ENABLED=true`.
- Secretos `SIMMER_HOST`, `SIMMER_USER`, `SIMMER_DEPLOY_KEY` y `SIMMER_KNOWN_HOSTS`.
- Un usuario de despliegue dedicado con Docker habilitado y una clave SSH exclusiva para Simmer. No reutilizar la clave actual `gha_deploy_clean`: está restringida y ejecuta el despliegue de la aplicación anterior al conectarse.
- Archivo `/opt/simmer/shared/.env` creado en el servidor a partir de `deploy/.env.production.example`, con claves aleatorias, nunca subido al repositorio.

El workflow crea una carpeta por commit en `/opt/simmer/releases/` y usa volúmenes nombrados `simmer_postgres_data`, `simmer_recipe_media`, `simmer_staticfiles` y `simmer_caddy_*`. No ejecuta `docker compose down -v` ni borra volúmenes. Caddy publica la API en los puertos 80/443, sirve archivos estáticos y medios, y administra HTTPS automáticamente; debe haber un dominio público apuntando al VPS antes de arrancar.

## Estado del VPS

Simmer está desplegado en `https://simmer-api.duckdns.org`, con Caddy y un certificado válido de Let's Encrypt. `/api/v1/health/` responde `ok`; el catálogo sirve 757 recetas publicadas y sus imágenes. Los datos y secretos de producción están en volúmenes Docker y `/opt/simmer/shared/.env`; ese archivo no está en Git.

El backend anterior `BeyondTheFrontier` fue detenido y eliminado según la autorización del propietario: se borró su carpeta, sus cuatro volúmenes de PostgreSQL/Caddy/estáticos y sus imágenes Docker. El stack de Simmer usa volúmenes independientes.

La clave SSH anterior `gha_deploy_clean` estaba restringida al despliegue de BeyondTheFrontier y no se reutiliza. Simmer tiene una clave de despliegue dedicada; su archivo privado permanece local en `.deploy/`, ignorado por Git.

## Primera configuración manual necesaria

1. Crear/conectar el repositorio privado `Simmer` en GitHub y subir los commits locales.
2. Añadir a GitHub Actions los secretos `SIMMER_HOST`, `SIMMER_USER`, `SIMMER_DEPLOY_KEY` y `SIMMER_KNOWN_HOSTS`, además de la variable `SIMMER_DEPLOY_ENABLED=true`.
3. Ejecutar el workflow CI y el primer despliegue desde `main`.

## URL de producción en Android

El host del APK release se configura con la propiedad Gradle `simmerApiBaseUrl`. Al registrar el nombre gratuito, compilar con `-PsimmerApiBaseUrl=https://<subdominio>.duckdns.org/api/v1/`. No distribuir APKs release mientras el hostname de ejemplo no esté registrado y HTTPS no responda.
