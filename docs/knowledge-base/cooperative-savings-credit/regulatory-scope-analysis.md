# Alcance regulatorio inicial de cooperativas de ahorro y crédito

- Fecha de consulta: 2026-08-04
- País asumido: Paraguay
- Estado: análisis inicial para COOP-00; inventario no exhaustivo
- Uso: fuente de requisitos y decisiones, no certificación ni opinión legal
- ADR rector: [ADR-0037](../../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md)

## Propósito

Separar hechos observados en fuentes oficiales de las decisiones funcionales que
debe tomar una cooperativa concreta antes de implementar membresía, ahorro,
crédito, LA/FT o reportes. Ningún número, plazo, catálogo o formato de este
documento se convierte automáticamente en una regla ejecutable.

COOP-00 deberá descargar la edición aplicable dentro de `.tools/downloads/`,
registrar URL, fecha, tamaño, versión y SHA-256, identificar modificatorias y
obtener validación jurídica/contable/de cumplimiento. Hasta entonces el estado es
`REFERENCIA_CONSULTADA`, no `FUENTE_CONGELADA`.

## Registro inicial de fuentes

| ID | Fuente oficial | Superficie observada | Estado actual |
|---|---|---|---|
| COOP-N01 | [Ley N.º 438/1994](https://www.bacn.gov.py/leyes-paraguayas/2373/ley-n-438-cooperativas-br) | naturaleza, socios, aportes, asambleas y órganos | referencia consultada; consolidación pendiente |
| COOP-N02 | [Ley N.º 5501/2015](https://www.bacn.gov.py/leyes-paraguayas/4482/modi) | modificaciones de membresía, control democrático y órganos | referencia consultada; modificatorias posteriores pendientes |
| COOP-N03 | [Ley N.º 2.157/2003](https://www.bacn.gov.py/leyes-paraguayas/2423/regula-el-funcionamiento-del-instituto-nacional-de-cooperativismo-y-establece-su-carta-organica) | autoridad, fiscalización y funciones del INCOOP | referencia consultada |
| COOP-N04 | [Resolución INCOOP N.º 22.668/2020](https://www.incoop.gov.py/?p=9655) | marco regulatorio de ahorro y crédito; abroga el de 2017 | landing oficial verificada; resolución/checksum pendientes |
| COOP-N05 | [Resolución INCOOP N.º 12.147/2014](https://www.incoop.gov.py/?p=4330) | plan de cuentas cooperativo estandarizado | landing oficial verificada; consolidación de cambios pendiente |
| COOP-N06 | [Resolución INCOOP N.º 18.398/2018](https://www.incoop.gov.py/?p=7247) | manual de cuentas del sector | landing oficial verificada; checksum pendiente |
| COOP-N07 | [Resolución SEPRELAD N.º 156/2020](https://www.seprelad.gov.py/resoluciones/resoluciones/resolucion-n-156-2020-reglamento-de-prevencion-de-la-y-ft-para-cooperativas-sujetas-a-la-incoop.pdf) | administración y gestión de riesgos LA/FT | PDF oficial localizado; descarga/checksum pendientes |
| COOP-N08 | [Resolución INCOOP N.º 22.957/2020](https://www.incoop.gov.py/?p=9815) | matriz basada en riesgo LA/FT para captación y crédito | landing y anexos oficiales localizados |
| COOP-N09 | [Resolución INCOOP N.º 154/2026](https://www.incoop.gov.py/?p=14383) | modificación reciente de la matriz LA/FT | landing oficial verificada; impacto exacto pendiente |
| COOP-N10 | [Comunicado INCOOP N.º 25/2026](https://www.incoop.gov.py/?p=14665) | promedio ponderado de morosidad del primer trimestre de 2026 | evidencia de parámetro periódico |
| COOP-N11 | [Comunicado INCOOP N.º 36/2024](https://www.incoop.gov.py/?p=13400) | tabla de obligaciones con plazo actualizada a julio de 2024 | referencia de calendario; verificar edición más reciente |
| COOP-N12 | [Central de Riesgos del INCOOP](https://www.incoop.gov.py/?page_id=5072) | consulta y archivos de operaciones crediticias | servicio/formato identificado; especificación vigente pendiente |
| COOP-N13 | [Resolución INCOOP N.º 14.877/2016](https://www.incoop.gov.py/?p=5695) | regulación de la Central de Riesgos Crediticios | landing, manual y formato localizados |
| COOP-N14 | [Comunicado INCOOP N.º 12/2026](https://www.incoop.gov.py/?p=14008) | Central de Riesgos, SICOOP y Alerta Temprana operativos; esta última mediante VPN coordinada | evidencia operativa actual; topología contractual pendiente |
| COOP-N15 | [Formatos complementarios CAC](https://www.incoop.gov.py/?p=6178) | formatos A/B/C con actualizaciones publicadas hasta 2024 | inventario inicial; vigencia por formato pendiente |

El registro se ampliará con estatuto social, resoluciones internas, manual de
crédito, manual de prevención, reglamentos de ahorro/crédito, contratos, políticas
contables y cualquier norma posterior aplicable a la entidad real.

## Superficies funcionales derivadas

| Superficie | Evidencia inicial | Plugin candidato | Verificación pendiente |
|---|---|---|---|
| admisión, estado y retiro de socios | COOP-N01/N02 | `cooperative_membership` | estatuto, categorías y causales reales |
| aportes y capital social | COOP-N01/N02 | `cooperative_membership` | cuota, periodicidad, integración y devolución |
| asambleas y órganos | COOP-N01/N02/N11 | `cooperative_governance` | calendario, quórum, elecciones y firmas |
| tipificación/supervisión | COOP-N03/N04/N11 | `cooperative_regulatory_paraguay` | tipo/nivel vigente de la cooperativa |
| captación de ahorros | COOP-N04 | `cooperative_savings` | productos, contratos, tasas y límites aprobados |
| concesión de créditos | COOP-N04/N12/N13 | `cooperative_credit` | productos, manual, comité, garantías y reporte SCR |
| solvencia, liquidez, mora y previsiones | COOP-N04/N10 | `cooperative_regulatory_paraguay` | fórmulas, períodos y parámetros vigentes |
| plan/manual de cuentas | COOP-N05/N06 | `accounting` + adaptador paraguayo | versión homologada y mapeos institucionales |
| prevención LA/FT | COOP-N07/N08/N09 | `aml_compliance` + paquete paraguayo | matriz, responsables, reportes y retención |
| obligaciones e informes | COOP-N11/N14/N15 | `cooperative_regulatory_paraguay` | canal, frecuencia, firma, formato y respuesta |

## Hallazgos que condicionan la arquitectura

1. La regulación distingue información social, financiera, prudencial y de
   prevención; no corresponde un único agregado o esquema.
2. El plan de cuentas y sus modificaciones exigen mapeo versionado, pero el mayor
   sigue perteneciendo a `accounting`.
3. El índice de morosidad comunicado periódicamente demuestra que determinados
   parámetros cambian fuera del ciclo de release del ERP.
4. La matriz LA/FT tuvo una modificación oficial en 2026; reglas y resultados
   deben identificar el paquete aplicado.
5. Central de Riesgos y SICOOP usan formatos y canales propios. Generar el
   artefacto y transmitirlo son responsabilidades separables.
6. La existencia de VPN o un sistema oficial no autoriza al ERP a administrar
   credenciales, abrir túneles o automatizar envíos sin un diseño técnico y
   contractual aprobado.
7. Las obligaciones varían según tipificación, documento y período; no debe
   publicarse un calendario antes de confirmar la edición aplicable.

## Matriz de trazabilidad que debe completar COOP-00

Cada requisito aceptado tendrá una fila con estas columnas:

| Campo | Contenido exigido |
|---|---|
| `RequirementId` | identidad estable `COOP-RQ-###` |
| fuente | `COOP-N##`, artículo/numeral/anexo y checksum |
| vigencia | desde/hasta, modificatoria y estado |
| interpretación | explicación neutral validada |
| propietario | plugin y agregado/caso de uso |
| dato fuente | campo/proyección y clasificación |
| regla | versión, redondeo, calendario y parámetros |
| permiso | actor, empresa y segregación requerida |
| evidencia | documento, evento, auditoría o artefacto |
| prueba | ejemplo normal, límite, negativo y rectificación |
| responsable | producto, jurídico, contabilidad, riesgos o cumplimiento |
| estado | propuesta, validada, implementada, verificada o retirada |

No se aceptará una regla regulatoria sin fuente exacta ni una fuente sin caso de
uso, dato, prueba y responsable.

## Preguntas abiertas para la cooperativa concreta

- ¿Es especializada o multiactiva y cuál es su tipo/nivel vigente?
- ¿Qué estatuto y reglamentos están aprobados y desde cuándo?
- ¿Qué clases de socio, aportes, sucursales y monedas opera?
- ¿Qué productos de ahorro y crédito deben entrar en el primer piloto?
- ¿Qué manual de crédito, niveles/comités y garantías usa?
- ¿Qué plan de cuentas institucional está homologado?
- ¿Qué responsables y matriz LA/FT están vigentes?
- ¿Qué sistemas INCOOP usa hoy y cómo presenta cada informe?
- ¿Qué fuentes contienen socios, saldos, cuotas, garantías y contabilidad?
- ¿Qué volumen, calidad, cierres pendientes y diferencias existen?

Estas respuestas son entradas obligatorias de COOP-00. No deben inferirse del
nombre de la institución ni de una copia histórica del sistema.
