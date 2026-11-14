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

En Google Cloud crea el consentimiento OAuth y un cliente OAuth **Web application**. Ese ID se utiliza como `serverClientId` de Credential Manager y como audiencia verificada por Django:

- Android/Gradle: `simmerGoogleWebClientId=<WEB_CLIENT_ID>` en `android/gradle.properties` local o como propiedad Gradle de la compilación.
- VPS: `GOOGLE_OAUTH_CLIENT_IDS=<WEB_CLIENT_ID>` en `/opt/simmer/shared/.env`.
- En el cliente OAuth Android, configura el paquete `com.simmer.app` y la huella SHA-1 del certificado. En desarrollo se obtiene con `./gradlew signingReport`; para Play se registra la huella del certificado de firma de Play.

El Client ID no es una contraseña; no se debe compartir el secreto de cliente web si Google llegara a crear uno para otro tipo de cliente.

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
- El servidor de producción todavía no ejecuta la nueva versión. No aceptará registros por email hasta que SMTP esté configurado y se despliegue la migración.
- Chef IA, monedas/Play Billing e i18n quedan fuera del MVP.
