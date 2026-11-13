# 06 — Idiomas, legal y publicación

## 1. Internacionalización (V1.1, pero preparada desde el MVP)

### Desde el MVP
- Ningún texto de interfaz escrito directamente en el código: todo en recursos de Android (`res/values/strings.xml`, inglés) y leído con `stringResource(...)`.
- Modelos `RecipeTranslation`, `CategoryTranslation`, `AreaTranslation` ya creados (vacíos).
- La API ya acepta `lang` y cae a inglés si no hay traducción.
- Fechas, números y monedas con las APIs de formato de Java/Android según la configuración regional (`java.time`, `NumberFormat`).

### Traducción de la interfaz
- Crear `res/values-es/strings.xml` y `res/values-pt-rBR/strings.xml` (portugués: variante de Brasil, `pt-BR`, como principal). Usar plurales (`plurals`) y argumentos posicionales donde haga falta.
- Idioma inicial = idioma del dispositivo si está soportado, si no inglés; selector en Ajustes mediante las preferencias de idioma por aplicación de Android (`AppCompatDelegate.setApplicationLocales`, con `locales_config.xml`).

### Traducción de recetas
- Se traduce **una sola vez** y se guarda en BD (no traducir en vivo).
- Comando `python manage.py translate_recipes --lang es [--limit N] [--only-missing]`:
  - Envía lotes (título, ingredientes, pasos) al proveedor de IA con instrucciones: mantener cantidades y unidades, no inventar, usar vocabulario culinario natural del idioma, devolver JSON válido.
  - Valida el JSON (misma cantidad de ingredientes y de pasos que el original) antes de guardar.
  - Marca `status=machine`.
- Revisión: muestrear 20–30 recetas por idioma, corregir errores sistemáticos en el prompt y repetir.
- Unidades: valorar mostrar gramos/ml además de tazas/onzas; conversión solo si es segura y se prueba.
- Las traducciones también cuestan tokens: estimar con la misma hoja de `05` antes de lanzar el lote completo; usar el modelo más económico que dé calidad suficiente.
- Chef IA: responde en el idioma del usuario (ya indicado en el prompt).

## 2. Licencias y derechos
- **TheMealDB:** leer sus términos de uso vigentes. Si piden clave de supporter para apps publicadas o monetizadas, adquirirla (pago único anunciado en su web, verificar). Respetar atribución si la exigen. Si los términos no permiten el uso previsto, parar y buscar alternativa (recetas propias originales + fotos con licencia clara).
- **Imágenes:** confirmar que su uso/copia está permitido por los términos; si no, usar otra fuente con licencia libre y registrar autor/licencia por imagen.
- **Textos de recetas:** no copiar de otros sitios. Las recetas propias o generadas deben ser originales.
- **Marcas y fuentes:** usar solo fuentes y assets con licencia libre (Google Fonts, Lottie con licencia permitida; guardar la licencia de cada animación descargada).
- Crear `docs/LICENCIAS.md` con cada recurso externo, su licencia y enlace.

## 3. Privacidad y protección de datos
Datos que se tratan: email, nombre, ID de Google, favoritos, historial de monedas, mensajes del chef, ID de publicidad (por AdMob).
- **Política de privacidad** pública (URL obligatoria en Google Play): qué datos, para qué, con quién (Google/AdMob, proveedor de IA, hosting), cuánto tiempo, derechos y contacto.
- **Términos de uso** (monedas, IA, reembolsos, conducta).
- Cumplir normativa aplicable según donde haya usuarios (UE/RGPD, Brasil/LGPD, etc.): consentimiento de anuncios, derecho de acceso y borrado.
- **Eliminar cuenta** dentro de la app **y** una URL web para solicitar el borrado (Google Play lo exige para apps con cuentas).
- Cuestionario "Data safety" de Play Console: declarar con precisión todos los datos recogidos y compartidos.
- Mensajes del chef: informar de que se envían a un proveedor de IA; no pedir datos sensibles; definir retención (p. ej. borrar a los N días o permitir borrarlos).
- Menores: clasificar la app como no dirigida a niños; el registro exige tener la edad mínima legal (indicar en términos).
- Contraseñas nunca en logs; logs sin datos personales innecesarios.

## 4. Seguridad (lista de comprobación previa a publicar)
- [ ] `DEBUG=False`, `ALLOWED_HOSTS` correcto, HTTPS forzado.
- [ ] Claves en variables de entorno, ninguna en el repo (revisar historial de git).
- [ ] Throttling activo en login, búsqueda y chef.
- [ ] Verificación de ID token de Google y de compras en servidor.
- [ ] Copias de seguridad probadas con una restauración real.
- [ ] Actualizaciones del sistema del VPS, firewall (solo 22, 80, 443), acceso SSH por clave, sin login root.
- [ ] Dependencias auditadas (`pip-audit` en el backend; revisar avisos de seguridad de las librerías de Gradle de la app).
- [ ] Panel admin protegido.
- [ ] Respuestas de error sin trazas internas.

## 5. Publicación en Google Play
1. Cuenta de desarrollador de Google Play (pago único; verificar importe y requisitos de verificación de identidad).
2. Crear la app en Play Console, categoría "Comida y bebida".
3. Ficha: nombre, descripción corta/larga, capturas (teléfono), icono 512×512, gráfico de funciones 1024×500.
4. Política de privacidad (URL), Data safety, clasificación de contenido, declaración de anuncios, declaración de IA si procede.
5. Build de producción (AAB firmado, `versionCode` creciente).
6. Pruebas: pista interna → pruebas cerradas. **Si es una cuenta personal nueva, Google puede exigir un período mínimo de prueba cerrada con un número de testers antes de pasar a producción; verificar los requisitos vigentes.**
7. Configurar productos de monedas (en V1.0) y testers de licencia.
8. Publicar en producción con despliegue gradual (p. ej. 10 % → 50 % → 100 %).
9. Monitorizar fallos (Play Console / Sentry) los primeros días.

## 6. Operación posterior
- Responder reseñas, corregir recetas con errores, actualizar importaciones.
- Revisión mensual: coste de IA vs ingresos, reglas de anuncios, políticas de tiendas.
- Plan de versiones: `versionName` semántico, notas de cambios en cada release.
