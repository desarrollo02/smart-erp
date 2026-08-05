# ADR-0037 — Familia para cooperativas de ahorro y crédito de Paraguay

- Estado: Aceptado
- Fecha: 2026-08-04
- Decisión de producto: agregar al plan los plugins necesarios para administrar
  una cooperativa de ahorro y crédito paraguaya
- Modifica: catálogo futuro y perfiles de composición; no renumera la secuencia
  ERP 1–19 ni autoriza código durante Sprint 8
- Alcance regulatorio: planificación arquitectónica, no certificación legal ni
  declaración de conformidad

## Contexto

Una cooperativa de ahorro y crédito no puede modelarse como una empresa comercial
que vende préstamos y mantiene cuentas por cobrar. La Ley N.º 438/1994 y sus
modificatorias regulan la organización cooperativa, la membresía y el control
democrático. La Ley N.º 2.157/2003 establece al INCOOP como autoridad de aplicación
y control. Para la actividad financiera, la Resolución INCOOP N.º 22.668/2020
aprobó el marco regulatorio de las cooperativas del sector de ahorro y crédito.

Ese marco distingue captación de ahorros, colocación de créditos, solvencia,
liquidez, morosidad, previsiones, gobierno, contabilidad e información de
supervisión. INCOOP continúa publicando variables de aplicación: por ejemplo, el
Comunicado N.º 25/2026 informó el promedio ponderado de morosidad del primer
trimestre de 2026. Además, la Resolución SEPRELAD N.º 156/2020 establece un
reglamento basado en riesgo de LA/FT para cooperativas supervisadas por INCOOP, y
la Resolución INCOOP N.º 154/2026 modificó la matriz sectorial de riesgo.

Por tanto, tasas, categorías, límites, formatos, matrices e indicadores no pueden
quedar hardcodeados como reglas permanentes ni inferirse de un sistema legado. Se
necesita conservar fuente oficial, versión, vigencia y checksum de cada paquete
regulatorio antes de usarlo para una decisión o reporte.

El roadmap vigente ya separa participantes, tesorería, cuentas por cobrar y
contabilidad. Reutilizar esas capacidades sin definir nuevos propietarios
provocaría errores de dominio:

- un socio no es solamente un cliente;
- un aporte social no es una venta ni un precio de catálogo;
- el ahorro del socio es un pasivo con submayor propio, no saldo de caja;
- un préstamo cooperativo no es una factura pendiente en
  `accounts_receivable`;
- tesorería liquida dinero, pero no posee el saldo contractual del ahorro o
  crédito;
- contabilidad recibe hechos reconciliables, pero no gobierna la operación;
- gobierno, LA/FT y reportes regulatorios tienen historia y autorizaciones que no
  pertenecen al kernel.

## Decisión

### 1. Familia y cantidad

Se agregan seis plugins funcionales reutilizables al catálogo futuro:

| Orden interno | Plugin | Responsabilidad principal |
|---:|---|---|
| C1 | `cooperative_membership` | socios, admisión, estado, aportes y desvinculación |
| C2 | `cooperative_governance` | asambleas, órganos electivos, mandatos y decisiones |
| C3 | `aml_compliance` | debida diligencia, perfiles de riesgo, alertas y casos LA/FT |
| C4 | `cooperative_savings` | productos de ahorro, cuentas, saldos, intereses y restricciones |
| C5 | `cooperative_credit` | productos, solicitudes, aprobación, cartera, garantías y cobranza |
| C6 | `cooperative_regulatory_paraguay` | reglas, proyecciones y artefactos INCOOP/SEPRELAD versionados |

El catálogo global planificado pasa de veintidós a **veintiocho plugins
reutilizables**: diecinueve ERP, tres de operaciones del proveedor y seis de la
familia cooperativa. La cifra describe el catálogo, no una distribución que deba
contenerlos a todos.

La familia cooperativa constituye un perfil vertical separado y no recibe los
números 20–25 del roadmap ERP. Su composición mínima futura reutilizará, cuando
estén disponibles y estables, `business_partners`, `treasury`, `accounting` y los
seis plugins anteriores. Los demás plugins ERP serán opcionales según las otras
actividades de la cooperativa. `human_resources` y `payroll` podrán administrar a
sus empleados, pero no son requisito para el dominio financiero de socios.

Esta decisión no reordena los plugins ERP 1–19, no adelanta trabajo respecto de
Sprint 8 y no crea módulos Maven, descriptores, esquemas ni perfiles ejecutables.
Una priorización futura distinta requerirá una decisión expresa de producto y la
revisión de predecesores técnicos.

### 2. Propiedad de `cooperative_membership`

Será dueño de:

- solicitud, admisión, número y condición de socio por empresa;
- categoría, fecha de ingreso, estado, motivos y vigencias;
- aceptación de estatuto, declaraciones y documentos propios de la membresía;
- suscripción e integración de aportes sociales, cuotas y saldos de capital;
- beneficiarios y referencias necesarias, con clasificación de privacidad;
- suspensión, renuncia, exclusión, fallecimiento y liquidación del vínculo;
- historia efectiva, certificados y auditoría funcional.

Requerirá `business-partners-api` para referenciar a la persona u organización,
pero no copiará su maestro ni convertirá automáticamente al socio en cliente,
proveedor o usuario. La identidad pública será `MemberId`; el participante se
vinculará por un `BusinessPartnerId` opaco y verificable por empresa.

Los aportes se registrarán como un submayor append-only con reversos explícitos,
no como una columna editable de saldo. El plugin emitirá instrucciones o hechos
idempotentes hacia tesorería y contabilidad mediante contratos públicos. No
poseerá cajas, cuentas bancarias ni asientos.

Su esquema privado previsto será `plg_cooperative_membership`.

### 3. Propiedad de `cooperative_governance`

Será dueño de:

- padrón elegible derivado de referencias públicas de socios y reglas vigentes;
- convocatorias, agenda, documentación, asistencia, delegaciones y quórum;
- asambleas ordinarias y extraordinarias, mociones, votaciones y resoluciones;
- Consejo de Administración, Junta de Vigilancia, Tribunal Electoral cuando
  corresponda, comités y otros órganos estatutarios aprobados;
- cargos, elecciones, mandatos, suplencias, vacancias y conflictos de interés;
- actas, firmas/evidencias, observaciones y obligaciones de seguimiento;
- calendario de vencimientos estatutarios y regulatorios.

Requerirá la API pública de `cooperative_membership`; conservará snapshots del
padrón y de la identidad visible usados en cada acto para que cambios posteriores
no reescriban la historia. No será dueño del legajo laboral de empleados ni de la
identidad OIDC.

Su esquema privado previsto será `plg_cooperative_governance`.

### 4. Propiedad de `aml_compliance`

Será dueño de:

- expediente de conocimiento y debida diligencia del socio o contraparte;
- perfil, factores y clasificación de riesgo con versión y vigencia;
- declaraciones, beneficiario final cuando aplique, PEP y resultados de listas;
- revisiones periódicas, vencimientos documentales y decisiones de aceptación;
- observaciones transaccionales tipadas recibidas desde dominios autorizados;
- reglas, escenarios, alertas, asignación, investigación y cierre de casos;
- aprobaciones, medidas reforzadas, conservación y evidencia de reporte;
- auditoría de consultas y cambios sensibles sin exponer payloads en logs.

La API pública expondrá decisiones mínimas como `DueDiligenceDecision` y un puerto
para observaciones idempotentes. `cooperative_savings` y `cooperative_credit`
podrán exigir una decisión vigente antes de abrir, desembolsar o ejecutar una
operación de riesgo. `aml_compliance` no leerá sus tablas ni alterará sus saldos.

Las listas, criterios y umbrales se conservarán como paquetes de política
versionados dentro de este dominio. El adaptador paraguayo podrá registrarlos
mediante una API pública de configuración; `aml_compliance` no dependerá de su
implementación. `cooperative_regulatory_paraguay` consumirá después una proyección
pública de resultados, manteniendo una sola dirección y ningún ciclo.

Su esquema privado previsto será `plg_aml_compliance`.

### 5. Propiedad de `cooperative_savings`

Será dueño de:

- productos y versiones de ahorro, moneda, plazo, tasa, base y calendario;
- cuentas, titulares, estado, apertura, renovación, cierre y beneficiarios;
- libro auxiliar append-only de depósitos, retiros, ajustes y reversos;
- saldo contable, disponible, retenido, devengado y conciliado;
- intereses, capitalización, vencimiento y certificados de depósito cuando el
  alcance aprobado los incluya;
- retenciones, garantías, embargos u otras restricciones con autoridad y vigencia;
- cuentas inactivas o dormidas, avisos, extractos y cierre diario;
- conciliación con liquidaciones de tesorería y hechos contables publicados.

Requerirá membresía y una decisión LA/FT vigente conforme a la política del
producto. Usará un contrato público de tesorería para liquidaciones idempotentes;
tesorería conservará caja/banco y este plugin conservará la obligación con el
socio. Las transacciones coordinadas deberán ser atómicas cuando el contrato y
JTA lo permitan o usar una saga/outbox visible y reconciliable; nunca se ocultará
una diferencia.

Los productos de ahorro no pertenecen a `commercial_catalog`. Tasas y condiciones
se versionan; una modificación futura no recalcula el pasado. Los importes usarán
tipos decimales, moneda y escala explícitas, nunca `double`.

Su esquema privado previsto será `plg_cooperative_savings`.

### 6. Propiedad de `cooperative_credit`

Será dueño de:

- productos y políticas de crédito versionados;
- solicitud, propósito, capacidad de pago, evaluación y documentación;
- análisis, puntaje o clasificación interna explicable;
- comité, niveles de aprobación, condiciones y excepciones autorizadas;
- codeudores, garantías personales/reales, avalúos y coberturas;
- contrato, desembolso, plan de cuotas y libro auxiliar de la cartera;
- capital, interés, cargos, pagos, imputación, mora y saldo por componente;
- refinanciación, reestructuración, prórroga, castigo y recuperación;
- gestión de cobranza, compromisos y expedientes vinculados;
- cierre diario, conciliación y hechos para riesgo, tesorería y contabilidad.

Requerirá membresía, decisión LA/FT y tesorería. Podrá solicitar a
`cooperative_savings` una retención tipada cuando un ahorro garantice un crédito,
sin leer ni modificar su saldo privado.

La cartera de préstamos **no** se implementará en `accounts_receivable`: ese
plugin seguirá siendo dueño de deuda comercial derivada de ventas/documentos. La
cartera cooperativa tiene devengamiento, imputación, garantías, mora,
reestructuración y reglas prudenciales propias.

Su esquema privado previsto será `plg_cooperative_credit`.

### 7. Propiedad de `cooperative_regulatory_paraguay`

Será el adaptador nacional y será dueño de:

- inventario de leyes, resoluciones, manuales, catálogos, formatos e indicadores;
- paquetes regulatorios con emisor, número, versión, vigencia, checksum y estado;
- tipificación y parámetros de supervisión aplicables a cada cooperativa;
- mapeo versionado al plan de cuentas cooperativo sin poseer el mayor contable;
- cálculos prudenciales reproducibles de liquidez, solvencia, morosidad,
  clasificación, previsiones, concentración y otros que se aprueben;
- proyecciones y validaciones de informes INCOOP;
- configuración sectorial y artefactos autorizados para SEPRELAD;
- lotes, responsables, aprobaciones, envíos, respuestas, rechazos y reproducción;
- evidencia del conjunto exacto de fuentes y datos usados en cada presentación.

Consumirá contratos públicos o proyecciones inmutables de membresía, gobierno,
LA/FT, ahorros, créditos y contabilidad. No será dueño de socios, cuentas,
préstamos, alertas, asientos ni saldos fuente. Una corrección regulatoria generará
una nueva versión o una presentación rectificativa; no reescribirá artefactos
presentados.

El plan de cuentas estandarizado se configurará en `accounting` mediante un
contrato público y mapeos versionados. No se crearán entidades contables paralelas
ni SQL hacia su esquema.

Su esquema privado previsto será `plg_cooperative_regulatory_paraguay`.

### 8. Grafo y composición

La dirección funcional prevista es:

```text
business-partners-api --> cooperative_membership
cooperative_membership --> cooperative_governance
cooperative_membership --> aml_compliance
cooperative_membership + aml-compliance-api + treasury-api
    --> cooperative_savings
cooperative_membership + aml-compliance-api + treasury-api
    --> cooperative_credit
cooperative_credit --> cooperative-savings-api para solicitar retenciones

membership + governance + aml + savings + credit + accounting-api
    --> cooperative_regulatory_paraguay

savings/credit/membership --hechos idempotentes--> treasury/accounting
```

Las flechas expresan consumo de contratos, no acceso a implementaciones. La
dependencia exacta y su carácter `REQUIRED` u `OPTIONAL` se congelarán en la
historia de diseño de cada plugin. Ninguna decisión autoriza ciclos, relaciones
JPA, joins, repositorios, DTO internos o SQL entre esquemas.

El perfil vertical deberá demostrar al menos estas variantes físicas:

- base sin familia cooperativa;
- membresía/gobierno sin operación financiera;
- cooperativa de ahorro y crédito completa;
- familia completa con `support_connector` ausente y presente;
- retiro físico de un plugin opcional sin pérdida de datos.

### 9. Controles financieros y seguridad

Antes de usar dinero real, cada submayor deberá cumplir:

- identidad empresarial y moneda/escala explícitas;
- fecha de operación, fecha valor, zona horaria y día hábil gobernados;
- entradas append-only, reversos explícitos y versión optimista;
- idempotencia de depósito, retiro, desembolso, cobro, devengamiento y cierre;
- tasas, bases de días, redondeo y calendarios versionados;
- cierres diarios/mensuales y reapertura excepcional autorizada;
- conciliación de submayor, tesorería, mayor contable y reportes;
- segregación creador/aprobador para productos, tasas, créditos, excepciones,
  reversos y presentaciones regulatorias;
- límites transaccionales y acumulados aplicados en servidor;
- auditoría por empresa, actor, socio, operación, motivo y correlación;
- privacidad, cifrado aplicable, retención y enmascarado definidos antes de datos
  reales;
- pruebas negativas de empresa, rol, objeto, concurrencia, repetición y fechas.

No se registrarán documentos personales, beneficiarios, saldos, detalles de
alertas o reportes sospechosos en logs generales. Las demos usarán exclusivamente
datos ficticios.

### 10. Orden de entrega y gates

El orden C1–C6 es una dirección de construcción de la familia, no autorización de
inicio. Se planifican estos cortes:

1. **COOP-00 — Gobierno y matriz normativa:** tipo/nivel de cooperativa, estatuto,
   productos iniciales, fuentes oficiales vigentes, privacidad, contabilidad,
   migración y decisiones abiertas;
2. **COOP-01 — Membresía y aportes:** caracterización, API, dominio, esquema,
   aplicación, seguridad, UI, composición y demo;
3. **COOP-02 — Gobierno cooperativo:** asambleas, órganos, mandatos, actas y
   vencimientos;
4. **COOP-03 — LA/FT:** debida diligencia, riesgo, observaciones, alertas y casos;
5. **COOP-04 — Ahorros:** primer producto, cuenta, submayor, interés, caja/banco,
   cierre y conciliación;
6. **COOP-05 — Créditos:** primer producto, aprobación, desembolso, cuotas,
   garantías, mora y cobranza;
7. **COOP-06 — Regulación Paraguay:** plan de cuentas, cálculos, informes y
   artefactos versionados;
8. **COOP-07 — Migración y piloto controlado:** doble corrida, conciliación,
   recuperación, operación paralela y aceptación independiente.

La primera operación financiera no podrá declararse lista para producción por
tener sólo ahorros o créditos visibles. Requiere los gates aplicables de
membresía, LA/FT, tesorería, contabilidad, regulación, respaldo, recuperación,
seguridad, conciliación, documentación, demo y validación independiente.

Antes de COOP-00 deberán existir contratos públicos estables de
`business_partners`, `treasury` y `accounting`, o una ADR aprobada que cambie esa
precedencia. Al 2026-08-04 solamente `business_partners` está implementado; esta
familia permanece planificada.

### 11. Experiencia visual y permisos

Las pantallas usarán Jakarta Faces 4.1, Material Design 3, contratos neutrales y
los rangos 375, 720 y 1280 px. Cada selector declarará fuente, propietario y
clase. Productos de ahorro/crédito, categorías internas y autoridades
administrables tendrán rutas autorizadas e historia; estados cerrados, permisos y
códigos regulatorios no admitirán altas arbitrarias.

Los permisos se congelarán por historia. Como mínimo se separarán consulta,
alta/modificación, aprobación, desembolso/retiro, reverso, cierre, exportación y
presentación regulatoria. La UI nunca sustituirá la autorización del servicio.

### 12. Extensiones no obligatorias del primer perfil

No se agregan ahora como plugins obligatorios:

- portal o aplicación de autoservicio del socio;
- tarjetas, ATM, billeteras, QR o redes de pago;
- conectores bancarios, SIPAP/SPI o proveedores de cobranza;
- scoring, listas, firma, SMS, correo o biometría de un proveedor específico;
- seguros, solidaridad, educación, vivienda o producción cooperativa;
- cobranza judicial, bienes adjudicados o central cooperativa;
- analítica, data warehouse o modelos de IA de decisión crediticia.

Estas capacidades requieren caracterización y, cuando corresponda, plugins o
adaptadores técnicos separados. Un futuro `member_self_service` sólo podrá
consumir APIs públicas y nunca poseer saldos. Cada conector externo deberá aprobar
proveedor, contrato, autenticación, residencia de datos, idempotencia, operación
degradada y threat model antes de incorporarse al catálogo.

## Consecuencias

### Positivas

- socio, ahorro, crédito, dinero, contabilidad y regulación tienen propietarios
  explícitos;
- la cooperativa puede activar gobierno sin abrir todavía operación financiera;
- reglas paraguayas volátiles quedan aisladas y reproducibles;
- la cartera crediticia no contamina cuentas por cobrar comerciales;
- saldos y reportes pueden conciliarse sin acceso cruzado a tablas;
- la familia puede reutilizarse en una composición vertical sin enviar plugins
  comerciales o del proveedor que no necesite.

### Costes y riesgos

- se agregan seis plugins y varios contratos financieros críticos;
- el cierre diario y la conciliación entre submayores requieren diseño y pruebas
  rigurosos;
- gobierno y LA/FT tratan datos sensibles y exigen segregación de funciones;
- la normativa y los indicadores cambian y necesitan mantenimiento continuo;
- migrar socios, ahorros, créditos y contabilidad exige reconciliación monetaria,
  no sólo conteo de filas;
- una cooperativa multiactiva puede requerir además plugins ERP de ventas,
  inventario, nómina u otros verticales.

## Alternativas descartadas

### Un único plugin `cooperative`

Se descarta porque mezclaría membresía, gobierno, dos submayores financieros,
casos LA/FT y regulación nacional, impediría activación/evolución independiente y
crearía un nuevo monolito dentro del monolito modular.

### Usar `business_partners` como maestro de socios

Se descarta porque un participante comercial no posee admisión, aportes, padrón,
derechos políticos ni desvinculación cooperativa. Se reutiliza su referencia sin
trasladarle esas reglas.

### Usar `accounts_receivable` para préstamos

Se descarta porque una deuda comercial no modela producto financiero, aprobación,
devengamiento, imputación, garantías, refinanciación ni clasificación prudencial.

### Usar `treasury` para cuentas de ahorro

Se descarta porque caja y banco representan disponibilidad y liquidación de la
cooperativa; la cuenta de ahorro representa una obligación contractual individual
con el socio.

### Incorporar INCOOP y SEPRELAD dentro de los dominios neutrales

Se descarta porque acoplaría membresía, ahorros, crédito y LA/FT a resoluciones y
formatos paraguayos que cambian. El adaptador nacional consume proyecciones y
conserva paquetes regulatorios versionados.

## Verificación futura obligatoria

Cada incremento ejecutará unitarias, propiedades monetarias, ArchUnit,
PostgreSQL/Testcontainers, JPA/JTA, concurrencia, idempotencia, seguridad negativa,
OIDC, Docker/Compose, health y Playwright responsive. Los plugins financieros
probarán redondeo, tasas, calendarios, reversos, cierres, reapertura, repetición,
fallas parciales, conciliación y conservación después de recrear contenedores.

La regulación se validará con fuentes oficiales descargadas dentro de `.tools/`,
versiones y checksums registrados, casos preparados por especialistas autorizados
y doble cálculo independiente. Un resultado técnico verde no constituye por sí
solo certificación legal o autorización para captar ahorros.

No corresponde ejecutar Maven, Docker ni Playwright al aceptar este ADR porque el
cambio es exclusivamente documental. El gate actual es G0 de enlaces, UTF-8 y
coherencia del roadmap.

## Referencias internas

- [ADR-0002 — Arquitectura de plugins](0002-arquitectura-plugins.md)
- [ADR-0011 — Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0013 — Eventos e idempotencia](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0016 — Autorización y auditoría](0016-autorizacion-y-auditoria-operaciones-plugin.md)
- [ADR-0028 — Gobierno de selectores](0028-gobierno-de-selectores-y-datos-administrables.md)
- [Épica de la familia cooperativa](../backlog/epica-cooperativa-ahorro-credito-paraguay.md)
- [Evidencia del análisis](../evidence/analisis-cooperativa-ahorro-credito-paraguay.md)
- [COOP-00 — Gobierno, alcance y matriz normativa](../backlog/COOP-00-gobierno-alcance-matriz-normativa.md)
- [Límites y dependencias de la familia](../architecture/cooperative-savings-credit-boundaries.md)
- [Alcance regulatorio inicial](../knowledge-base/cooperative-savings-credit/regulatory-scope-analysis.md)

## Fuentes oficiales consultadas

- [Ley N.º 438/1994 — De Cooperativas, BACN](https://www.bacn.gov.py/leyes-paraguayas/2373/ley-n-438-cooperativas-br)
- [Ley N.º 5501/2015 — Modificatoria de la Ley 438/94, BACN](https://www.bacn.gov.py/leyes-paraguayas/4482/modi)
- [Ley N.º 2.157/2003 — Carta Orgánica del INCOOP, BACN](https://www.bacn.gov.py/leyes-paraguayas/2423/regula-el-funcionamiento-del-instituto-nacional-de-cooperativismo-y-establece-su-carta-organica)
- [Resolución INCOOP N.º 22.668/2020 — Marco regulatorio de ahorro y crédito](https://www.incoop.gov.py/?p=9655)
- [Resolución INCOOP N.º 12.147/2014 — Plan de cuentas cooperativo](https://www.incoop.gov.py/?p=4330)
- [Resolución SEPRELAD N.º 156/2020 — Reglamento LA/FT para cooperativas](https://www.seprelad.gov.py/resoluciones/resoluciones/resolucion-n-156-2020-reglamento-de-prevencion-de-la-y-ft-para-cooperativas-sujetas-a-la-incoop.pdf)
- [Resolución INCOOP N.º 154/2026 — Modificación de la matriz de riesgo LA/FT](https://www.incoop.gov.py/?p=14383)
- [Comunicado INCOOP N.º 25/2026 — Índice de morosidad](https://www.incoop.gov.py/?p=14665)
