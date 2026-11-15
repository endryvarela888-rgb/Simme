# Repositorio y despliegue

## CI

`.github/workflows/ci.yml` construye la imagen de producción, ejecuta `manage.py check` y compila un APK debug con Android SDK 36 en cada push o pull request a `main`.

## Despliegue continuo

`.github/workflows/deploy.yml` está habilitado para despliegue manual y automático. Los secretos y la variable de Actions están configurados; el último despliegue aplicado actualizó backend y SMTP. El archivo `/opt/simmer/shared/.env` permanece solo en el VPS y no se versiona.

El workflow crea una carpeta por commit en `/opt/simmer/releases/` y usa volúmenes nombrados `simmer_postgres_data`, `simmer_recipe_media`, `simmer_staticfiles` y `simmer_caddy_*`. No ejecuta `docker compose down -v` ni borra volúmenes. Caddy publica la API en los puertos 80/443, sirve archivos estáticos y medios, y administra HTTPS automáticamente; debe haber un dominio público apuntando al VPS antes de arrancar.

## Estado del VPS

Simmer está desplegado en `https://simmer-api.duckdns.org`, con Caddy y un certificado válido de Let's Encrypt. `/api/v1/health/` responde `ok`; el catálogo sirve 757 recetas publicadas y sus imágenes. Los datos y secretos de producción están en volúmenes Docker y `/opt/simmer/shared/.env`; ese archivo no está en Git.

El backend anterior `BeyondTheFrontier` fue detenido y eliminado según la autorización del propietario: se borró su carpeta, sus cuatro volúmenes de PostgreSQL/Caddy/estáticos y sus imágenes Docker. El stack de Simmer usa volúmenes independientes.

La clave SSH anterior `gha_deploy_clean` estaba restringida al despliegue de BeyondTheFrontier y no se reutiliza. Simmer usa una clave de despliegue dedicada; el archivo privado permanece local en `.deploy/`, ignorado por Git.

## Estado actual y siguiente paso

El backend actual está desplegado y el envío SMTP se probó desde el VPS. Google Sign-In está implementado, pero todavía espera que el propietario cree y configure los clientes OAuth descritos en `SETUP-EXTERNAL-ACCOUNTS.md`.

## URL de producción en Android

El host del APK debug y release se configura con la propiedad Gradle `simmerApiBaseUrl`; el valor por defecto ya es `https://simmer-api.duckdns.org/api/v1/`. Se puede sobreescribir, por ejemplo, con `-PsimmerApiBaseUrl=https://<subdominio>.duckdns.org/api/v1/`.
