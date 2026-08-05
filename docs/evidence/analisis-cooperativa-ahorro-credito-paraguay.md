# Análisis para cooperativa de ahorro y crédito de Paraguay

- Fecha de consulta: 2026-08-04
- Estado: planificación aceptada; implementación futura
- Decisión: [ADR-0037](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md)
- Épica: [Cooperativa de ahorro y crédito](../backlog/epica-cooperativa-ahorro-credito-paraguay.md)

## Pregunta analizada

Qué plugins necesita Logixone para administrar una cooperativa de ahorro y
crédito sin convertir el ERP modular en un único dominio financiero acoplado.

Se asumió Paraguay por el baseline del proyecto, la zona operativa y los plugins
nacionales ya planificados. La implementación deberá volver a confirmar país,
tipo/nivel de supervisión, estatuto y servicios de la cooperativa concreta.

## Evidencia oficial consultada

| Fuente | Hecho usado para el diseño | Consecuencia arquitectónica |
|---|---|---|
| [Ley N.º 438/1994](https://www.bacn.gov.py/leyes-paraguayas/2373/ley-n-438-cooperativas-br) y [Ley N.º 5501/2015](https://www.bacn.gov.py/leyes-paraguayas/4482/modi) | naturaleza cooperativa, membresía voluntaria, control democrático y órganos | membresía y gobierno no pertenecen a clientes/ventas ni al kernel |
| [Ley N.º 2.157/2003](https://www.bacn.gov.py/leyes-paraguayas/2423/regula-el-funcionamiento-del-instituto-nacional-de-cooperativismo-y-establece-su-carta-organica) | INCOOP es autoridad de aplicación, fiscalización y control | fuentes, plazos, reportes y evidencia deben ser reproducibles |
| [Resolución INCOOP N.º 22.668/2020](https://www.incoop.gov.py/?p=9655) | marco vigente localizado para ahorro y crédito, que abrogó el de 2017 | ahorros, créditos y prudencia necesitan dominios/reglas explícitos |
| [Resolución INCOOP N.º 12.147/2014](https://www.incoop.gov.py/?p=4330) y modificaciones listadas por INCOOP | plan de cuentas estandarizado del sector | `accounting` conserva el mayor; el adaptador paraguayo conserva mapeos/versiones |
| [Resolución SEPRELAD N.º 156/2020](https://www.seprelad.gov.py/resoluciones/resoluciones/resolucion-n-156-2020-reglamento-de-prevencion-de-la-y-ft-para-cooperativas-sujetas-a-la-incoop.pdf) | prevención LA/FT basada en administración y gestión de riesgos para cooperativas supervisadas | se requiere expediente, riesgo, alertas/casos y trazabilidad separados |
| [Resolución INCOOP N.º 154/2026](https://www.incoop.gov.py/?p=14383) | modificación reciente de la matriz de riesgo LA/FT sectorial | las matrices son paquetes regulatorios versionados, no constantes |
| [Comunicado INCOOP N.º 25/2026](https://www.incoop.gov.py/?p=14665) | publicación trimestral del promedio ponderado de morosidad aplicable al marco | los indicadores variables necesitan fuente, vigencia y reproducción |

La revisión no declara que esta lista sea exhaustiva ni reemplaza asesoría legal,
contable o de cumplimiento. Antes de implementar se descargarán los documentos
oficiales aplicables en `.tools/`, se verificarán versión y checksum y se
construirá una matriz requisito–caso de uso–dato–regla–prueba–reporte.

## Alternativas comparadas

| Alternativa | Resultado |
|---|---|
| un plugin único `cooperative` | rechazado: mezcla seis ciclos de vida y crea un monolito interno |
| extender `business_partners` con campos de socio | rechazado: admisión, aportes y derechos políticos no son datos comerciales |
| usar `accounts_receivable` para préstamos | rechazado: no cubre aprobación, devengamiento, garantías, mora ni reglas prudenciales |
| usar `treasury` para cuentas de ahorro | rechazado: tesorería liquida caja/banco; el ahorro es una obligación individual |
| incorporar reglas INCOOP/SEPRELAD en ahorro y crédito | rechazado: impide versionado nacional y contamina dominios neutrales |
| seis plugins con contratos públicos | aceptado: preserva propietarios, activación y evolución independiente |

## Familia incorporada

1. `cooperative_membership` — socios y aportes;
2. `cooperative_governance` — asambleas y órganos;
3. `aml_compliance` — debida diligencia, riesgo, alertas y casos;
4. `cooperative_savings` — productos, cuentas y submayor de ahorros;
5. `cooperative_credit` — solicitud, aprobación y submayor de cartera;
6. `cooperative_regulatory_paraguay` — reglas, cálculos y presentaciones
   paraguayas.

El perfil reutiliza `business_partners`, `treasury` y `accounting`. El catálogo
global futuro queda en veintiocho reutilizables, pero la familia no renumera ERP
1–19 y no obliga a componer plugins comerciales o del proveedor.

## Riesgos identificados

- operar ahorros o créditos sin cerrar LA/FT, contabilidad y regulación;
- editar saldos en vez de usar libros append-only y reversos;
- diferencias no visibles entre submayor, caja/banco y mayor;
- reglas o indicadores desactualizados y sin checksum;
- exposición de datos personales, saldos o casos sensibles;
- falta de segregación entre creador y aprobador;
- migración que concilie filas pero no dinero, intereses, mora o garantías;
- presentar pruebas técnicas como autorización regulatoria.

ADR-0037 convierte estos riesgos en gates explícitos antes de datos reales y
producción.

## Revisión de documentación relacionada

Se actualizaron el índice ADR, el backlog, el roadmap, la vista de arquitectura,
la guía de implementación y el manual técnico. El manual de usuario y los runbooks
operativos no cambian porque todavía no existe pantalla, permiso, comando,
despliegue ni recorrido ejecutable de la familia.

No se regenera el PDF de cierre ni se decide un instalador por este corte
intermedio: ambos siguen ligados a la recongelación final de Sprint 8. La familia
cooperativa no se representa como implementada en la fotografía vigente.

## Pruebas aplicables

No corresponden Maven, JUnit, ArchUnit, PostgreSQL, Docker, Compose ni Playwright:
no se modificaron POM, Java, migraciones, descriptores, composición o UI. El gate
aplicable es la validación documental G0 de enlaces locales, UTF-8, mojibake y
coherencia de cantidades/estados.

## Resultado G0

Se ejecutó `tmp/validate_docs.py` con el runtime Python bundled de Codex después
de completar todos los cambios. Resultado sobre **283 archivos Markdown**:

- enlaces locales rotos: 0;
- errores UTF-8: 0;
- archivos con mojibake: 0;
- coincidencias con secretos locales: 0.
