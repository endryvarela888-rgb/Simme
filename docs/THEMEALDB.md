# Verificación de TheMealDB

Revisión realizada el 13 de noviembre de 2026 en las páginas oficiales:

- API guide: https://www.themealdb.com/docs_api_guide.php
- Terms: https://www.themealdb.com/terms_of_use.php
- FAQ: https://www.themealdb.com/faq.php

## Estado técnico

- La API V1 respondió durante la comprobación para `categories.php`, `list.php?a=list` y `search.php?f=a` con la clave pública de prueba `1`.
- V1 documenta búsqueda por primera letra (`search.php?f=`), búsqueda por nombre, detalle por ID, lista de categorías y áreas, filtros por categoría/área e ingrediente individual.
- El filtro de varios ingredientes, selección aleatoria de diez y últimos añadidos pertenecen a V2 de pago. Simmer no depende de esas funciones para su MVP.
- La consulta `search.php?f=a` ya devuelve campos más nuevos como `strCountry` además de los campos habituales; el importador debe ignorar campos desconocidos y tolerar áreas nulas.
- La respuesta observada incluyó instrucciones con prefijos “step 1” y fecha `dateModified`; limpiar prefijos y conservar UTF-8 es necesario.

## Publicación y coste

Los términos oficiales dicen que la clave gratuita se limita a desarrollo y que no se pueden publicar apps en una tienda sin suscripción de pago. Para el uso pagado permiten datos y artwork, con la condición de atribuir TheMealDB como fuente. La guía actual anuncia una suscripción de por vida de **£10**. Esto es un pago único pequeño y cabe en el presupuesto que aprobó el propietario; hay que adquirirla y guardar constancia antes de publicar.

Los términos permiten copiar y modificar contenido devuelto por los endpoints oficiales, pero prohíben quitar avisos de derechos/marca. El arte se atribuye a TheMealDB; revisar `strCreativeCommonsConfirmed` y atender cualquier reclamación de terceros. El MVP guardará copias locales para que la app no llame a TheMealDB.

## Límites que siguen abiertos

- La documentación pública no especifica un límite numérico concreto de solicitudes para V1. El importador debe hacer pausas, reintentos con espera incremental y no volver a pedir imágenes innecesariamente.
- El catálogo fluctúa; no se garantiza de antemano que cada una de las ≥300 recetas cumpla nuestras reglas de calidad (imagen, ingredientes y pasos). Contar resultados válidos tras una importación real.
- El `search.php?f=a` observado devuelve datos completos, pero el acceso de prueba no autoriza su publicación. Antes de una tienda, reemplazar clave y validar los términos de nuevo.
