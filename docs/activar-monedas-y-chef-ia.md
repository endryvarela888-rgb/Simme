# Activar monedas, suscripciones y Chef IA

La aplicación y el backend ya tienen los flujos preparados. Las compras permanecen desactivadas por defecto: no se concede saldo sin verificar el pago con Google Play. La IA permanece desactivada hasta configurar clave, modelo, precios reales y un límite mensual.

## Catálogo de Google Play

Crea productos de compra única con estos ID exactos y configura sus precios en Play Console. Google Play mostrará el importe localizado en la moneda del comprador.

| ID de producto | Monedas | Precio de referencia |
|---|---:|---:|
| `coins_098` | 20 | USD 0.98 |
| `coins_199` | 45 | USD 1.99 |
| `coins_399` | 100 | USD 3.99 |
| `coins_999` | 260 | USD 9.99 |
| `coins_1999` | 550 | USD 19.99 |

Crea también tres suscripciones mensuales:

| ID | Precio de referencia | Beneficio |
|---|---:|---|
| `simmer_basic_monthly` | USD 1.98 | Sin anuncios |
| `simmer_premium_monthly` | USD 5.99 | Sin anuncios y 10 monedas por día de uso, hasta 30 días en un periodo de 30 días |
| `simmer_chef_elite_monthly` | USD 11.99 | Sin anuncios, 20 monedas diarias y 100 monedas por cada renovación verificada |

Los precios de referencia deben ajustarse en Play Console para cada mercado. La app toma el importe localizado desde Google Play cuando está disponible. Publica los productos en una pista de pruebas y añade las cuentas de prueba antes de probar compras.

## Credenciales de Google Play en el servidor

1. En Google Play Console, vincula la aplicación con un proyecto de Google Cloud.
2. Crea una cuenta de servicio en ese proyecto y dale permisos de acceso a la aplicación en Play Console para consultar y gestionar pedidos/suscripciones.
3. Genera una clave JSON para esa cuenta y guárdala de forma segura. No la subas a GitHub ni la incluyas en la app Android.
4. En los secretos del despliegue del servidor, configura `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` con el JSON completo como una sola variable secreta y `GOOGLE_PLAY_PRODUCTS_ENABLED=true`.
5. Comprueba que `GOOGLE_PLAY_PACKAGE_NAME=com.simmer.app` y vuelve a desplegar el backend.

El backend verifica cada compra con Google, acredita cada ficha de compra una sola vez y consume las compras de monedas. Si una compra no se puede verificar, no se acredita.

## Activar Chef IA

Antes de encender el chat, configura en el entorno privado del backend:

- `AI_PROVIDER=openai`
- `AI_API_KEY`: clave privada del proveedor; nunca en Android ni en el repositorio.
- `AI_MODEL`: un modelo de API que esté habilitado en la cuenta del proveedor.
- `AI_MONTHLY_BUDGET_USD`: límite mensual positivo en USD.
- `AI_INPUT_PRICE_PER_MILLION_USD` y `AI_OUTPUT_PRICE_PER_MILLION_USD`: tarifas actuales del modelo en USD por millón de tokens.
- `AI_MAX_OUTPUT_TOKENS=2000` (máximo acordado para cada respuesta del Chef IA).

Con cualquiera de estos valores ausente o en cero, el chat no llama al proveedor ni cobra monedas. El backend reserva el coste máximo estimado antes de llamar a la IA, registra el uso informado por el proveedor y devuelve la moneda si la respuesta falla. Configura también un límite de gasto en el panel del proveedor: el límite interno de Simmer y el del proveedor son barreras distintas.

## Lo que queda preparado

- El saldo y el historial de movimientos se mantienen en el servidor.
- Solo se concede saldo de compras después de la verificación de Google Play.
- Cada respuesta exitosa del Chef IA cuesta una moneda; una respuesta fallida devuelve la moneda.
- La IA solo recibe una instrucción de responder sobre cocina y obtiene el contexto de receta si se eligió una.
- Los chats se conservan durante tres días desde su última actividad y luego dejan de estar disponibles.
- Las recompensas diarias usan `America/Sao_Paulo`, una por fecha local, con un máximo de 30 en cualquier periodo de 30 días.

## Pendiente antes de habilitarlo para usuarios

Faltan los productos activos en Play Console, la vinculación de la cuenta de servicio, y para el Chef IA la clave, modelo/tarifas y presupuesto mensual. La prueba final debe hacerse con una pista interna de Google Play y compras de prueba; no habilites el catálogo de producción antes de comprobar las verificaciones de compra y las renovaciones.
