# J11-S10-02 — Renderers cerrados de floorplans en el shell

- Estado: Completada; gates automatizados verdes
- Sprint: 10
- Fecha de inicio: 2026-08-14
- Tipo: kernel de composición y adaptador Jakarta Faces
- Dependencia: J11-S10-01 completada
- ADR rector: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)

## Objetivo

Propagar la experiencia v2 hasta el shell y registrar una familia cerrada de
renderers para los cinco propósitos, sin migrar todavía contratos de Inventario o
Compras y sin alterar el recorrido v1 de maestros.

## Alcance

- conservar `ScreenExperienceDefinition` en `ComposedScreen` después de aplicar
  overlays;
- seleccionar `MASTER_DATA`, `WORKLIST`, `TRANSACTION_EDITOR`,
  `GUIDED_OPERATION` o `INQUIRY` únicamente para contratos 2.x;
- rechazar versiones mayores desconocidas, propósitos incompletos, semánticas no
  compatibles y contenido de slot v2 todavía no soportado;
- producir regiones y componentes JSF desde registros cerrados del shell;
- aplicar estados dinámicos sin relajar visibilidad, habilitación ni
  obligatoriedad estáticas;
- impedir en el servidor que una acción bloqueada dinámicamente alcance al
  handler;
- conservar sin cambios los directorios, altas y fichas v1;
- definir adaptación shell-owned en compacto, medio y expandido.

## Criterios de aceptación

- **CA-01:** el kernel copia la experiencia v2 al resultado compuesto y conserva
  el constructor histórico de `ComposedScreen`.
- **CA-02:** el shell reconoce sólo 1.x heredado y 2.x con experiencia; un major
  futuro falla de forma segura.
- **CA-03:** cada propósito selecciona un floorplan cerrado sin recibir XHTML,
  CSS, JavaScript, EL ni nombres de componentes del plugin.
- **CA-04:** `WORKLIST` exige trabajo y acciones; `TRANSACTION_EDITOR` exige
  cabecera, líneas, resumen y acciones; los demás propósitos validan sus regiones
  mínimas.
- **CA-05:** todos los elementos no acción tienen semántica compatible; cada
  acción usa su intención, énfasis y confirmación declarados.
- **CA-06:** los estados dinámicos son parciales: una ausencia conserva el estado
  estático, ocultar/bloquear endurece la UI y no puede reactivar un elemento base.
- **CA-07:** un estado dinámico con ID ajeno a la pantalla rechaza la respuesta y
  una acción bloqueada se vuelve a negar antes de invocar el handler.
- **CA-08:** el renderer v2 usa un único formulario para que la acción incluya
  todos los datos declarados de la tarea.
- **CA-09:** compacto `0–599` y medio `600–839` se reducen a una columna; expandido
  `840+` aplica composición propia por propósito.
- **CA-10:** las pantallas v1 productivas conservan sus secciones, modos y acciones
  existentes.
- **CA-11:** pruebas de kernel, renderer, recursos Facelets, CSS, regresión del
  shell, ArchUnit y reactor completo quedan verdes.
- **CA-12:** no se cambian contratos, handlers, dominio o persistencia de
  Inventario/Compras; eso corresponde a J11-S10-03 y J11-S10-04.

## Decisiones del renderer

`ComposedScreen` expone ahora la experiencia como `Optional`, con un constructor
de cinco argumentos que continúa creando una pantalla v1. El compositor copia el
valor del contrato funcional después de ordenar elementos, slots y overlays.

`ShellFloorplan` es un enum interno, no una extensión del plugin. Cada miembro
define el conjunto mínimo de roles y un identificador CSS controlado. El registro
acepta semánticas compatibles con el tipo neutral y resuelve textos sólo mediante
`ShellTextCatalog`.

`REASON_REQUIRED` no crea un canal de datos oculto: el contrato piloto deberá
declarar un campo de motivo, el handler lo marcará dinámicamente como requerido y
lo validará en servidor. La confirmación visual sigue siendo propiedad del shell.

## Resultado

- prueba focal del compositor: 8/8 verde;
- selección, compatibilidad y estados del shell: 9/9 verde;
- Facelets/CSS y regresión de selectores: 4/4 verde;
- regresión completa `web-shell -am test`: 16/16 módulos y 69 pruebas propias del
  shell verdes;
- arquitectura/composición: 24/24 módulos y 34 pruebas verdes;
- reactor `verify`: 28/28 módulos, 147 reportes y 546 pruebas sin fallos,
  errores u omitidas;
- validación documental del índice: 371 Markdown sin enlaces rotos, errores de codificación,
  mojibake ni secretos;
- migración de pantallas productivas: ninguna, deliberadamente.

La evidencia final se registra en
[J11-S10-02-renderers-shell-floorplans](../../evidence/J11-S10-02-renderers-shell-floorplans.md).
