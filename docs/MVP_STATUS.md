# Estado del MVP

## Implementado en el árbol de trabajo

- Android Compose con API HTTPS de producción como base URL en debug y release.
- Inicio obligatorio de sesión, registro por email con código de verificación, reenvío, recuperación de contraseña y mantenimiento seguro del token.
- Login de Google mediante Credential Manager y verificación del ID token en backend.
- Catálogo, categorías/regiones, búsqueda con debounce, filtros combinables, paginación, detalle, favoritos, vídeo y modo cocinar.
- Eliminación autenticada de cuenta en la app y solicitud/verificación por email desde web.
- Intersticial cada N recetas (por defecto 5), contador persistente en DataStore, precarga, no bloqueo si no carga, UMP y AdMob Next-Gen.
- Páginas públicas de privacidad, términos y borrado como borradores con contacto reemplazable.
- CI compila backend y APK Android.

## No está terminado / requiere configuración del propietario

- El árbol actual necesita pasar CI antes de desplegar.
- El servidor de producción todavía ejecuta el backend anterior. No desplegar la migración nueva hasta que SMTP se configure, porque el registro por email responderá `503` deliberadamente si el correo no está configurado.
- Crear cuenta y verificar remitente con proveedor SMTP; completar las variables indicadas en `SETUP-EXTERNAL-ACCOUNTS.md`.
- Crear OAuth Client ID web y Android, registrar SHA-1 y completar los placeholders para habilitar el flujo Google real.
- Crear cuenta/app/unidad intersticial y mensajes de UMP en AdMob. Los IDs de prueba solo se usan en debug; release no solicita anuncios mientras no se proporcione un conjunto real.
- Configurar secretos/variable de GitHub Actions para el deploy.
- Probar el APK en dispositivo real, probar email/Google/anuncio, TalkBack, el contador del 5.º detalle y el borrado de cuenta de extremo a extremo.
- Probar una copia/restauración de la base de datos; la política, términos y datos del responsable/contacto son borradores.
- Antes de Play: cuenta de desarrollador, firma/AAB, ficha y declaraciones, y licencia/atribución de TheMealDB.

## Fuera del MVP

Chef de IA, monedas, pagos y español/portugués.
