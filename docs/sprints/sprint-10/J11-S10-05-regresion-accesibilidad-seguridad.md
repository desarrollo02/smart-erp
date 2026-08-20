# J11-S10-05 — Regresión, accesibilidad y seguridad

- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Sprint: 10
- Fecha: 2026-08-20
- Tipo: regresión transversal, accesibilidad, responsive y seguridad negativa
- Dependencia: J11-S10-04 completada automáticamente
- ADR rector: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)

## Objetivo

Demostrar que los floorplans 2.0 conservan los recorridos vigentes de Inventario
y Compras, operan con teclado, respetan movimiento reducido, se adaptan en todos
los límites responsive del proyecto y rechazan el acceso cuando falta permiso o
el plugin no está efectivo para la empresa.

## Alcance

- restaurar el foco después de postbacks de contexto y acciones;
- priorizar campo inválido, aviso visible y título principal al restaurar foco;
- verificar foco visible mediante teclado y `prefers-reduced-motion: reduce`;
- recorrer 375, 599, 600, 720, 839, 840 y 1280 px sin overflow normal;
- conservar selectores, acciones y fuente nativa legibles en compacto;
- revalidar los recorridos integrales de Inventario y Compras;
- comprobar denegación segura con plugin desactivado y permisos insuficientes;
- ejecutar reactor, ArchUnit, integración OIDC/JTA, migraciones y health;
- conservar evidencia visual reproducible y actualizar la documentación afectada.

## Criterios de aceptación

- **CA-01:** un postback de contexto o acción lleva el foco a un destino útil y
  predecible sin depender del mouse.
- **CA-02:** una validación fallida prioriza el primer control inválido; un aviso
  visible se anuncia antes de usar el título como fallback.
- **CA-03:** `prefers-reduced-motion: reduce` elimina las transiciones no
  esenciales del shell.
- **CA-04:** la navegación por Tab produce foco visible en los controles.
- **CA-05:** 375, 599, 600, 720, 839, 840 y 1280 px no presentan overflow
  horizontal normal ni superposición de controles.
- **CA-06:** los recorridos integrales de Inventario y Compras siguen verdes y
  conservan sus reglas funcionales.
- **CA-07:** desactivar un plugin elimina su aporte operativo y el servidor
  rechaza la ruta o acción; ocultar UI no sustituye la autorización.
- **CA-08:** el perfil físico conserva los ocho plugins esperados, health live y
  ready responden correctamente y las migraciones aplicadas permanecen estables.
- **CA-09:** reactor, suites focales, ArchUnit, integración y Playwright quedan
  verdes sobre una materialización exacta del índice Git.

## Decisiones de implementación

El shell marca en `sessionStorage` los postbacks de contexto y acciones, sin
persistir datos del formulario. Al completar la navegación restaura foco en el
primer control inválido, el aviso visible o el `h1` enfocable. La lógica continúa
siendo propiedad del shell; ningún plugin aporta JavaScript, XHTML o CSS.

Los tests Playwright activan explícitamente el modo de movimiento reducido y
comprueban tanto la media query como la transición efectiva del documento. La
navegación por teclado verifica `:focus-visible`, y los mismos recorridos prueban
los límites responsive y la denegación al desactivar cada plugin.

La revisión visual detectó que el selector empresarial, su acción y la fuente
nativa podían superponerse en compacto. El breakpoint del shell pasó a una
grilla de dos filas: selector y acción en la primera; fuente y enlace en la
segunda. Se agregó una regresión de recursos antes de repetir ambos recorridos.

## Validación

- reactor raíz: 28 módulos, 565 pruebas Surefire verdes;
- integración: 8 pruebas Failsafe verdes, incluidas health, OIDC y dos recorridos
  Playwright;
- total automatizado: 573 pruebas, 0 fallos, errores u omitidas;
- ArchUnit: 34 pruebas verdes;
- Inventario y Compras: un recorrido integral Playwright verde por módulo;
- responsive: 375, 599, 600, 720, 839, 840 y 1280 px;
- evidencia visual: 42 PNG revisados, 6.674.853 bytes;
- migrador: siete esquemas validados y cero migraciones nuevas en la repetición;
- runtime: aplicación, PostgreSQL y Keycloak saludables, con ocho plugins
  efectivos en el catálogo;
- imagen: `logixone/app:j11-s10-05-regression`, manifiesto
  `sha256:ebdcbed6cb391bf7eb5df608fc00bf7e1522955e07ba40de0005d16ab2d4477b`;
- materialización de código validada:
  `.tools/tmp/validation/J11-S10-05-compact/`.

La historia queda implementada y validada automáticamente. La validación
independiente continúa pendiente y no equivale a aceptación humana ni cierre de
Sprint 10. El siguiente trabajo autorizado es J11-S10-06.

La evidencia detallada está en
[J11-S10-05-regresión, accesibilidad y seguridad](../../evidence/J11-S10-05-regresion-accesibilidad-seguridad.md).
