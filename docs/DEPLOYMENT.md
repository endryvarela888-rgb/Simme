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

La captura identifica `vps-4caee504.vps.ovh.net` (`51.38.80.236`, Ubuntu 26.04, 2 vCPU y 4 GB RAM). La clave SSH local `gha_deploy_clean` inicia una acción forzada del proyecto `BeyondTheFrontier`; al probarla, se reconstruyó ese proyecto y siguieron activos su backend, PostgreSQL y Caddy. No se pudo inspeccionar su configuración ni sus volúmenes por SSH. Simmer usa el puerto loopback 8001 para no desplazar esos servicios por accidente.

La captura de OVH indica que no hay dominios configurados en ese VPS. Para mantener el presupuesto, se propone un subdominio gratuito de DuckDNS que apunte al VPS; Caddy puede obtener y renovar el certificado para ese nombre. El propietario debe registrar el subdominio disponible y completar `SIMMER_DOMAIN` en `.env` antes del despliegue. DuckDNS requiere una cuenta y entrega un token para actualizar el DNS; no compartirlo en el repositorio. Véanse [DuckDNS](https://www.duckdns.org/) y [Caddy Automatic HTTPS](https://caddyserver.com/docs/automatic-https).

## Primera configuración manual necesaria

1. Conectar GitHub para crear el repositorio privado `Simmer` y subir los commits locales.
2. Agregar la clave pública dedicada de Simmer al usuario de despliegue `ubuntu` (o a otro usuario aislado con Docker); configurar la clave privada y el `known_hosts` verificado como secretos de Actions.
3. Registrar un subdominio gratuito y ejecutar `deploy/bootstrap-host.sh <subdominio>` en el VPS para crear el `.env` con secretos aleatorios en el servidor.
4. Revisar el inventario y respaldo del proyecto anterior. Al arrancar Compose de Simmer se ocuparán los puertos 80/443, por lo que el Caddy antiguo se debe detener; sus volúmenes viejos se eliminarán solo tras confirmar que Simmer está saludable.
5. Después habilitar `SIMMER_DEPLOY_ENABLED`.

## URL de producción en Android

El host del APK release se configura con la propiedad Gradle `simmerApiBaseUrl`. Al registrar el nombre gratuito, compilar con `-PsimmerApiBaseUrl=https://<subdominio>.duckdns.org/api/v1/`. No distribuir APKs release mientras el hostname de ejemplo no esté registrado y HTTPS no responda.
