# 01 — Producto y alcance

## 1. Visión
> Plataforma: **Android nativo (Kotlin)**, exclusivo por ahora. iOS queda como posibilidad futura.

Una app de cocina bonita, fluida y fácil: encuentras una receta, la sigues paso a paso, la guardas, y si quieres, un chef de IA te ayuda a adaptarla a lo que tienes en casa.

## 2. Usuarios
- Cocinero casual que busca "qué cocino hoy".
- Persona con ingredientes en casa que quiere ideas.
- Principiante que necesita guía paso a paso.

## 3. Funciones por fase

### MVP (inglés)
- Registro e inicio de sesión: email/contraseña y Google.
- Cerrar sesión, recuperar contraseña (por email), eliminar cuenta (requisito de Google Play).
- Inicio con categorías (≈14) en tarjetas animadas.
- Lista de recetas por categoría y por región (area).
- Búsqueda por nombre con debounce.
- Filtros: categoría, región, ingrediente principal.
- Detalle de receta: imagen grande, ingredientes con cantidades, pasos numerados, etiquetas, enlace a vídeo (si existe).
- Modo "cocinar": pasos de uno en uno, pantalla encendida.
- Favoritos (guardar/quitar, pantalla de favoritos, sincronizado con la cuenta).
- Anuncio intersticial cada 5 recetas abiertas.
- Estados vacíos, de carga (skeletons) y de error con reintento.
- Animaciones de calidad (ver `04`).
- Pantalla de ajustes (cuenta, política de privacidad, términos, eliminar cuenta).

### V1.0 (después del MVP)
- Chef de IA: chat general y chat contextual por receta.
- Sistema de monedas: saldo, historial, compra con Google Play Billing.
- Anuncios recompensados para ganar monedas gratis (pocas).

### V1.1
- Traducción a español y portugués (recetas + interfaz).
- Selector de idioma + idioma del dispositivo por defecto.

### Ideas futuras (no construir antes)
- Lista de compras, planificador semanal, temporizadores por paso.
- Recetas creadas por usuarios con fotos.
- iOS.
- Suscripción mensual como alternativa a monedas.
- Etiquetas propias ("rápidas", "económicas").

## 4. Historias de usuario (MVP)
1. Como usuario nuevo, me registro con Google en dos toques y llego al inicio.
2. Como usuario, veo categorías y entro a "Dessert" para ver sus recetas.
3. Como usuario, escribo "chicken" y veo resultados en menos de 1 s tras dejar de teclear.
4. Como usuario, filtro por región "Italian" dentro de una categoría.
5. Como usuario, abro una receta y sigo los pasos en modo cocinar.
6. Como usuario, guardo una receta en favoritos y la encuentro en otro dispositivo al iniciar sesión.
7. Como usuario, elimino mi cuenta y todos mis datos desde ajustes.

## 5. Fuera del MVP (explícito)
IA, monedas, pagos, traducciones, iOS, comentarios, recetas de usuarios.

## 6. Métricas de éxito del MVP
- Importación: ≥ 300 recetas con imagen y pasos completos.
- Tiempo de carga de la lista < 1,5 s en red móvil normal.
- Cero cierres inesperados en el flujo principal en pruebas manuales.
- Login Google y email funcionando en dispositivo real.
