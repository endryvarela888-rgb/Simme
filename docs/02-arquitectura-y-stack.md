# 02 — Arquitectura y stack

## 1. Diagrama general
```
┌──────────────────────┐        HTTPS         ┌────────────────────────────────────────┐
│  App Android (Kotlin)│ ───────────────────► │  Cloudflare (DNS + caché imágenes)     │
│  - UI / animaciones  │                      └───────────────┬────────────────────────┘
│  - AdMob SDK         │                                      │
│  - Play Billing      │                      ┌───────────────▼────────────────────────┐
│  - Google Sign-In    │                      │  VPS (Docker Compose)                  │
└──────────────────────┘                      │  ┌──────────┐  ┌───────────────────┐   │
                                              │  │  Nginx   │─►│ Django + DRF      │   │
                                              │  │ (TLS,    │  │ (gunicorn)        │   │
                                              │  │ /media)  │  └─────┬─────────────┘   │
                                              │  └──────────┘        │                 │
                                              │               ┌──────▼──────┐          │
                                              │               │ PostgreSQL  │          │
                                              │               └─────────────┘          │
                                              └───────┬───────────────┬────────────────┘
                                                      │               │
                                    ┌─────────────────▼───┐   ┌───────▼─────────────────┐
                                    │ Google (verificar   │   │ API de IA (proveedor    │
                                    │ ID token, Play      │   │ intercambiable)         │
                                    │ Developer API)      │   └─────────────────────────┘
                                    └─────────────────────┘
```
Fuente de datos única de recetas: TheMealDB → comando de importación → nuestra PostgreSQL + `/media`. La app **nunca** habla con TheMealDB.

## 2. Tecnologías
### Backend
- Python 3.12, Django 5.x, Django REST Framework.
- `djangorestframework-simplejwt` (JWT), `django-cors-headers`, `django-filter`, `drf-spectacular` (OpenAPI), `django-environ`.
- `google-auth` para verificar el ID token de Google.
- `Pillow` para convertir imágenes a WebP.
- `requests`/`httpx` para TheMealDB y el proveedor de IA.
- `gunicorn`, `whitenoise` (estáticos), `psycopg`.
- Tests con `pytest-django`.
- Opcional más adelante: Redis (rate limit y caché) y Celery (traducciones masivas).

### App Android (nativa)
- Kotlin + Jetpack Compose + Material 3; Navigation Compose; arquitectura MVVM con `ViewModel` y `StateFlow`; Hilt para inyección de dependencias.
- Red: Retrofit + OkHttp + `kotlinx.serialization`. Listas paginadas con Paging 3. Imágenes con Coil.
- Almacenamiento: DataStore (preferencias, contador de anuncios); tokens cifrados con Android Keystore.
- Login con Google: Credential Manager + Sign in with Google.
- Anuncios: Google Mobile Ads SDK (AdMob) + User Messaging Platform (consentimiento).
- Pagos (V1.0): Google Play Billing Library; alternativa RevenueCat (verificar condiciones y coste vigentes antes de elegir).
- Animaciones: APIs de animación de Compose, transiciones compartidas, Lottie Compose, Canvas/AGSL.
- Idiomas: recursos de Android (`values`, `values-es`, `values-pt-rBR`).
- Build: Gradle Kotlin DSL con version catalog; R8 en release; salida en AAB. Sin servicios de build de terceros: se compila con Android Studio.
- Calidad: JUnit, MockK, Turbine, pruebas de UI de Compose, ktlint/detekt, Android Lint.
- Solo Android por ahora; iOS queda como posibilidad futura (ver `04`, sección 1). Detalle completo de la app en `04`.

### Infraestructura
- VPS con Docker Compose (Django, PostgreSQL, Nginx).
- Dominio + Cloudflare gratis (DNS, caché de imágenes, protección básica).
- TLS con Let's Encrypt (certbot o Caddy).
- Copias de seguridad: `pg_dump` diario con rotación + copia fuera del VPS.
- Correo transaccional (recuperar contraseña): proveedor con plan gratuito, a verificar.
- Almacenamiento de imágenes: disco del VPS (`/media`, servido por Nginx) en el MVP; migrable a un almacenamiento de objetos más tarde.

## 3. Entornos
| Entorno | Backend | App | Datos |
|---|---|---|---|
| local | `docker compose up` en el PC | Android Studio + emulador/dispositivo | BD local, 30 recetas de prueba |
| producción | VPS | build firmado de Play Store | BD real con ≥300 recetas |

Un único `settings.py` leyendo `.env`; `DEBUG=False` en producción.

## 4. Variables de entorno (backend)
```
DJANGO_SECRET_KEY=
DJANGO_ALLOWED_HOSTS=
DATABASE_URL=
JWT_ACCESS_MINUTES=15
JWT_REFRESH_DAYS=30
GOOGLE_OAUTH_CLIENT_IDS=      # IDs válidos (web + android), separados por coma
AI_PROVIDER=                  # nombre del adapter
AI_API_KEY=
AI_MODEL=
GOOGLE_PLAY_PACKAGE_NAME=
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON=   # ruta al json, fuera del repo
EMAIL_HOST= EMAIL_HOST_USER= EMAIL_HOST_PASSWORD=
MEDIA_ROOT=/app/media
THEMEALDB_API_KEY=1           # cambiar por la clave de supporter si hace falta
```

## 5. Decisiones clave y razones
- **Importar, no consultar en vivo:** velocidad, independencia, control de licencias y de imágenes, menos carga.
- **Traducciones en tabla propia:** permite añadir idiomas sin cambiar el esquema.
- **IA solo en backend:** protege la clave y permite cobrar monedas con control.
- **Libro mayor (ledger) de monedas:** el saldo nunca se edita directamente, solo se suman movimientos; da auditoría y evita fraude por doble gasto.
- **Verificación de compras en servidor:** el cliente nunca decide si pagó.

## 6. Costes esperados
- Obligatorio: hosting del VPS, cuenta de desarrollador de Google Play (pago único, verificar importe), dominio (opcional pero recomendado).
- Opcional: TheMealDB supporter (£10 único, si los términos lo exigen).
- Variable: coste de la API de IA (solo se genera cuando los usuarios gastan monedas; limitar monedas gratis).
- Todo lo demás: planes gratuitos (verificar límites vigentes).

## 7. Rendimiento y escalado
- Índices en `Recipe(category, area)`, búsqueda por nombre con `trigram` de PostgreSQL (`pg_trgm`) o `icontains` en el MVP.
- Paginación en todas las listas (20 por página).
- Imágenes en 2 tamaños (miniatura 400 px, grande 1000 px) en WebP.
- Caché HTTP (`ETag`/`Cache-Control`) para categorías y regiones.
