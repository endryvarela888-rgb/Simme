# Repositorio y despliegue

## CI

`.github/workflows/ci.yml` construye la imagen de producción, ejecuta `manage.py check` y compila un APK debug con Android SDK 36 en cada push o pull request a `main`.

## Despliegue continuo

`.github/workflows/deploy.yml` queda preparado, pero apagado hasta configurar el acceso seguro al VPS. Requiere:

- Variable de repositorio `SIMMER_DEPLOY_ENABLED=true`.
- Secretos `SIMMER_HOST`, `SIMMER_USER`, `SIMMER_DEPLOY_KEY` y `SIMMER_KNOWN_HOSTS`.
- Un usuario de despliegue dedicado con Docker habilitado y una clave SSH exclusiva para Simmer. No reutilizar la clave actual `gha_deploy_clean`: está restringida y ejecuta el despliegue de la aplicación anterior al conectarse.
- Archivo `/opt/simmer/shared/.env` creado en el servidor a partir de `deploy/.env.production.example`, con claves aleatorias, nunca subido al repositorio.

El workflow crea una carpeta por commit en `/opt/simmer/releases/` y usa volúmenes nombrados `simmer_postgres_data` y `simmer_recipe_media`. No ejecuta `docker compose down -v` ni borra volúmenes.

## Estado del VPS

La captura identifica `vps-4caee504.vps.ovh.net` (`51.38.80.236`, Ubuntu 26.04, 2 vCPU y 4 GB RAM). La clave SSH local `gha_deploy_clean` inicia una acción forzada del proyecto `BeyondTheFrontier`; al probarla, se reconstruyó ese proyecto y siguieron activos su backend, PostgreSQL y Caddy. No se pudo inspeccionar su configuración ni sus volúmenes por SSH. Simmer usa el puerto loopback 8001 para no desplazar esos servicios por accidente.

La captura de OVH indica que no hay dominios configurados en ese VPS. El servicio nuevo no quedará público con HTTPS hasta que se conozca/configure el dominio y se conecte un proxy TLS (Caddy). No poner el puerto de la API directamente en internet ni habilitar despliegue antes de preparar acceso y `.env`.

## Primera configuración manual necesaria

1. Conectar el plugin de GitHub para crear el repositorio privado `Simmer` y subir el commit local.
2. Preparar en OVH una clave dedicada y un usuario de despliegue que pueda ejecutar Docker sin `sudo`; configurar la clave privada y el `known_hosts` verificado como secretos de Actions.
3. Configurar el dominio/API TLS y el `.env` del VPS; después habilitar `SIMMER_DEPLOY_ENABLED`.
