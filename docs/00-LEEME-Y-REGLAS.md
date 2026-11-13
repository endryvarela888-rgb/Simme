# Proyecto: App de Recetas (nombre provisional: "Sazón")

> Documento de entrada. Léelo primero. Después sigue el orden de los demás archivos.

## 1. Qué es
App Android nativa (Kotlin + Jetpack Compose; exclusiva de Android por ahora) con cientos de recetas con imágenes y pasos, organizadas por categorías y regiones, con búsqueda, filtros, favoritos, cuentas de usuario (email + Google), interfaz muy animada y un chef de IA que funciona con monedas.

## 2. Restricciones del proyecto (innegociables)
- **Presupuesto cero**, salvo el hosting (VPS). Todo lo demás debe ser gratuito o de pago único muy pequeño.
- **MVP en inglés.** Español y portugués van después (ver `06`).
- **Monetización**: (a) un anuncio cada 5 recetas abiertas, (b) chef de IA con monedas; el precio de las monedas debe superar siempre el coste de la API de IA + comisión de la tienda.
- Backend propio ya conocido por el equipo: **Django + DRF + PostgreSQL + Docker en VPS**.

## 3. Decisiones tomadas (asumidas; cambiar solo con acuerdo explícito)
| Tema | Decisión |
|---|---|
| App | Android nativa: Kotlin + Jetpack Compose (solo Android por ahora; iOS posible en el futuro) |
| Backend | Django + Django REST Framework |
| Base de datos | PostgreSQL |
| Fuente de recetas | TheMealDB (importación única a nuestra BD) |
| Login | Email/contraseña propio + Google (verificación del ID token en backend) |
| Auth de API | JWT (access + refresh) |
| Anuncios | Google AdMob (Google Mobile Ads SDK + UMP para consentimiento) |
| Pagos de monedas | Google Play Billing (obligatorio para bienes digitales en Android) |
| IA | Proxy en backend, proveedor intercambiable (adapter) |
| Imágenes | Copia propia en WebP servida por Nginx + Cloudflare (plan gratis) |
| Idiomas | EN (MVP), luego ES y PT con tabla de traducciones |

> iOS no se construye ahora. El backend (`03`) y su API son independientes de la plataforma, y la capa `domain` de la app se mantiene en Kotlin puro para facilitar una futura versión iOS (ver `04`, sección 1).

## 4. Índice de documentos
1. `01-producto-y-alcance.md` — visión, funciones, MVP vs final, historias de usuario.
2. `02-arquitectura-y-stack.md` — stack, diagrama, repos, entornos, infraestructura, costes.
3. `03-backend-django.md` — modelos, endpoints, importación, auth, seguridad de API.
4. `04-app-movil.md` — pantallas, navegación, estado, diseño y animaciones.
5. `05-monetizacion-ia-monedas.md` — anuncios, chef IA, economía de monedas, billing.
6. `06-i18n-legal-publicacion.md` — traducciones, licencias, privacidad, Play Store.
7. `07-plan-paso-a-paso.md` — plan ejecutable con criterio de "hecho" por paso.

## 5. Reglas de trabajo para quien desarrolle (Cowork / Claude)
1. **Un paso a la vez.** Cada paso de `07` se implementa, se prueba y se confirma antes de pasar al siguiente. No adelantar pasos ni abrir tareas en paralelo.
2. **Antes de crear o editar un archivo o ejecutar un comando, decir la ruta exacta y el comando exacto**, y esperar confirmación del usuario.
3. Cada paso termina con: qué se hizo, cómo probarlo, resultado esperado.
4. Nunca poner claves (IA, Google, AdMob, Django SECRET_KEY) en el código ni en el repo. Siempre en variables de entorno (`.env` fuera de git).
5. Ninguna llamada a la API de IA desde la app móvil. Solo desde el backend.
6. Si algo de este documento choca con la realidad (una API cambió, una política cambió), parar y avisar en lugar de improvisar.
7. Verificar términos y precios vigentes antes de depender de ellos (TheMealDB, AdMob, Play Billing, proveedor de IA, Cloudflare).

## 6. Estructura de carpetas propuesta
```
sazon/
├─ backend/            # Django project
│  ├─ config/          # settings, urls, wsgi
│  ├─ apps/
│  │  ├─ accounts/     # usuarios, login, Google
│  │  ├─ recipes/      # Category, Area, Recipe, Ingredient, Favorite
│  │  ├─ coins/        # saldo, ledger, compras
│  │  ├─ chef/         # proxy IA, conversaciones
│  │  └─ ads/          # config remota de anuncios (opcional)
│  ├─ manage.py
│  ├─ requirements.txt
│  └─ Dockerfile
├─ android/            # app Android (Kotlin + Compose), ver estructura en 04
│  ├─ app/
│  ├─ gradle/libs.versions.toml
│  └─ build.gradle.kts
├─ deploy/             # docker-compose, nginx, scripts
├─ docs/               # estos documentos
└─ README.md
```
