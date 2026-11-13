# 04 — App Android (Kotlin + Jetpack Compose)

## 1. Alcance de plataforma
- **Android nativo, exclusivo por ahora.** Kotlin + Jetpack Compose.
- `minSdk`: 26 (Android 8.0) como propuesta; `targetSdk`: el que exija Google Play en el momento de publicar (verificar el requisito vigente).
- **iOS en el futuro:** el backend y la API ya son independientes de la plataforma. Para facilitar la versión iOS más adelante:
  - Mantener la capa `domain` en Kotlin puro (sin clases de Android), de modo que pueda compartirse con Kotlin Multiplatform si se decide.
  - Mantener el contrato de la API en OpenAPI (`drf-spectacular`), que sirve para cualquier cliente.
  - No construir nada de iOS ahora.

## 2. Stack Android
| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin (versión estable vigente) |
| UI | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose (rutas tipadas) |
| Arquitectura | MVVM + flujo de datos unidireccional (UDF), `ViewModel` + `StateFlow` |
| Inyección de dependencias | Hilt |
| Red | Retrofit + OkHttp + `kotlinx.serialization` |
| Listas paginadas | Paging 3 (con `LazyVerticalGrid` / `LazyColumn`) |
| Imágenes | Coil (Compose) con caché en memoria y disco |
| Almacenamiento local | DataStore (preferencias, contador de anuncios); tokens cifrados con Android Keystore (p. ej. Tink) |
| Caché de datos | Caché HTTP de OkHttp en el MVP; Room solo si hace falta modo sin conexión más completo |
| Login Google | Credential Manager + Sign in with Google (Google Identity Services) |
| Anuncios | Google Mobile Ads SDK (AdMob) + User Messaging Platform (consentimiento) |
| Pagos (V1.0) | Google Play Billing Library (KTX); alternativa: SDK de RevenueCat (verificar coste/límites) |
| Animaciones | APIs de animación de Compose, transiciones compartidas (`SharedTransitionLayout`), Lottie Compose, Canvas/AGSL para efectos |
| Splash | `androidx.core:splashscreen` |
| Fuentes | Google Fonts descargables (`ui-text-google-fonts`) |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`), R8 activado en release |
| Pruebas | JUnit, MockK, Turbine, pruebas de UI de Compose |
| Calidad | ktlint o detekt, Android Lint |

Verificar versiones estables vigentes de cada librería al crear el proyecto; no fijar versiones antiguas de memoria.

## 3. Arquitectura
Una sola `Activity` (`MainActivity`) con Compose. Un solo módulo Gradle en el MVP, organizado por paquetes y por funcionalidad.

```
UI (Composables)  ◄── estado (StateFlow) ──  ViewModel  ──►  Repository  ──►  Retrofit API / DataStore
        │                                         ▲
        └──────── eventos del usuario ────────────┘
```
- **ui:** pantallas y componentes, sin lógica de negocio.
- **domain (opcional pero recomendado):** modelos y casos de uso en Kotlin puro.
- **data:** repositorios, DTOs de la API, mapeadores DTO → modelo, fuentes de datos.
- Cada pantalla tiene su `UiState` (sealed: `Loading`, `Success`, `Empty`, `Error`) y sus eventos.
- Errores de red modelados con un tipo resultado (`Result`/sealed class), nunca excepciones sin controlar en la UI.

## 4. Estructura del proyecto
```
android/
├─ app/
│  ├─ src/main/
│  │  ├─ AndroidManifest.xml
│  │  ├─ java/<paquete>/
│  │  │  ├─ SazonApp.kt                 # @HiltAndroidApp
│  │  │  ├─ MainActivity.kt
│  │  │  ├─ di/                         # módulos Hilt (red, repos, datastore)
│  │  │  ├─ core/
│  │  │  │  ├─ network/                 # Retrofit, interceptors, Authenticator
│  │  │  │  ├─ datastore/
│  │  │  │  ├─ ads/                     # AdManager, contador, consentimiento
│  │  │  │  └─ util/
│  │  │  ├─ designsystem/               # tema, colores, tipografía, componentes base
│  │  │  ├─ navigation/                 # NavHost, rutas
│  │  │  └─ feature/
│  │  │     ├─ auth/                    # welcome, login, register, forgot
│  │  │     ├─ home/
│  │  │     ├─ category/
│  │  │     ├─ search/
│  │  │     ├─ recipe/                  # detalle + modo cocinar
│  │  │     ├─ favorites/
│  │  │     ├─ profile/                 # ajustes, eliminar cuenta
│  │  │     ├─ chef/                    # (V1.0)
│  │  │     └─ coins/                   # (V1.0)
│  │  └─ res/                           # values (en), values-es, values-pt-rBR, drawables, raw (Lottie)
│  ├─ src/test/  src/androidTest/
│  └─ build.gradle.kts
├─ gradle/libs.versions.toml
├─ build.gradle.kts
└─ settings.gradle.kts
```

## 5. Navegación y pantallas
Grafo con Navigation Compose: grafo `auth` (sin sesión) y grafo `main` (con barra inferior).

```
auth:   Welcome → Login | Register → (ForgotPassword)
main:   Home | Search | Favorites | Profile        (barra inferior)
        Category(slug) → Recipe(id) → Cook(id)
        Chef (V1.0) · Coins (V1.0) · Settings
```

- **Welcome:** logo animado, botón "Continue with Google", "Sign up with email", "I already have an account". Opción "explorar sin cuenta" (solo lectura; favoritos piden login).
- **Inicio:** saludo, "receta del día" destacada con parallax, carrusel horizontal de categorías, sección "Por región". Pull-to-refresh.
- **Categoría:** cabecera con imagen que se contrae con el scroll (`TopAppBar` colapsable), cuadrícula de 2 columnas paginada, chips de región.
- **Búsqueda:** campo con debounce de 350 ms (`Flow.debounce`), chips de filtros (categoría, región, ingrediente), historial reciente local, estado vacío ilustrado.
- **Detalle:** imagen hero con transición compartida desde la tarjeta, título, etiquetas, botón de favorito animado, pestañas Ingredientes / Pasos, botón "Start cooking", botón "Ask the Chef" (V1.0, oculto en el MVP).
- **Cocinar:** un paso por pantalla (`HorizontalPager`), barra de progreso, pantalla encendida mientras esté visible (`FLAG_KEEP_SCREEN_ON`), panel inferior (`ModalBottomSheet`) con ingredientes.
- **Favoritos:** cuadrícula, quitar con animación, estado vacío.
- **Perfil/Ajustes:** cuenta, idioma (V1.1), política de privacidad, términos, cerrar sesión, eliminar cuenta con confirmación fuerte.
- Botón "atrás" y gestos del sistema correctos en todas las pantallas (incluido atrás predictivo, cuando sea posible).

## 6. Estados universales
Cada pantalla de datos implementa: carga (skeleton con shimmer), vacío (ilustración + acción), error (mensaje + "Retry"), sin conexión (aviso; mostrar lo cacheado si existe).

## 7. Datos y red
- Un único `ApiService` (Retrofit) con DTOs `@Serializable`; URL base por `BuildConfig` (debug apunta al PC o entorno local, release al dominio de producción).
- `OkHttp` con:
  - `AuthInterceptor`: añade `Authorization: Bearer <access>`.
  - `Authenticator`: ante un 401 intenta `refresh` una sola vez (con bloqueo para no lanzar varios refrescos a la vez); si falla, cierra sesión y navega a Welcome.
  - Caché HTTP en disco y tiempos de espera razonables.
  - Logging solo en debug y sin imprimir tokens.
- Paging 3 para `/recipes/` (`PagingSource` o `RemoteMediator` si se añade Room).
- **Favoritos con actualización optimista:** se refleja al instante en el estado y se revierte con aviso si el servidor falla.
- Tokens: guardados cifrados (Keystore). Nada sensible en texto plano ni en logs.
- `network_security_config`: tráfico en claro prohibido en release (solo HTTPS).

## 8. Sistema de diseño ("cocina cálida")
### Estilo
Cálido, apetitoso, limpio. Mucha fotografía, tarjetas muy redondeadas, sombras suaves, microinteracciones. Material 3 como base, personalizado (no aspecto por defecto).

### Tokens (paquete `designsystem/`)
- **Colores (propuesta inicial, ajustable):**
  - `primary` tomate/naranja cálido `#E8590C`, `primaryDark` `#C2410C`
  - `secondary` verde albahaca `#2F9E44`
  - `background` crema `#FFF8F0`, `surface` blanco `#FFFFFF`
  - `onBackground` carbón `#2B2118`, `onSurfaceVariant` `#7A6A5C`
  - `error` `#D9480F`
  - Modo oscuro: fondo `#1B1511`, superficie `#261E18`, texto `#F5EBDD`
- **Tipografía:** títulos con una serif expresiva (p. ej. Playfair Display) y cuerpo con una sans legible (p. ej. Inter), cargadas con Google Fonts descargables.
- **Espaciado:** 4/8/12/16/24/32 dp. **Formas:** esquinas 12/20/28 dp y pill.
- Tema claro y oscuro desde el principio; color dinámico (Material You) desactivado para mantener la identidad de marca (decisión revisable).

### Componentes base (`designsystem/components`)
`SazonButton`, `SazonIconButton`, `SazonChip`, `RecipeCard`, `CategoryCard`, `SectionHeader`, `SearchBar`, `SkeletonBox`, `EmptyState`, `ErrorState`, `TagPill`, `StepCard`, `IngredientRow`, `FavoriteButton`, `SazonSnackbar`, y plantillas de pantalla.

## 9. Animaciones (lista concreta, por prioridad)
Reglas: fluidas a 60 fps, sin bloquear la interacción, respetar la escala de animaciones del sistema (si `ANIMATOR_DURATION_SCALE` es 0, usar transiciones instantáneas), medir en un teléfono de gama media.

**Imprescindibles para el MVP**
1. **Splash/Welcome:** logo que entra con resorte (`spring`) + ingredientes flotando suavemente (Lottie).
2. **Entrada de listas:** tarjetas aparecen en cascada (fade + slide con retardo por índice).
3. **Transición compartida** de la imagen entre tarjeta y detalle (`SharedTransitionLayout` / `sharedElement`).
4. **Parallax** de la imagen hero del detalle y cabecera colapsable en categoría.
5. **Botón de favorito:** corazón con "pop" (escala con resorte) + partículas breves (Lottie).
6. **Pulsación de tarjetas:** escala 0.97 con resorte + vibración ligera (`HapticFeedback`).
7. **Pestañas Ingredientes/Pasos:** indicador que se desliza entre pestañas.
8. **Modo cocinar:** transición entre pasos con `HorizontalPager` + barra de progreso animada; celebración al terminar (Lottie).
9. **Skeleton shimmer** durante las cargas.
10. **Barra inferior:** icono activo con rebote y cambio de color.

**Mejoras posteriores**
- Carrusel de categorías con escala según la posición central.
- Efectos con Canvas/AGSL (vapor sobre la receta del día, degradados animados).
- Chef IA: indicador de "escribiendo" con animación de olla/vapor; texto que aparece en streaming.
- Confeti al ganar monedas.

Los archivos Lottie se guardan en `res/raw/` con su licencia anotada en `docs/LICENCIAS.md`.

## 10. Flujo de anuncios (MVP)
- Contador `recipesOpened` persistido en DataStore.
- Al abrir el detalle de una receta: `recipesOpened += 1`. Cuando llega a 5 → mostrar el intersticial **al salir** del detalle o en una pausa natural, nunca mientras el usuario lee ni en el modo cocinar. Reiniciar a 0.
- Precargar el anuncio con antelación; si no está listo, no bloquear: saltar y reintentar en la siguiente oportunidad.
- IDs de prueba en debug; IDs reales solo en release (por `BuildConfig`/flavor).
- Mostrar el formulario de consentimiento (UMP) antes de pedir anuncios donde corresponda.
- Declarar en el manifiesto el permiso de ID de publicidad (`com.google.android.gms.permission.AD_ID`) y reflejarlo en el formulario "Data safety".
- El valor "cada N recetas" viene del servidor (`/api/v1/config/`) para ajustarlo sin publicar versión nueva (por defecto 5).

## 11. Rendimiento
- Claves estables (`key = recipe.id`) en `LazyColumn`/`LazyGrid`; modelos de UI inmutables y estables para evitar recomposiciones.
- Imágenes con tamaño solicitado a la medida de la vista (miniatura vs. grande) y placeholders.
- R8/minificación activado en release; Baseline Profiles para acelerar el arranque (cuando el MVP esté estable).
- Medir con Android Studio Profiler y el Layout Inspector antes de optimizar a ciegas.
- Evitar trabajo pesado en el hilo principal; corrutinas con `Dispatchers.IO` para disco/red.

## 12. Accesibilidad
`contentDescription` en iconos, contraste suficiente, áreas táctiles ≥ 48 dp, soporte de texto grande (sp), compatibilidad con TalkBack en los flujos principales.

## 13. Pruebas
- Unitarias: ViewModels (con Turbine), repositorios, contador de anuncios, mapeadores.
- UI: pruebas de Compose para login, lista, favoritos.
- Prueba manual guiada en un dispositivo real antes de cada entrega (lista en el paso 3.14 de `07`).

## 14. Configuración Android
- `applicationId` único (p. ej. `com.tuempresa.sazon`): definirlo una vez, no se puede cambiar después de publicar.
- Variantes `debug` y `release` con su propia URL base e IDs de AdMob.
- Permisos: `INTERNET`, `ACCESS_NETWORK_STATE`, ID de publicidad (ver sección 10). No pedir permisos innecesarios.
- Firma de la app: **guardar el keystore/clave de subida en dos lugares seguros**; perderla complica las actualizaciones (usar Play App Signing).
- Huellas SHA-1/SHA-256 de debug y release registradas en Google Cloud para el login con Google.
- Publicación como **AAB**; `versionCode` creciente y `versionName` semántico.
- Iconos adaptativos, splash con `core-splashscreen`, orientación vertical por defecto.
- Reglas de copia de seguridad (`backup_rules`/`data_extraction_rules`) para no respaldar tokens.
