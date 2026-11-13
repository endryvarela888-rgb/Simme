# 07 — Plan paso a paso

**Regla:** un paso a la vez. Antes de cada paso, anunciar rutas y comandos exactos y esperar confirmación. Un paso no se da por terminado hasta que se cumpla su "Hecho cuando". Después se pasa al siguiente, nunca antes.

Formato de cada paso: **Qué** → **Probar** → **Hecho cuando**.

---

## FASE 0 — Preparación

**0.1 Herramientas locales**
- Qué: instalar Git, Docker Desktop, Python 3.12 y Android Studio (incluye JDK, SDK de Android y emulador), editor de código.
- Probar: `git --version`, `docker --version`, `python --version`; Android Studio abre y el emulador arranca.
- Hecho cuando: las 3 versiones se muestran y el emulador abre.

**0.2 Repositorio**
- Qué: crear repo `sazon` con la estructura de `00` (carpetas `backend`, `android`, `deploy`, `docs`), `.gitignore` (incluye `.env`, `media/`, `*.keystore`), copiar estos documentos a `docs/`.
- Hecho cuando: primer commit subido, sin ningún secreto dentro.

**0.3 Cuentas gratuitas necesarias para el MVP**
- Qué: Google Cloud (proyecto + pantalla de consentimiento OAuth), Cloudflare, AdMob (se puede dejar para el paso 3.13), proveedor de correo con plan gratuito. La cuenta de Google Play puede esperar a la Fase 4.
- Hecho cuando: lista de cuentas anotada en un archivo local (sin contraseñas en el repo).

**0.4 Licencia de TheMealDB**
- Qué: leer sus términos vigentes y decidir si hace falta la clave de supporter para una app publicada y monetizada (ver `06`).
- Hecho cuando: decisión escrita en `docs/LICENCIAS.md`.

---

## FASE 1 — Backend

**1.1 Proyecto Django + Docker**
- Qué: proyecto `config`, `docker-compose.yml` con `web` y `db` (PostgreSQL), `.env.example`, settings leyendo entorno.
- Probar: `docker compose up` y abrir `/api/v1/health/` → `{"status":"ok"}`.
- Hecho cuando: levanta desde cero con un solo comando.

**1.2 Usuario propio y auth por email**
- Qué: `accounts.User` (email como login), registro, login, refresh, logout con SimpleJWT.
- Probar: tests + peticiones con curl/Postman.
- Hecho cuando: registro → login → `/me/` funciona y un token caducado es rechazado.

**1.3 Modelos de recetas + admin**
- Qué: `Category`, `Area`, `Recipe`, `RecipeIngredient`, `Favorite`, `*Translation` (vacías) con migraciones y registro en admin.
- Hecho cuando: se puede crear y ver una receta de prueba en `/admin/`.

**1.4 Importación — prueba pequeña**
- Qué: `import_mealdb --limit 20` (categorías, regiones, recetas, imágenes WebP en 2 tamaños).
- Probar: revisar 5 recetas a mano en el admin; ejecutar dos veces y comprobar que no se duplica.
- Hecho cuando: 20 recetas correctas con imagen, pasos separados bien e ingredientes completos.

**1.5 Importación completa**
- Qué: importar todo el catálogo disponible.
- Probar: script de comprobación (≥300 publicadas, todas con imagen existente, ≥1 ingrediente, ≥2 pasos).
- Hecho cuando: el script pasa y el resumen no muestra errores graves.

**1.6 API de lectura**
- Qué: `/categories/`, `/areas/`, `/recipes/` (filtros, búsqueda, orden, paginación), `/recipes/{id}/`, `/recipes/random/`.
- Hecho cuando: pruebas automáticas cubren filtros y búsqueda, y la documentación OpenAPI se genera.

**1.7 Favoritos**
- Qué: endpoints de favoritos + campo `is_favorite` en listas y detalle para usuarios con sesión.
- Hecho cuando: añadir, repetir y quitar funcionan sin errores.

**1.8 Login con Google**
- Qué: `/auth/google/` verificando el ID token; IDs de cliente en el entorno.
- Probar: token inválido rechazado; con un token real (desde la app en el paso 3.3) entra y crea usuario.
- Hecho cuando: pruebas automáticas con token simulado pasan (la prueba real se completa en 3.3).

**1.9 Recuperar contraseña y eliminar cuenta**
- Qué: flujo de reset por correo; `DELETE /me/` borrando datos personales y favoritos; página web simple para solicitar borrado.
- Hecho cuando: ambos flujos probados de punta a punta.

**1.10 Endurecer**
- Qué: throttling, ajustes de seguridad, logs, `/config/` (valor "cada N recetas" de anuncios), colección de pruebas.
- Hecho cuando: lista de seguridad de `06` (parte backend) en verde.

---

## FASE 2 — Despliegue del backend

**2.1 VPS y Docker en producción**
- Qué: preparar VPS (usuario no root, SSH por clave, firewall 22/80/443), clonar repo, `docker-compose.prod.yml` con Nginx + gunicorn + PostgreSQL, `.env` de producción en el servidor.
- Hecho cuando: `/api/v1/health/` responde por IP.

**2.2 Dominio, TLS y Cloudflare**
- Qué: dominio o subdominio, registros DNS, HTTPS, caché de `/media/` en Cloudflare.
- Hecho cuando: `https://api.tudominio/api/v1/health/` responde con candado válido.

**2.3 Datos y copias de seguridad**
- Qué: migrar, importar recetas en producción, `pg_dump` diario con rotación y copia externa; probar una restauración.
- Hecho cuando: restauración exitosa en una BD de prueba.

---

## FASE 3 — App Android (MVP, Kotlin + Compose)

**3.1 Proyecto Android + tema**
- Qué: crear en `android/` un proyecto Kotlin con Jetpack Compose, version catalog, Hilt, ktlint o detekt; tema Material 3 (colores, tipografías, modo oscuro) y componentes base mínimos. Definir el `applicationId` definitivo.
- Hecho cuando: una pantalla de ejemplo con el tema abre en el emulador y en un teléfono real.

**3.2 Cliente API y login por email**
- Qué: Retrofit + OkHttp con `AuthInterceptor` y `Authenticator` (refresh), tokens cifrados con Keystore, pantallas Welcome/Login/Register y navegación entre el grafo `auth` y `main`.
- Hecho cuando: registrarse, cerrar la app, reabrir y seguir con sesión; cerrar sesión funciona.

**3.3 Google Sign-In**
- Qué: configurar OAuth en Google Cloud (cliente web + cliente Android con la huella SHA-1 de debug), integrar Credential Manager, enviar el ID token a `/auth/google/`.
- Hecho cuando: el login con Google real funciona en un dispositivo.

**3.4 Inicio y categorías**
- Qué: pantalla Inicio con categorías y receta del día, estados de carga/error.
- Hecho cuando: datos reales desde el VPS se muestran con skeletons mientras cargan.

**3.5 Lista de recetas**
- Qué: pantalla de categoría con cuadrícula, Paging 3 (scroll infinito) y chips de región.
- Hecho cuando: desplazarse por 100+ recetas es fluido.

**3.6 Búsqueda y filtros**
- Qué: pestaña Búsqueda con debounce, filtros combinables, historial local.
- Hecho cuando: las historias 3 y 4 de `01` se cumplen.

**3.7 Detalle de receta**
- Qué: hero, ingredientes, pasos, etiquetas, enlace de vídeo.
- Hecho cuando: cualquier receta se ve completa y sin cortes de texto.

**3.8 Favoritos**
- Qué: botón con actualización optimista, pantalla de favoritos.
- Hecho cuando: un favorito aparece al instante y persiste tras reiniciar y en otro dispositivo.

**3.9 Modo cocinar**
- Qué: pasos de uno en uno con `HorizontalPager`, progreso, pantalla encendida.
- Hecho cuando: se puede cocinar una receta completa sin salir de la pantalla.

**3.10 Perfil y ajustes**
- Qué: datos de cuenta, enlaces legales (URLs provisionales), eliminar cuenta con confirmación.
- Hecho cuando: eliminar cuenta borra los datos y devuelve a Welcome.

**3.11 Animaciones, primera pasada**
- Qué: las 10 imprescindibles de `04` (de una en una, comprobando fluidez y la escala de animaciones del sistema).
- Hecho cuando: todas funcionan sin tirones en un teléfono de gama media.

**3.12 Estados vacíos, errores y sin conexión**
- Hecho cuando: desconectar la red muestra mensajes claros y permite reintentar sin cierres.

**3.13 Anuncios**
- Qué: AdMob con IDs de prueba, contador cada 5 recetas en DataStore, consentimiento UMP, valor remoto desde `/config/`, permiso de ID de publicidad en el manifiesto.
- Hecho cuando: con IDs de prueba aparece el anuncio exactamente a la 5.ª receta abierta, sin bloquear la app si falla la carga.

**3.14 Pulido y revisión**
- Qué: recorrido completo con una lista de pruebas manuales, TalkBack en los flujos principales, iconos y splash finales, R8 activado, rendimiento medido con el Profiler.
- Hecho cuando: cero cierres inesperados en el flujo principal y la lista de comprobación de `06` (parte app) en verde.

---

## FASE 4 — Lanzamiento del MVP

**4.1 Documentos legales:** política de privacidad y términos publicados en una URL pública; página de borrado de cuenta.
**4.2 Cuenta de Google Play y ficha:** capturas, descripción, Data safety, clasificación, declaración de anuncios.
**4.3 Build de producción:** IDs reales de AdMob, URL de producción, firma, AAB.
**4.4 Pruebas internas y cerradas** (cumpliendo los requisitos vigentes de Play).
**4.5 Publicación gradual** y monitorización.
- Hecho cuando: la app está disponible en Play Store y el flujo completo funciona con la versión instalada desde la tienda.

---

## FASE 5 — V1.0: chef de IA y monedas

**5.1 Hoja de costes:** elegir proveedor/modelo, medir tokens reales con 20 conversaciones de prueba, calcular precios mínimos (método de `05`). *Hecho cuando* hay una tabla con coste por mensaje y precios propuestos.
**5.2 Adapter de IA y endpoint interno:** `/chef/messages/` solo para staff, sin monedas, con límites de longitud y `max_tokens`. *Hecho cuando* responde en streaming y con el tono del chef.
**5.3 Wallet y ledger:** modelos, operaciones atómicas, pruebas de concurrencia e idempotencia. *Hecho cuando* el saldo siempre coincide con la suma de movimientos.
**5.4 Cobro por mensaje:** reserva → respuesta → liquidación con tokens reales. *Hecho cuando* sin saldo no se llama a la IA y con error del proveedor se devuelve la reserva.
**5.5 Chat en la app:** pantalla de chat, streaming, indicador animado, saldo visible.
**5.6 Chat contextual por receta:** botón en el detalle con la receta como contexto.
**5.7 Productos en Play Console y compra en la app** (modo prueba con testers).
**5.8 Verificación en servidor y consumo de compras;** reintento de compras pendientes; manejo de reembolsos.
**5.9 Anuncios recompensados** con tope diario y verificación en servidor.
**5.10 Límites, alertas y panel de métricas** (gasto, ventas, uso).
**5.11 Revisión legal/privacidad** (avisos de IA, Data safety actualizado) y lanzamiento de la versión 1.0.

---

## FASE 6 — V1.1: español y portugués
**6.1** Revisar que todos los textos estén en `values/strings.xml` y crear `values-es/strings.xml` y `values-pt-rBR/strings.xml`.
**6.2** Comando `translate_recipes` con validación; prueba con 20 recetas por idioma y revisión humana.
**6.3** Traducción completa, selector de idioma, ficha de Play en los tres idiomas.
**6.4** Lanzamiento de V1.1.

---

## Riesgos principales y respuesta
| Riesgo | Respuesta |
|---|---|
| Términos de TheMealDB no permiten el uso comercial previsto | Decidir en el paso 0.4; plan B: recetas propias + imágenes con licencia clara |
| Coste de IA mayor al ingreso por monedas | Hoja de costes (5.1), topes de monedas gratis, presupuesto diario, modelo económico |
| Rechazo de Google Play (privacidad, anuncios, pagos) | Cumplir la lista de `06`; usar Play Billing para monedas |
| Pérdida del keystore | Copia en dos lugares seguros desde el primer build |
| Animaciones lentas en móviles modestos | Probar en gama media, respetar la escala de animaciones del sistema, medir con el Profiler antes de optimizar |
| Clave de IA filtrada | Solo backend, variables de entorno, rotación inmediata si hay sospecha |
| Fraude de monedas | Verificación en servidor, idempotencia, ledger, límites por usuario |
| Traducciones con errores | Validación estructural + muestreo humano + estado `machine/reviewed` |

## Definición global de "terminado" para el MVP
- Todos los pasos de las Fases 0–4 cumplidos.
- Backend en producción con copias probadas.
- App publicada con login (email y Google), categorías, búsqueda, filtros, favoritos, modo cocinar, animaciones y anuncios cada 5 recetas.
- Documentos legales publicados y eliminación de cuenta operativa.
