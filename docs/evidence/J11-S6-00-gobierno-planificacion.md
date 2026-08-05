# Evidencia J11-S6-00 - Gobierno y planificación de Sprint 6

- Fecha: 2026-07-29
- Estado: Completada documentalmente
- Historia: [J11-S6-00](../sprints/sprint-06/J11-S6-00-gobierno-planificacion.md)

## Resultado

Se definieron identidad, frontera, exclusiones, fuente de conocimiento, secuencia,
gates, riesgos y demo objetivo del primer plugin `business_partners`.

La autorización se limita a caracterizar el legado en `J11-S6-01`. No se ejecutó
el generador, no se creó módulo Maven y no se escribieron Java, SQL, XHTML o CSS.

## Decisiones conservadas

- el kernel no será propietario de participantes;
- el plugin futuro será dueño de `plg_business_partners`;
- otros plugins usarán contratos e identificadores públicos;
- la personalización empresarial se implementará al final sobre slots versionados;
- no se materializa outbox sin intercambio asíncrono concreto;
- la validación independiente G7 continúa pendiente y bloquea cierre formal,
  promoción y producción, no la caracterización documental autorizada.

## Validación aplicable

Esta historia no modifica código. Su gate es documental: existencia de la ficha,
coherencia con ADR-0011/0012/0013, enlaces locales y UTF-8. La comprobación conjunta
se registró al finalizar `J11-S6-01`: 147 Markdown decodificados como UTF-8
estricto, 560 enlaces locales resueltos y cero errores.
