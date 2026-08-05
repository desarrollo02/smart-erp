# J11-S8-05 - Interfaz de `inventory`

- Estado: Completada
- Sprint: 8
- Fecha de inicio: 2026-07-31
- Fecha de finalización: 2026-07-31
- Dependencia: [J11-S8-04](J11-S8-04-aplicacion-seguridad-inventory.md)
- ADR: [ADR-0025](../../adr/0025-recorridos-visuales-inventory.md)

## Objetivo

Construir recorridos JSF productivos, compactos y comprensibles para existencias,
depósitos y conteos sobre los casos de uso reales de inventario, manteniendo
Material Design 3, responsive y autorización server-side.

## Alcance

- tres menús y pantallas: `Existencias`, `Depósitos` y `Conteos`;
- modos separados `directory`, `create` y `detail`;
- proyecciones empresariales para búsquedas y totales, sin nuevas tablas;
- alta y mantenimiento de depósitos/ubicaciones;
- inscripción y mantenimiento de artículos inventariables;
- consulta de disponibilidad, entrada, salida y transferencia en unidad base;
- reserva, consumo, liberación y expiración;
- preparación, captura, revisión, contabilización y cancelación de conteos;
- handlers neutrales y renderer único propiedad del shell;
- slots públicos acotados para personalización futura.

## Fuera de alcance

- composición física WAR/migrador, fixture, imágenes y demo ejecutada;
- importación masiva, costos, valoración, compras, ventas o documentos;
- movimiento en unidad alternativa desde la UI, aunque la API pública lo soporte;
- edición directa de saldos o movimientos contabilizados;
- gráficos analíticos, reportes o paginación avanzada.

## Criterios de aceptación

- **CA-01:** el descriptor publica tres menús y tres pantallas protegidos por
  `inventory.view`.
- **CA-02:** el shell representa los contratos mediante el floorplan cerrado y no
  agrega XHTML, CSS, JavaScript o EL dentro del plugin.
- **CA-03:** depósitos permite buscar, crear, abrir, renombrar, agregar ubicación e
  inactivar con `inventory.storage.manage`.
- **CA-04:** existencias permite buscar/inscribir artículos, consultar totales y
  clave exacta, actualizar snapshot, inactivar, mover y reservar con permisos
  separados.
- **CA-05:** conteos permite buscar, crear alcance, agregar/capturar líneas y
  ejecutar transiciones; contabilizar exige `inventory.adjustments.post`.
- **CA-06:** cada acción revalida empresa, plugin, permiso, recurso y versión; la
  empresa nunca proviene de un campo o parámetro de la pantalla.
- **CA-07:** claves, cantidades, fechas, UUID e idempotencia inválidos producen un
  mensaje comprensible sin revelar SQL, stacktrace ni datos sensibles.
- **CA-08:** directorio, alta y ficha son utilizables en 375, 720 y 1280 px y en
  599/600 y 839/840 sin overflow horizontal normal.
- **CA-09:** labels, teclado, foco, contraste, estados vacíos/error y movimiento
  reducido conservan accesibilidad.
- **CA-10:** pruebas de plugin, renderer, arquitectura y reactor quedan verdes; la
  ejecución Playwright se completa sobre la composición real de J11-S8-06.

## Secuencia

1. fijar ADR, contratos, regiones, acciones y textos;
2. implementar la proyección de lectura empresarial;
3. publicar descriptor y handlers autorizados;
4. registrar las tres presentaciones en el shell;
5. validar módulos, arquitectura, documentación y reactor;
6. componer y ejecutar Playwright en J11-S8-06 antes de cerrar visualmente el corte.

## Resultado

El descriptor publica los tres menús y contratos de pantalla; el plugin aporta
consultas empresariales y handlers autorizados para depósitos, existencias y
conteos, y el shell los representa mediante su renderer JSF único. Las pruebas de
handlers, descriptor, renderer, PostgreSQL, arquitectura y reactor quedaron
verdes. La [evidencia reproducible](../../evidence/J11-S8-05-interfaz-inventory.md)
registra los comandos y resultados.

La historia no crea todavía una demo nueva: la composición física, el fixture, el
despliegue y la validación Playwright real corresponden a `J11-S8-06`.
