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
4. El mismo Web Client ID se configura como Gradle property `simmerGoogleWebClientId` para Android y como `GOOGLE_OAUTH_CLIENT_IDS` en `/opt/simmer/shared/.env` para Django. No pegues ningún `client_secret`; Android usa el Web Client ID público y el backend verifica la audiencia del token.
5. Ejecuta la app debug local con esa propiedad y prueba una cuenta añadida como tester. El APK debug de GitHub Actions tiene otra firma; para el primer test de Google usa la app ejecutada desde Android Studio con el certificado debug cuyo SHA-1 registraste.

## 3. AdMob y consentimiento

El debug usa IDs de prueba oficiales. El release no solicita anuncios hasta recibir los IDs reales y `SIMMER_ADS_ENABLED=True` en el VPS.

En AdMob crea/registra la app Android con paquete `com.simmer.app`, crea un bloque de anuncio intersticial y un mensaje de privacidad UMP. Necesitamos:

- App ID de AdMob (formato `ca-app-pub-…~…`).
- ID del bloque intersticial (formato `ca-app-pub-…/…`).
- Política de privacidad revisada y datos de contacto definitivos antes de enviar a Play.

Compilación release una vez configurado: `./gradlew bundleRelease -PsimmerAdmobAppId=<APP_ID> -PsimmerAdmobInterstitialId=<INTERSTITIAL_ID> -PsimmerGoogleWebClientId=<WEB_CLIENT_ID>`.

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

## Estado del código

- Android usa la API de producción HTTPS por defecto en debug y release.
- Email verification, recuperación, borrado, Google sign-in, UMP e intersticial están implementados con placeholders o IDs de prueba.
- El backend nuevo está desplegado en el VPS; el registro por email y la autenticación SMTP ya se comprobaron.
- Google Sign-In espera los OAuth Client IDs del propietario; los anuncios reales permanecen desactivados hasta configurar AdMob.
- Chef IA, monedas/Play Billing e i18n quedan fuera del MVP.
