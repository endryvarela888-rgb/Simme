# Simmer: decisiones confirmadas

- Nombre del producto: **Simmer** (reemplaza el nombre provisional Sazón en código y entregables nuevos).
- La app obliga a iniciar sesión antes de entrar. El flujo inicial presenta registro o login por email; explorar sin cuenta queda desactivado.
- Si el detalle técnico del contador de anuncios, la compra de monedas o el índice de búsqueda necesita decisiones, el equipo las resuelve durante el desarrollo; solo se consulta al propietario si el impacto es material.
- Revisar cualquier problema de API y política antes de integrar una dependencia externa. Avisar al propietario de bloqueos importantes.
- Gastos de pago único pequeño aceptables dentro del presupuesto acordado; no se autoriza gasto recurrente fuera del VPS.

## Decisiones técnicas temporales del primer corte

- `applicationId`: `com.simmer.app`, provisional hasta validar la identidad de publicación.
- Android debug apunta al backend local en `10.0.2.2:8000`; release apunta a un dominio de producción todavía no contratado.
- La búsqueda inicial usa `icontains`; `pg_trgm` se añadirá si las mediciones del catálogo justifican la extensión.
- El contador de anuncios se mantendrá local en DataStore para no crear actividad de usuario en el backend; el intervalo podrá ajustarse desde configuración remota cuando exista el módulo de anuncios.
- Google Play confirma las compras y el servidor verifica el `purchaseToken`; la app no acredita saldo por su cuenta.
