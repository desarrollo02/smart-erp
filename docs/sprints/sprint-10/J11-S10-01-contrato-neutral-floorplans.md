# J11-S10-01 — Contrato neutral versionado de floorplans

- Estado: Completada; gates automatizados verdes
- Sprint: 10
- Fecha de inicio: 2026-08-14
- Fecha de finalización: 2026-08-14
- Tipo: contrato público Java puro
- Dependencia: J11-S10-00 completada
- ADR rector: [ADR-0047](../../adr/0047-floorplans-operativos-transaccionales.md)
- Baseline aceptado: [inventario y métricas](inventario-tareas-metricas-floorplans.md)

## Objetivo

Evolucionar `plugin-api` de forma aditiva para que una pantalla v2 declare su
propósito, regiones semánticas, tipos de dato y acciones, y para que la respuesta
interactiva pueda expresar visibilidad, habilitación y obligatoriedad dinámicas.

## Alcance

- taxonomía cerrada de cinco propósitos;
- roles cerrados para regiones y datos semánticos;
- intención, énfasis y confirmación de acciones;
- experiencia v2 opcional en `ScreenDefinition` con constructor v1 compatible;
- estado dinámico neutral por elemento en `ScreenInteraction.Result`;
- invariantes, copias inmutables y pruebas de compatibilidad;
- actualización aditiva de `PluginApiVersion.CURRENT` dentro de `0.4.x`.

## Criterios de aceptación

- **CA-01:** el propósito sólo admite `MASTER_DATA`, `WORKLIST`,
  `TRANSACTION_EDITOR`, `GUIDED_OPERATION` o `INQUIRY`.
- **CA-02:** fecha, cantidad, dinero, estado, referencia buscable, líneas y
  resumen tienen tipos neutrales sin Jakarta.
- **CA-03:** las acciones distinguen intención, primaria/secundaria/destructiva y
  necesidad de confirmación o motivo.
- **CA-04:** una experiencia v2 referencia únicamente regiones y elementos reales
  de su pantalla; las acciones apuntan a elementos `ACTION`.
- **CA-05:** el constructor vigente de cuatro argumentos sigue creando contratos
  v1 sin experiencia.
- **CA-06:** un contrato v2 sin experiencia es rechazado; un v1 no puede declarar
  silenciosamente experiencia v2.
- **CA-07:** versiones mayores futuras pueden modelarse, pero el shell será quien
  las rechace si todavía no las soporta.
- **CA-08:** la respuesta interactiva puede ocultar, deshabilitar o requerir un
  elemento; un bloqueo visible puede incluir una explicación segura.
- **CA-09:** colecciones y mapas quedan copiados e inmutables y se rechazan
  duplicados, referencias huérfanas y estados contradictorios.
- **CA-10:** `PluginApiVersion.CURRENT` avanza a `0.4.4`; los rangos vivos
  `[0.4.0,0.5.0)` continúan compatibles.
- **CA-11:** pruebas unitarias y ArchUnit demuestran compatibilidad y Java puro.
- **CA-12:** no se modifican shell, plugins, dominio, persistencia, rutas ni
  `sales` en esta historia.

## Gates

1. `plugin-api` y dependencias mínimas con pruebas unitarias;
2. arquitectura para la regla Java puro;
3. reactor completo para comprobar compatibilidad binaria de fuentes vivas;
4. validación documental y `git diff --check`.

Los comandos se ejecutan contra una materialización exacta del índice bajo
`.tools/tmp/validation/J11-S10-01/`.

## Resultado

- `PluginApiVersion.CURRENT` avanzó de `0.4.3` a `0.4.4` dentro del rango vivo
  `[0.4.0,0.5.0)`.
- `ScreenDefinition` admite una `ScreenExperienceDefinition` v2 opcional y
  conserva el constructor v1 de cuatro argumentos.
- Se agregaron propósito, roles de región, tipos semánticos, intención/énfasis de
  acciones y confirmación cerrada.
- `ScreenInteraction.Result` conserva su constructor v1 y agrega estados
  dinámicos inmutables por elemento.
- v2 rechaza regiones o elementos huérfanos, acciones duplicadas/no declaradas y
  estados contradictorios; v1 conserva el diagnóstico de duplicados en el
  registro del kernel.

La evidencia completa está en
[J11-S10-01-contrato-neutral-floorplans](../../evidence/J11-S10-01-contrato-neutral-floorplans.md).
