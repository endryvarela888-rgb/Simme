# Datos externos pendientes para completar el MVP

Los placeholders permiten preparar el código sin guardar credenciales en Git. **No pegues contraseñas SMTP ni claves privadas en el chat ni en archivos versionados.** Cuando tengas las cuentas, se guardan en los secretos de GitHub o en `/opt/simmer/shared/.env` del VPS.

## 1. Correo transaccional (bloquea el registro por email)

Crea una cuenta en un proveedor que permita envío SMTP transaccional y verifica el remitente que usarás. Necesitamos:

- `EMAIL_HOST`: hostname SMTP.
- `EMAIL_PORT`: normalmente 587.
- `EMAIL_USE_TLS`: normalmente `True` para el puerto 587.
- `EMAIL_HOST_USER`: usuario SMTP.
- `EMAIL_HOST_PASSWORD`: contraseña o clave SMTP.
- `DEFAULT_FROM_EMAIL`: remitente verificado, por ejemplo `Simmer <cuentas@tu-dominio>`.
- `SIMMER_CONTACT_EMAIL`: email de soporte que se mostrará en privacidad, términos y borrado de cuenta.

El VPS solo admite usuarios nuevos cuando SMTP está configurado. Localmente, Django escribe los códigos en la consola para poder desarrollar sin proveedor.

## 2. Google Sign-In

El código usa Credential Manager en Android y valida el ID token en Django. No se usa un OAuth client secret. Google necesita dos OAuth Client IDs en el mismo proyecto:

1. En Google Cloud Console crea o selecciona el proyecto `Simmer` y abre **Google Auth Platform**. Completa Branding con el nombre Simmer y un correo de soporte. Configura la audiencia como **External** y añade tu cuenta de Google como usuario de prueba mientras la app esté en pruebas.
2. En **Clients → Create client → Android**, usa el package name exacto `com.simmer.app` y la huella SHA-1 de la firma debug local. Desde PowerShell:

   ```powershell
   cd D:\Recursos\Proyecto\Simmer\android
   .\gradlew.bat signingReport
   ```

   Copia el valor `SHA1` del bloque `Variant: debug`. En una publicación posterior se añade también el SHA-1 de Play App Signing.
3. En **Clients → Create client → Web application**, crea un cliente Web. No necesita JavaScript origins ni redirect URI para este flujo. Copia el **Client ID**, que termina en `.apps.googleusercontent.com`.
4. El mismo Web Client ID se configura como Gradle property `simmerGoogleWebClientId` (en `android/local.properties`, que no se versiona) y como `GOOGLE_OAUTH_CLIENT_IDS` en `/opt/simmer/shared/.env` para Django. No pegues ningún `client_secret`; Android usa el Web Client ID público y el backend verifica la audiencia del token.
5. Ejecuta la app debug local con esa propiedad y prueba una cuenta añadida como tester. El APK debug de GitHub Actions tiene otra firma; para el primer test de Google usa la app ejecutada desde Android Studio con el certificado debug cuyo SHA-1 registraste.

## 3. AdMob y consentimiento

El debug usa IDs de prueba oficiales. El release no solicita anuncios hasta recibir los IDs reales y `SIMMER_ADS_ENABLED=True` en el VPS.

En AdMob crea/registra la app Android con paquete `com.simmer.app`, crea un bloque de anuncio intersticial y un mensaje de privacidad UMP. Necesitamos:

- App ID de AdMob (formato `ca-app-pub-…~…`).
- ID del bloque intersticial (formato `ca-app-pub-…/…`).
- Política de privacidad revisada y datos de contacto definitivos antes de enviar a Play.

El AAB release se genera desde el workflow manual descrito en la sección 5. Este genera el bundle firmado y no expone el keystore en el repositorio.

El valor remoto del intervalo está en el entorno del VPS: `SIMMER_AD_INTERVAL=5`. El contador local persiste en DataStore; si el anuncio no está listo, no bloquea la navegación y la oportunidad queda pendiente para el siguiente cambio de pantalla.

## 4. CI/CD de GitHub

Repositorio privado ya creado: `endryvarela888-rgb/Simme`. Para habilitar deploy automático, en **Settings → Secrets and variables → Actions** añade:

**Secrets**

- `SIMMER_HOST` = `51.38.80.236`
- `SIMMER_USER` = `ubuntu`
- `SIMMER_DEPLOY_KEY` = contenido de la clave privada dedicada que está localmente en `.deploy/simmer_ovh_admin` (no la subas al repo ni al chat).
- `SIMMER_KNOWN_HOSTS` = huella de host SSH verificada del VPS; mantenerla desde el archivo local `known_hosts`, no desactivar la comprobación del host.

**Repository variable**

- `SIMMER_DEPLOY_ENABLED` = `true`

La configuración habilita el despliegue al hacer push de cambios de backend o deploy. Las compilaciones de Android no despliegan backend.

## 5. Antes de publicar

- Cuenta de Google Play Console del propietario y verificación de identidad.
- Certificado/clave de firma conservado fuera del repositorio y copias seguras.
- Comprar/confirmar la licencia de TheMealDB necesaria para publicar y conservar su atribución; ver `THEMEALDB.md`.
- Completar y revisar privacidad/términos y datos de contacto. Los textos servidos actualmente son borradores, no documentos legales finales.
- Crear la ficha, capturas, declaraciones de datos/anuncios y completar la pista de pruebas que exija Play al tipo de cuenta.
- La CI normal genera un APK de depuración. El workflow manual `.github/workflows/android-release.yml` genera el AAB de lanzamiento cuando se configuren las credenciales de firma y los IDs reales.
- Revisar privacidad y términos para reflejar los anuncios reales, los mensajes enviados al proveedor de IA, la conservación de chats durante tres días y las monedas/suscripciones.
- El chat IA muestra **Report this response** y guarda los reportes en el panel de administración; define quién revisará esa cola antes de habilitarlo al público. Google Play exige que el usuario pueda reportar contenido ofensivo generado por IA dentro de la app.
- Comprueba que eliminar cuenta está disponible tanto dentro de la app como desde una página pública y completa la sección de eliminación de datos en Play Console.

### Secretos para generar el AAB firmado

En **Settings → Secrets and variables → Actions → Secrets**, añadir:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`
- `ANDROID_UPLOAD_STORE_PASSWORD`
- `SIMMER_GOOGLE_WEB_CLIENT_ID`
- `SIMMER_ADMOB_APP_ID`
- `SIMMER_ADMOB_INTERSTITIAL_ID`

La clave de subida debe tener una copia segura fuera de GitHub y nunca subirse al repositorio. Cuando los secretos estén configurados y los cambios estén en `main`, ejecutar **Actions → Build Simmer release bundle → Run workflow**. Indicar un nombre de versión y un `version_code` superior al último publicado; descargar el AAB firmado desde los artefactos del workflow y subirlo a la pista interna de Play Console.

#### Crear la clave de subida desde Windows

1. Instala Android Studio o el JDK de Java 17 si todavía no tienes `keytool`. Crea una carpeta privada fuera del proyecto, por ejemplo `C:\Users\TU_USUARIO\simmer-signing`.
2. En PowerShell, crea el archivo de firma. `keytool` te pedirá contraseñas; guárdalas en un gestor seguro. No envíes la clave ni sus contraseñas por chat.

   ```powershell
   New-Item -ItemType Directory -Force "$env:USERPROFILE\simmer-signing"
   keytool -genkeypair -v -keystore "$env:USERPROFILE\simmer-signing\upload-key.jks" -alias simmer-upload -keyalg RSA -keysize 2048 -validity 10000
   ```

3. Conserva copias cifradas de `upload-key.jks`, el alias (`simmer-upload`) y ambas contraseñas. Si pierdes esta clave, no podrás firmar actualizaciones con ella.
4. Convierte el archivo a Base64 en memoria y copia el resultado al portapapeles para pegarlo directamente en GitHub:

   ```powershell
   $jksBytes = [System.IO.File]::ReadAllBytes("$env:USERPROFILE\simmer-signing\upload-key.jks")
   $keystoreBase64 = [Convert]::ToBase64String($jksBytes)
   Set-Clipboard $keystoreBase64
   ```

5. En el repositorio, abre **Settings → Secrets and variables → Actions → New repository secret**. Crea `ANDROID_UPLOAD_KEYSTORE_BASE64` y pega el texto copiado. Repite para `ANDROID_UPLOAD_KEY_ALIAS` (`simmer-upload`), `ANDROID_UPLOAD_KEY_PASSWORD` y `ANDROID_UPLOAD_STORE_PASSWORD`.
6. Añade también los secretos de Google/AdMob de la lista. El workflow solo crea el AAB; después se sube a Play Console. Si activas Play App Signing, Play conserva la clave de distribución y esta clave funciona como clave de subida.

No reutilices una contraseña de Gmail ni una clave ya incluida en el proyecto. Guarda el archivo JKS fuera de OneDrive/repositorios y con una copia de respaldo protegida.

## 6. Chef IA, monedas y suscripciones

La implementación ya existe en Android y Django, pero se mantiene cerrada hasta que el propietario configure las cuentas externas. Pasos e IDs exactos: `docs/activar-monedas-y-chef-ia.md`.

- La clave de API, modelo, tarifas actuales y límite mensual del proveedor de IA van en `/opt/simmer/shared/.env`; no en el APK ni en el repositorio.
- Las compras requieren los productos de Play Console, el perfil de pagos, una cuenta de servicio con permisos mínimos para consultar compras y el JSON privado en el entorno del VPS.
- `GOOGLE_PLAY_PRODUCTS_ENABLED` debe seguir en `false` hasta que la verificación del servidor esté preparada y los productos estén activos.
- Probar compras, renovaciones, recompensas diarias y recibos desde la pista interna con testers autorizados antes de abrir la tienda.
- No habilitar Chef IA hasta que la clave, el modelo y las tarifas reales estén configurados, junto con límites de gasto tanto en el proveedor como en Simmer.

## Estado del código

- Android usa la API de producción HTTPS por defecto en debug y release.
- Email verification, recuperación, borrado, Google sign-in, UMP e intersticial están implementados con placeholders o IDs de prueba.
- El backend nuevo está desplegado en el VPS; el registro por email y la autenticación SMTP ya se comprobaron.
- Google Sign-In espera los OAuth Client IDs del propietario; los anuncios reales permanecen desactivados hasta configurar AdMob.
- Chef IA, monedas/Play Billing y recompensas de suscripción están implementados en el código local, pero aún requieren las cuentas y pruebas descritas arriba. i18n sigue pendiente.
