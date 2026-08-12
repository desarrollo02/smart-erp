# J11-S9-00 - Gobierno y planificación de `purchasing`

- Estado: Completada documentalmente
- Sprint: 9
- Fecha: 2026-08-11
- Tipo: gobierno y planificación
- Dependencia: continuidad excepcional autorizada por producto
- ADR rector: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Objetivo

Convertir el cuarto plugin del roadmap en un incremento ordenado y verificable,
registrando la continuidad hasta una candidata sin ocultar los gates ni declarar
terminados los Sprints abiertos.

## Decisiones de producto registradas

1. continuar con la siguiente iteración funcional;
2. iniciar por `purchasing`, siguiente plugin de ADR-0011;
3. usar `C:\cosme\mega\miaterra\fuente\tag` como fuente Miaterra actualizada y
   de solo lectura;
4. aclaración posterior: ejecutar todas las pruebas automatizadas aplicables y
   diferir únicamente la validación independiente de otra persona;
5. no considerar un Sprint cerrado ni una versión comercializable mientras la
   composición, los gates runtime o la validación independiente estén pendientes;
6. no regenerar instalador hasta la decisión formal de cierre de una candidata.

## Actividades completadas

- se agregó y luego corrigió la excepción temporal en `AGENTS.md` para dejar
  obligatorios los gates automatizados;
- se actualizó Sprint 8 para reflejar continuidad sin cierre;
- se creó la épica específica de Compras;
- se fijó el orden J11-S9-00 a J11-S9-08;
- se definieron alcance, límites, recorridos, gates automatizados y validación
  independiente;
- se dejó el tratamiento de rama pendiente antes del primer cambio de código;
- se inició y cerró la caracterización documental J11-S9-01.

## Criterios de aceptación

- **CA-01:** la autorización y sus límites están versionados. **Cumplido.**
- **CA-02:** la deuda de pruebas está enumerada y no se presenta como verde.
  **Cumplido.**
- **CA-03:** el orden de plugins no cambia. **Cumplido; comienza `purchasing`.**
- **CA-04:** Sprint 8 no se presenta como cerrado. **Cumplido.**
- **CA-05:** no se crea código antes de caracterizar y confirmar decisiones.
  **Cumplido.**
- **CA-06:** no se modifica el legado. **Cumplido por procedimiento de solo
  lectura.**
- **CA-07:** no se crea, publica o fusiona una rama sin resolver el baseline.
  **Cumplido.**
- **CA-08:** J11-S9-07 conserva el gate comercializable completo. **Cumplido como
  planificación; ejecución pendiente.**

## Pruebas

No se modificó código, POM, migraciones, composición ni UI en esta historia de
gobierno, por lo que no tenía una prueba de código propia. Los gates de las
historias J11-S9-02 a J11-S9-05 se ejecutaron posteriormente sobre el corte
materializado; la validación independiente continúa pendiente.

## Resultado

Sprint 9 queda abierto. Producto aceptó PU-D01 a PU-D10 sin cambios y autorizó la
rama local `sprint/09-purchasing` el 2026-08-11. Ambos bloqueos de inicio quedaron
resueltos y J11-S9-02 quedó habilitada. Los gates automatizados del corte no
compuesto están verdes; composición/runtime y validación independiente siguen
pendientes.
