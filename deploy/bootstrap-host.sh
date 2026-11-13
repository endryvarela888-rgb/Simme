#!/usr/bin/env bash
set -euo pipefail

domain="${1:?Usage: bootstrap-host.sh <api-domain>}"
env_file="/opt/simmer/shared/.env"

sudo install -d -m 700 -o "$(id -un)" -g "$(id -gn)" \
  /opt/simmer/shared /opt/simmer/incoming /opt/simmer/releases

if [[ ! -e "$env_file" ]]; then
  umask 077
  {
    printf 'POSTGRES_DB=simmer\nPOSTGRES_USER=simmer\n'
    printf 'POSTGRES_PASSWORD=%s\n' "$(openssl rand -hex 32)"
    printf 'DJANGO_SECRET_KEY=%s\n' "$(openssl rand -hex 50)"
    printf 'SIMMER_DOMAIN=%s\n' "$domain"
    printf 'DJANGO_ALLOWED_HOSTS=%s,localhost,127.0.0.1\n' "$domain"
    printf 'DJANGO_CSRF_TRUSTED_ORIGINS=https://%s\n' "$domain"
    printf 'DJANGO_SECURE_SSL_REDIRECT=True\n'
    printf 'DJANGO_SESSION_COOKIE_SECURE=True\n'
    printf 'DJANGO_CSRF_COOKIE_SECURE=True\n'
    printf 'DJANGO_SECURE_HSTS_SECONDS=0\n'
  } > "$env_file"
fi

chmod 600 "$env_file"
printf 'Simmer host directories and environment are ready. Secrets were generated locally on the VPS.\n'
