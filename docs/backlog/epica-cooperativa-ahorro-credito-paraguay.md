# Épica — Cooperativa de ahorro y crédito de Paraguay

- Estado: Planificada; implementación no autorizada durante Sprint 8
- Familia obligatoria: `cooperative_membership`, `cooperative_governance`,
  `aml_compliance`, `cooperative_savings`, `cooperative_credit` y
  `cooperative_regulatory_paraguay`
- Fundaciones reutilizadas: `business_partners`, `treasury` y `accounting`
- Decisión: [ADR-0037](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md)
- Alcance: cooperativa paraguaya supervisada por INCOOP; confirmar tipo, nivel,
  estatuto y productos antes de implementar

## Objetivo

Administrar socios, aportes, gobierno democrático, prevención LA/FT, captación de
ahorros, cartera de créditos y obligaciones regulatorias paraguayas con fuentes de
verdad separadas, submayores reconciliables y reglas versionadas.

La épica no promete certificación ni autorización para captar ahorros. Cada
incremento regulatorio deberá verificarse contra las fuentes oficiales vigentes y
ser aceptado por responsables jurídicos, contables, de riesgos y cumplimiento de
la cooperativa.

## Perfil de composición previsto

| Capacidad | Plugin propietario | Dependencias públicas principales |
|---|---|---|
| persona u organización referenciada | `business_partners` | kernel multiempresa |
| membresía, aportes y estado de socio | `cooperative_membership` | `business-partners-api` |
| asambleas y órganos | `cooperative_governance` | `cooperative-membership-api` |
| debida diligencia y casos LA/FT | `aml_compliance` | referencia pública de socio |
| cuentas y submayor de ahorro | `cooperative_savings` | membresía, LA/FT y tesorería |
| préstamos y submayor de cartera | `cooperative_credit` | membresía, LA/FT y tesorería |
| caja, banco y liquidaciones | `treasury` | contratos financieros idempotentes |
| mayor, períodos y cierres | `accounting` | hechos publicados por los submayores |
| reportes y reglas de Paraguay | `cooperative_regulatory_paraguay` | proyecciones públicas de todos los anteriores |

No se incluyen por defecto ventas, inventario, POS, estaciones de servicio,
nómina, soporte integrado ni otros verticales. Una cooperativa multiactiva podrá
componerlos cuando su actividad los requiera, sin trasladar sus datos a la familia
financiera.

## Plan de trabajo

### COOP-00 — Gobierno, alcance y matriz normativa

Historia refinada: [COOP-00 — Gobierno, alcance y matriz normativa](COOP-00-gobierno-alcance-matriz-normativa.md).

- identificar tipo y nivel de supervisión, estatuto, sucursales y servicios;
- inventariar leyes, resoluciones, manuales, catálogos, formatos y plazos;
- descargar fuentes aplicables dentro de `.tools/downloads/`, verificar checksum y
  registrar vigencia;
- acordar producto inicial de ahorro y crédito, monedas, calendarios y redondeos;
- definir migración, privacidad, retención, respaldos y recuperación;
- aprobar segregación de funciones, permisos y matriz maker-checker;
- resolver contratos con tesorería, contabilidad y eventos/outbox;
- producir glosario, casos de uso, decisiones COOP-D01 en adelante y pruebas de
  caracterización sin copiar código legado.

**Salida:** alcance legal/funcional trazable y primer Sprint implementable. No se
acepta una lista informal de campos como sustituto.

### COOP-01 — `cooperative_membership`

- API Java pura con `MemberId`, referencias y consultas mínimas;
- solicitud, admisión, estado, categoría y número de socio;
- aportes suscritos/integrados en libro append-only con reversos;
- beneficiarios y documentos clasificados;
- suspensión, renuncia, exclusión, fallecimiento y liquidación;
- permisos, auditoría, historia efectiva y aislamiento empresarial;
- directorio, alta y ficha JSF Material Design 3 responsive;
- esquema `plg_cooperative_membership`, migraciones y conciliación de apertura.

**Fuera del corte:** cuentas de ahorro, préstamos, asientos y reportes oficiales.

### COOP-02 — `cooperative_governance`

- padrón elegible reproducible y snapshot por acto;
- convocatoria, agenda, asistencia, delegación y quórum;
- mociones, votaciones, resoluciones, actas y seguimiento;
- Consejo de Administración, Junta de Vigilancia, Tribunal Electoral aplicable y
  comités;
- mandatos, suplencias, vacancias, incompatibilidades y conflictos;
- calendario de obligaciones estatutarias/regulatorias;
- firma/evidencia, permisos separados y recorridos responsive.

**Fuera del corte:** elecciones remotas o firma electrónica con proveedor no
aprobado.

### COOP-03 — `aml_compliance`

- expediente KYC y debida diligencia;
- factores, perfil y clasificación de riesgo versionados;
- PEP, beneficiario final y resultados de listas cuando apliquen;
- revisiones periódicas y vencimientos documentales;
- API de decisión vigente para apertura/desembolso;
- puerto idempotente de observación transaccional;
- escenarios, alertas, casos, investigación y cierre;
- medidas reforzadas, aprobaciones, conservación y evidencia de reportes;
- seguridad negativa, consultas sensibles auditadas y logs sanitizados.

**Fuera del corte:** envío automático a autoridades sin formato, identidad y
autorización oficialmente verificados.

### COOP-04 — `cooperative_savings`

- primer producto de ahorro versionado y aprobado;
- apertura/cierre de cuenta y titulares válidos;
- depósitos, retiros y reversos idempotentes;
- saldos contable, disponible, retenido y devengado;
- interés, base de días, redondeo, capitalización y fecha valor;
- restricciones y garantías con autoridad/vigencia;
- cierre diario y extracto reproducible;
- integración con tesorería sin duplicar caja o banco;
- hechos contables y conciliación submayor–tesorería–mayor;
- concurrencia, fallas parciales y operación degradada visible.

**Fuera del corte:** tarjetas, ATM, billeteras, QR y conectores de red.

### COOP-05 — `cooperative_credit`

- primer producto y política de crédito versionados;
- solicitud, evaluación, capacidad de pago y documentación;
- aprobación por niveles/comité y excepción justificada;
- garantías, avalúos y referencias públicas;
- desembolso idempotente y plan de cuotas reproducible;
- capital, interés, cargos, pago e imputación por componente;
- mora, refinanciación, reestructuración, castigo y recuperación;
- cobranza, promesas y expediente operativo;
- cierre diario, hechos contables y conciliación de cartera;
- retención de ahorro como garantía mediante comando público.

**Fuera del corte:** modelos opacos de IA que aprueben o rechacen crédito sin
explicación y revisión humana.

### COOP-06 — `cooperative_regulatory_paraguay`

- registro versionado de fuentes y paquetes regulatorios;
- tipificación/nivel aplicable y parámetros efectivos;
- mapeo al plan de cuentas cooperativo;
- cálculos prudenciales aprobados con doble cálculo;
- proyecciones e informes INCOOP reproducibles;
- configuración y artefactos SEPRELAD autorizados;
- validación, aprobación, envío, respuesta, rechazo y rectificación;
- checksum de artefactos y evidencia de datos/reglas usados;
- calendario de obligaciones y alertas de cambio normativo.

**Fuera del corte:** declarar conformidad cuando falte una fuente vigente,
validación experta o aceptación oficial.

### COOP-07 — Migración y piloto controlado

- perfilar origen y calidad con autorización expresa;
- migrar socios, aportes, ahorros, créditos y saldos por lotes idempotentes;
- reconciliar conteos, capital, intereses, mora, garantías y mayor contable;
- conservar trazabilidad origen–destino, errores y cuarentena;
- ejecutar doble corrida y operación paralela con datos protegidos;
- probar respaldo, restauración, rollback y continuidad;
- completar demo real con datos ficticios y aceptación independiente;
- actualizar manuales, fotografía, PDF y decisión de instalador del Sprint.

## Límites de dominio

| Dato o proceso | Propietario | No debe poseerlo |
|---|---|---|
| persona/organización y canales | `business_partners` | membresía |
| condición de socio y aportes | `cooperative_membership` | participantes, tesorería |
| asamblea, órgano y mandato | `cooperative_governance` | kernel |
| expediente, alerta y caso LA/FT | `aml_compliance` | ahorros, créditos |
| producto, cuenta y saldo de ahorro | `cooperative_savings` | catálogo, tesorería |
| producto, préstamo y saldo de cartera | `cooperative_credit` | ventas, cuentas por cobrar |
| caja, banco y liquidación | `treasury` | ahorros, créditos |
| cuenta contable, asiento y cierre | `accounting` | regulación Paraguay |
| paquete y presentación regulatoria | `cooperative_regulatory_paraguay` | dominios fuente |

Todo cruce usa IDs, puertos, comandos o eventos públicos. Se prohíben asociaciones
JPA, joins, SQL, repositorios y DTO internos entre esquemas.

## Reglas financieras transversales

- usar decimal exacto, moneda y escala explícitas;
- separar fecha de operación, fecha valor y fecha contable;
- versionar tasa, base de días, redondeo, calendario y vigencia;
- mantener libro append-only; corregir mediante reverso y nueva entrada;
- exigir idempotencia y correlación en todos los efectos monetarios;
- cerrar/reabrir períodos con permiso y evidencia;
- conciliar diariamente submayores, tesorería y contabilidad;
- no permitir edición manual directa de un saldo derivado;
- conservar snapshots de contratos, reglas y decisiones aplicadas;
- demostrar recuperación después de fallo parcial o recreación de contenedores.

## Criterios de aceptación de la épica

- **COOP-CE01:** los seis plugins tienen API pública cuando corresponda, esquema,
  migraciones, descriptor, permisos, menú, pruebas y documentación propios.
- **COOP-CE02:** no existen JPA, SQL, repositorios, DTO ni tablas cruzadas.
- **COOP-CE03:** socio y participante se vinculan por IDs sin compartir entidad.
- **COOP-CE04:** aportes, ahorros y créditos usan libros append-only y reversos.
- **COOP-CE05:** cada efecto monetario es idempotente y reconciliable con tesorería
  y contabilidad.
- **COOP-CE06:** `accounts_receivable` no contiene cartera cooperativa.
- **COOP-CE07:** producto/condición financiera conserva versión, vigencia y
  snapshot aplicado.
- **COOP-CE08:** LA/FT puede impedir o escalar una operación mediante decisión
  pública, sin acceder al saldo privado.
- **COOP-CE09:** gobierno conserva padrón, quórum, decisiones y mandatos
  reproducibles.
- **COOP-CE10:** cada regla o artefacto paraguayo identifica fuente, versión,
  vigencia y checksum.
- **COOP-CE11:** reportes presentados son inmutables; una corrección es una nueva
  versión o rectificación trazable.
- **COOP-CE12:** permisos y pruebas negativas cubren empresa, rol, objeto,
  segregación y operación sensible.
- **COOP-CE13:** logs, URLs, errores, demos y auditoría general no exponen datos
  personales, saldos ni detalles de casos LA/FT.
- **COOP-CE14:** cierre, redondeo, concurrencia, repetición, reverso y fallas
  parciales tienen pruebas deterministas.
- **COOP-CE15:** UI y Playwright cubren 375, 720 y 1280 px, teclado, foco, vacío,
  error, listas grandes, denegación e inactivos.
- **COOP-CE16:** desactivar o retirar conserva migraciones, libros y evidencia.
- **COOP-CE17:** migración reconcilia importes además de conteos y tiene rollback.
- **COOP-CE18:** manual de usuario, manual técnico, guía de implementación,
  fotografía, demo, PDF e instalador se gobiernan según el Sprint.
- **COOP-CE19:** ninguna capacidad se presenta como certificada sin validación
  oficial y profesional aplicable.

## Decisiones obligatorias antes del código

1. tipo/nivel de cooperativa y alcance del estatuto;
2. productos, monedas, calendarios, tasas, cargos y redondeos iniciales;
3. tratamiento de aportes, excedentes y desvinculación;
4. órganos, elecciones, quórum y firma/evidencia aplicables;
5. matriz LA/FT vigente, responsables, retención y canal de reporte;
6. plan de cuentas y mapeos regulatorios vigentes;
7. fuente de índice de morosidad y otros parámetros variables;
8. contratos síncronos/asíncronos con tesorería y contabilidad;
9. segregación maker-checker y límites operativos;
10. origen, calidad, reconciliación, privacidad y rollback de migración.

## No incluido en el primer perfil

- autoservicio web/móvil del socio;
- tarjetas, ATM, QR, billeteras o transferencias por redes externas;
- conectores específicos a bancos, scoring, firma, listas, SMS o correo;
- seguros, vivienda, producción, educación u otras secciones cooperativas;
- cobranza judicial y bienes adjudicados;
- decisiones crediticias autónomas mediante IA;
- certificación legal implícita por ejecutar pruebas técnicas.

## Inicio autorizado

Esta épica agrega planificación, no código. Al 2026-08-04 `treasury` y
`accounting` todavía no están implementados y Sprint 8 continúa abierto. COOP-00
sólo podrá entrar a un Sprint futuro mediante prioridad expresa de producto y con
sus predecesores/contratos resueltos. No se adelanta sobre el trabajo actual.

## Referencias

- [ADR-0037 — Familia cooperativa](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md)
- [Roadmap general](epica-roadmap-plugins-productivos.md)
- [ADR-0013 — Eventos e idempotencia](../adr/0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0016 — Autorización y auditoría](../adr/0016-autorizacion-y-auditoria-operaciones-plugin.md)
- [ADR-0028 — Gobierno de selectores](../adr/0028-gobierno-de-selectores-y-datos-administrables.md)
- [Evidencia del análisis normativo](../evidence/analisis-cooperativa-ahorro-credito-paraguay.md)
- [Límites y dependencias](../architecture/cooperative-savings-credit-boundaries.md)
- [Alcance regulatorio inicial](../knowledge-base/cooperative-savings-credit/regulatory-scope-analysis.md)
- [Evidencia del refinamiento COOP-00](../evidence/COOP-00-refinamiento-plan.md)
