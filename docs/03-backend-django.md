# 03 — Backend Django

## 1. Apps y responsabilidades
| App | Responsabilidad |
|---|---|
| `accounts` | Usuario, registro, login, Google, recuperar contraseña, borrar cuenta |
| `recipes` | Category, Area, Recipe, RecipeIngredient, RecipeTranslation, Favorite, importación |
| `coins` (V1.0) | Wallet, CoinTransaction (ledger), productos de monedas, verificación de compras |
| `chef` (V1.0) | Conversaciones, mensajes, proxy de IA, límites |
| `ads` (opcional) | Configuración remota (cada cuántas recetas, activar/desactivar) |

## 2. Modelos (MVP)

### accounts.User
Usar `AbstractUser` con login por email.
- `email` (único, índice), `display_name`, `avatar_url` (opcional), `auth_provider` (`email` | `google`), `google_sub` (único, nulo), `preferred_language` (`en` por defecto), `created_at`.
- Crear el modelo de usuario propio **desde el principio** (no se puede cambiar después sin dolor).

### recipes.Category
- `external_id` (id de TheMealDB), `name` (único), `slug`, `thumbnail` (imagen), `description`.

### recipes.Area
- `name` (único), `slug`.

### recipes.Recipe
- `external_id` (único, id de TheMealDB), `title`, `slug`, `category` (FK), `area` (FK, nulo), `instructions` (texto), `steps` (JSON: lista de pasos ya separados), `image` (ruta WebP grande), `thumbnail` (ruta WebP pequeña), `tags` (JSON lista), `youtube_url`, `source_url`, `is_published` (bool), `created_at`, `updated_at`.
- Índices: `(category)`, `(area)`, `title` (trigram), `is_published`.

### recipes.RecipeIngredient
- `recipe` (FK), `position` (orden), `name`, `measure` (texto, ej. "200 g"), índice por `name` (para filtrar por ingrediente).

### recipes.RecipeTranslation (se crea en el MVP, se llena en V1.1)
- `recipe` (FK), `language` (`es`, `pt`), `title`, `steps` (JSON), `ingredients` (JSON con name/measure), `status` (`machine` | `reviewed`), `updated_at`. Único `(recipe, language)`.
- Igual para `CategoryTranslation` y `AreaTranslation` (solo `name`).

### recipes.Favorite
- `user` (FK), `recipe` (FK), `created_at`. Único `(user, recipe)`.

## 3. Importación desde TheMealDB

Comando: `python manage.py import_mealdb`

Datos de la fuente (API v1, clave de prueba o de supporter): base `https://www.themealdb.com/api/json/v1/<KEY>/`
- `categories.php` → categorías con imagen y descripción.
- `list.php?a=list` → regiones.
- `search.php?f=<letra>` (a–z) → recetas completas por letra. Con esto se obtiene el catálogo sin pedir una por una.
- `lookup.php?i=<id>` → una receta.

Campos de una receta: `idMeal`, `strMeal`, `strCategory`, `strArea`, `strInstructions`, `strMealThumb`, `strTags`, `strYoutube`, `strSource`, `strIngredient1..20`, `strMeasure1..20`.

Algoritmo:
1. Importar categorías y regiones (`update_or_create` por nombre).
2. Recorrer letras a–z; por cada receta:
   - Ignorar si no tiene `strInstructions` o `strMealThumb`.
   - Construir ingredientes: pares no vacíos de `strIngredientN`/`strMeasureN`.
   - Separar `strInstructions` en pasos (por saltos de línea; limpiar numeraciones tipo "STEP 1", líneas vacías, y fusionar líneas demasiado cortas).
   - Descargar la imagen, convertirla a WebP en dos tamaños (400 y 1000 px de ancho), guardar en `MEDIA_ROOT/recipes/<external_id>/`.
   - `update_or_create` por `external_id`.
3. Ser idempotente: ejecutarlo dos veces no duplica nada.
4. Pausa corta entre peticiones (cortesía), reintentos con backoff, log de errores por receta sin detener todo.
5. Opciones: `--limit N`, `--skip-images`, `--only-new`.
6. Al terminar, imprimir resumen: importadas, saltadas, con error.

Comprobaciones después de importar: ≥300 recetas publicadas, todas con ≥1 ingrediente, ≥2 pasos e imagen existente en disco.

## 4. API REST (prefijo `/api/v1/`)

### Auth
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/register/` | email, password, display_name → manda código de verificación; la cuenta queda inactiva |
| POST | `/auth/login/` | email, password → tokens |
| POST | `/auth/verify-email/` | email, code (6 dígitos) → verifica y devuelve tokens |
| POST | `/auth/resend-verification/` | email → vuelve a mandar código (60 s entre envíos) |
| POST | `/auth/google/` | id_token → verifica con Google y devuelve tokens |
| POST | `/auth/google/` | `id_token` de Google → crea o enlaza usuario → tokens |
| POST | `/auth/refresh/` | refresh → nuevo access |
| POST | `/auth/logout/` | invalida refresh (blacklist) |
| POST | `/auth/password/forgot/` | envía enlace/código por email |
| POST | `/auth/password/reset/` | confirma nueva contraseña |
| POST | `/auth/account-deletion/request/` + `/confirm/` | verificación por email y borrado desde web |
| DELETE | `/auth/me/` | requiere la contraseña actual; borra cuenta y favoritos |
| GET/PATCH | `/me/` | perfil |
| DELETE | `/me/` | borra la cuenta y sus datos (requiere confirmar contraseña o reautenticar con Google) |

Login con Google (flujo): la app obtiene el ID token con Google Sign-In → lo envía a `/auth/google/` → el backend lo verifica con `google-auth` (firma, `aud` dentro de `GOOGLE_OAUTH_CLIENT_IDS`, `exp`, `email_verified`) → busca por `google_sub`, si no existe enlaza por email verificado o crea el usuario → devuelve JWT propios. Nunca confiar en datos de Google que el cliente envíe aparte del token.

### Recetas (lectura pública; favoritos requieren login)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/categories/` | lista con imagen |
| GET | `/areas/` | lista de regiones |
| GET | `/recipes/` | lista paginada; params: `category`, `area`, `ingredient`, `search`, `ordering`, `page` |
| GET | `/recipes/{id}/` | detalle completo |
| GET | `/recipes/random/` | una receta aleatoria |
| GET | `/favorites/` | favoritos del usuario |
| POST | `/favorites/` | `{recipe_id}` |
| DELETE | `/favorites/{recipe_id}/` | quitar |

Idioma: cabecera `Accept-Language` o parámetro `lang`; si falta la traducción, devolver inglés.

Formato de lista: `{count, next, previous, results:[{id,title,thumbnail,category,area,is_favorite}]}`. El detalle incluye `ingredients[]`, `steps[]`, `tags[]`, `youtube_url`.

### V1.0 — Monedas y chef (resumen; detalle en `05`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/coins/balance/` | saldo |
| GET | `/coins/transactions/` | historial |
| GET | `/coins/products/` | paquetes disponibles |
| POST | `/coins/purchase/verify/` | `purchase_token` + `product_id` → verifica con Google y acredita |
| POST | `/coins/reward/` | reclamar moneda por anuncio recompensado (verificación del lado del servidor) |
| POST | `/chef/messages/` | mensaje del usuario (+ `recipe_id` opcional) → respuesta (streaming SSE) |
| GET | `/chef/conversations/` | historial |

## 5. Seguridad del backend
- JWT corto (15 min) + refresh largo con rotación y lista negra.
- Contraseñas con los validadores de Django y hash por defecto (PBKDF2/Argon2 si se instala).
- Throttling DRF: login/registro (5/min por IP), búsqueda (60/min), chef (por usuario).
- CORS cerrado (la app Android no lo necesita; solo permitir orígenes propios si hay panel web).
- `SECURE_*` activados en producción (HSTS, cookies seguras, `X-Content-Type-Options`).
- Validar y limitar todos los parámetros (`page_size` máximo 50).
- Registro de errores con Sentry (plan gratuito, verificar) o logs rotados.
- Panel `/admin/` en ruta no obvia, con contraseña fuerte y, si es posible, restringido por IP.
- Respuestas de error uniformes: `{code, message, details}`.

## 6. Pruebas mínimas obligatorias
- Registro/login/refresh/logout.
- Google login con token falso → rechazo; con token válido simulado → éxito.
- Lista y filtros devuelven lo esperado; búsqueda sin resultados devuelve lista vacía.
- Favorito duplicado no rompe; borrar uno inexistente devuelve 404 limpio.
- Importación idempotente.
- Borrado de cuenta elimina favoritos y datos personales.

## 7. Despliegue
- `Dockerfile` multi-stage; `docker-compose.yml` con `web`, `db`, `nginx`.
- Migraciones automáticas en el arranque (`entrypoint.sh`) tras una copia de seguridad.
- `collectstatic` en la construcción.
- Healthcheck `GET /api/v1/health/` (sin autenticación).
