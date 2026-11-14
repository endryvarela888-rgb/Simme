# Revisión de SDK de anuncios

Revisado el 13 de noviembre de 2026 en documentación oficial de Google:

- Google anunció el 6 de julio de 2026 que GMA Next-Gen SDK es el SDK preferido para Android; el SDK Mobile Ads anterior pasa a Legacy.
- La documentación da soporte a Legacy hasta el 30 de junio de 2027; desde esa fecha queda obsoleto y para el 30 de junio de 2028 puede dejar de servir anuncios o impedir nuevas publicaciones.
- UMP 4.0.0 es la versión publicada en la guía actual de consentimiento.

Esto afecta a la decisión del plan `02`/`04` de usar el antiguo `play-services-ads`. El cliente integra ahora GMA Next-Gen + UMP, conserva la decisión de producto de mostrar un intersticial cada cinco recetas y lo intenta al salir del detalle, nunca durante la lectura o el modo cocinar. Validar comportamiento con IDs de prueba y en dispositivo antes de producción.

Fuentes: https://developers.google.com/ads/blog/posts/2026/07/announcing-google-mobile-ads-next-gen, https://developers.google.com/admob/android/next-gen/quick-start, https://developers.google.com/admob/android/privacy
