# Sprint 8 - Inventario `inventory`

- Estado: Reabierto; J11-S8-C04 implementa y valida localmente el gobierno Git por Sprint, con `sprint/08-cierre` publicada y el PR/protecciones remotas pendientes; J11-S8-C03 eleva los metadatos neutrales a 91/91, incorpora `reference_data` con el subconjunto normativo trazable `PY/PYG/USD` y conecta país/moneda a socios y catálogo; J11-S8-C02 mantiene retorno contextual seguro para selectores de plugins y los 11 usos nativos administrables, alta/consulta y ciclo activo/inactivo de
  unidades, categorías, marcas, etiquetas, perfiles tributarios, tipos de canal y familias de variantes, revisión
  explícita e historial visible de contenido/vigencia tributaria, además de altas de familias de
  variantes, revisión/historial append-only y reemplazo seguro de definiciones simples, revisión de nombre con historial visible append-only de tipos de canal, revisión estructural/historial append-only de familias y asignación versionada de familias a artículos;
  definiciones gobernadas de identificación/dirección; publicación completa, paginación, recongelación, PDF, instalador y G7 pendientes; Docker/Playwright de C03 verdes
- Fecha de planificación: 2026-07-31
- Dependencia técnica: G0-G6 de Sprint 7 verdes
- Pendiente transversal: validación independiente G7 de la guía candidata
- ADR rector: [ADR-0011](../../adr/0011-roadmap-dependencias-plugins-productivos.md)

## Objetivo

Construir `inventory` como tercer plugin productivo y primera capacidad operativa.
Debe administrar depósitos, ubicaciones, existencias, movimientos y reservas por
empresa, usando identificadores y contratos públicos de `commercial_catalog` sin
leer sus tablas ni importar sus entidades.

## Orden propuesto

| Orden | Historia | Resultado esperado |
|---:|---|---|
| 1 | [J11-S8-00](J11-S8-00-gobierno-planificacion.md) | alcance, decisiones, riesgos y gates |
| 2 | [J11-S8-01](J11-S8-01-caracterizacion-inventory.md) | caracterización y decisiones IN-D01 a IN-D10 confirmadas |
| 3 | [J11-S8-02](J11-S8-02-dominio-contratos-inventory.md) | dominio y contratos públicos versionados |
| 4 | [J11-S8-03](J11-S8-03-persistencia-inventory.md) | esquema privado, migraciones y repositorios |
| 5 | [J11-S8-04](J11-S8-04-aplicacion-seguridad-inventory.md) | aplicación, permisos, auditoría y concurrencia |
| 6 | [J11-S8-05](J11-S8-05-interfaz-inventory.md) | directorios y tareas JSF Material Design 3 responsive |
| 7 | [J11-S8-06](J11-S8-06-integracion-composicion-inventory.md) | composición física, integración con catálogo y demo candidata |
| 8 | [J11-S8-07](J11-S8-07-validacion-demo-cierre.md) | validación integral, demo oficial, retrospectiva y PDF; baseline congelado |
| 9 | [J11-S8-08](J11-S8-08-instalador-windows-cierre.md) | instalador Windows, preflight, montaje, pruebas y cierre formal |

Correcciones posteriores al congelamiento:

| Historia | Resultado esperado |
|---|---|
| [J11-S8-C01](J11-S8-C01-administracion-perfiles-tributarios.md) | administración visual autorizada de perfiles, recongelación y repetición de gates afectados |
| [J11-S8-C02](J11-S8-C02-gobierno-selectores-administrables.md) | fuentes gobernadas y administración visible de catálogos antes de recongelar |
| [J11-S8-C03](J11-S8-C03-datos-referencia-normativos.md) | países y monedas normativos compartidos, trazables y revalidados antes de `purchasing` |
| [J11-S8-C04](J11-S8-C04-gobierno-git-ramas.md) | adoptar ramas por Sprint, protección de `main`, PR por historia y detener el crecimiento de temporales versionados |

## Plan de ramas para el cierre

La estrategia Git aprobada el 2026-08-05 usa `main` como baseline aceptado, una
única rama `sprint/*` activa y ramas cortas `story/*`, `fix/*` o `chore/*`. Como el
primer commit Git importó el estado abierto de Sprint 8, se registra una excepción
transitoria: `main` permanece congelada en `166c5e1` y el cierre continúa en
`sprint/08-cierre`. No se creará `sprint/09-purchasing` hasta completar todos los
gates, integrar el cierre en `main` y crear el tag anotado `sprint-08`.

La adopción, las protecciones, los checks y la higiene no destructiva del índice
se detallan en [J11-S8-C04](J11-S8-C04-gobierno-git-ramas.md). El responsable de
producto autorizó el 2026-08-05 ejecutar la adopción. La autorización no permite
fusionar el cierre del Sprint en `main` antes de completar todos sus gates.

## Límites iniciales

- no incorporar compras, ventas, logística, documentos ni contabilidad;
- no mover stock desde `commercial_catalog` ni crear relaciones JPA entre plugins;
- no aceptar unidades o conceptos inexistentes mediante SQL directo;
- no definir valoración contable como efecto secundario del movimiento;
- no prometer stock global consistente entre empresas;
- no diseñar persistencia antes de confirmar dimensiones, reservas, negativos,
  unidades, concurrencia e idempotencia;
- no agregar outbox hasta existir un productor y consumidor reales aprobados.

## Demo visual objetivo

Con datos ficticios, la demo debe mostrar depósitos/ubicaciones, consulta de
existencias, entrada y salida trazables, reserva/liberación, denegación por permiso
o plugin inactivo y conservación al recrear la aplicación. Debe ejecutarse en 375,
720 y 1280 px sin simular compras, venta ni factura.

## Instalador Windows de cierre

Después de completar J11-S8-07 y congelar el baseline, J11-S8-08 creará el primer
instalador Windows conforme a la
[metodología transversal](../../runbooks/metodologia-instalador-windows-cierre-sprint.md).
Debe diagnosticar antes de cambiar la máquina, pedir consentimiento/UAC, montar el
proyecto, verificar health y conservar datos. La edición interna
`0.8.0-internal.1` recorrió esas fases y conservó datos, pero quedó obsoleta al
aprobar J11-S8-C01. Sprint 8 no se cierra hasta completar la corrección,
resolver el gate de selectores acordado, recongelar y preguntar a producto si se
creará un instalador nuevo. Con respuesta `SÍ` se ejecutarán la matriz Windows
independiente y los gates de entrega aplicables; con `NO`, `current` permanecerá
intacto y no representará el baseline nuevo. G7 continúa pendiente.

## Estado del incremento

El responsable de producto confirmó sin cambios
[IN-D01 a IN-D10](../../knowledge-base/inventory/legacy-characterization.md#decisiones-para-confirmación-de-producto)
el 2026-07-31. `J11-S8-02` materializó y validó los módulos `inventory-api` e
`inventory`, el contrato `1.0.0`, la dependencia requerida del catálogo y el
dominio neutral. `J11-S8-03` agregó ADR-0024, esquema privado V1 con nueve tablas,
unidad JPA y seis repositorios empresariales validados en PostgreSQL. `J11-S8-04`
agregó V2, diez entidades, siete repositorios, siete permisos, casos de uso
auditados, contratos CDI y frontera JTA; su reactor completo quedó verde.
`J11-S8-05` agregó tres menús y contratos de pantalla, directorios empresariales,
handlers autorizados y su presentación mediante el renderer JSF Material Design 3
responsive del shell. `J11-S8-06` incorporó el perfil físico único
`with-inventory-demo` a WAR/migrador, migraciones V1–V2 idempotentes, imágenes,
activación/permisos reales y una demo Playwright de movimiento, reserva,
disponibilidad, conteo y seguridad negativa en siete anchos. Se habilita
`J11-S8-07` completó los gates técnicos G0-G6, congeló las imágenes finales,
ejecutó la demo oficial de tres plugins productivos, creó la
[fotografía de dependencias](estructura-plugins-y-dependencias.md), la
[retrospectiva](retrospective.md) y el PDF obligatorio. `J11-S8-08` implementó el
instalador nativo, preflight, plan/consentimiento, ejecución, reparación, artefacto
`current` y demo visual. La instalación interna real terminó con migración y health
verdes, y dos reparaciones conservaron secretos, volúmenes y conteos. El hallazgo
visual J11-S8-C01 reabrió ese baseline. La corrección agregó el octavo menú
fusionado, administración autorizada de perfiles tributarios y demo Playwright en
375/720/1280 px; reactor, arquitectura, composición, health y seguridad negativa
quedaron verdes. Faltan resolver el gate de selectores, recongelar la fotografía y
el PDF, preguntar si se creará un instalador y completar G7. Si la respuesta es
`SÍ`, se suman VM Windows limpia e incompatible, escenarios reales de
UAC/cancelación, Authenticode y la matriz acordada; con `NO`, esos escenarios no
son gate del Sprint y `current` queda intacto. No se autoriza iniciar el siguiente
plugin ni declarar cerrado el Sprint hasta resolver los gates aplicables.

ADR-0027 agregó históricamente `point_of_sale` como décimo plugin futuro, después
de `treasury`. ADR-0033 insertó luego `recurring_billing` como orden 8 y desplazó
POS al 11. ADR-0034 agregó después `vehicle_telemetry` como orden 7, amplió el
roadmap a diecinueve y desplazó POS al 12. Ninguna decisión autoriza adelantar su
implementación: Sprint 8 y los plugins 4 a 11 deben completarse primero.
ADR-0035 agregó el 2026-08-04 la venta offline como requisito de la primera
versión productiva de POS y planificó `POS-OFF-00` a `POS-OFF-06`; no cambia el
orden ni autoriza iniciar código durante Sprint 8.
ADR-0036 agregó el 2026-08-04 la familia futura de operaciones del proveedor:
`customer_support`, `release_management` y el técnico opcional
`support_connector`. El catálogo global planificado pasa a veintidós
reutilizables, pero la secuencia ERP permanece en diecinueve y no cambia el
trabajo autorizado de Sprint 8. Los plugins centrales se componen en la instancia
del proveedor y el conector, si se contrata, en el ERP del cliente con HTTPS sólo
saliente, consentimiento y sin ejecución remota.
ADR-0037 agregó el 2026-08-04 una familia vertical para cooperativas de ahorro y
crédito de Paraguay: `cooperative_membership`, `cooperative_governance`,
`aml_compliance`, `cooperative_savings`, `cooperative_credit` y
`cooperative_regulatory_paraguay`. El catálogo global planificado pasó a
veintiocho en ese corte histórico, sin renumerar ERP 1–19 ni iniciar código. El perfil futuro reutiliza
participantes, tesorería y contabilidad; préstamos no se guardan como cuentas por
cobrar comerciales y ahorros no se guardan como caja.
COOP-00 quedó refinada después como historia futura con COOP-D01–D15, registro
normativo, grafo, flujos monetarios y gates G0–G5. Sigue sin datos de una
cooperativa concreta, prioridad de Sprint o autorización de implementación.
Estas ampliaciones son documentales. El G0 final posterior al refinamiento validó 287
Markdown sin enlaces rotos, errores UTF-8, mojibake ni filtraciones de secretos.
No se ejecutaron Maven, Docker ni Playwright porque no cambiaron código, POM,
migraciones, composición o UI vigente.

ADR-0028 auditó todos los selectores actuales: 18 nativos del shell/kernel y 51
declarados inicialmente por los tres plugins funcionales. J11-S8-C02 agregó
`plugin-api` 0.4.2, renderer contextual autorizado y la pantalla
`/catalog/definitions`. Con los dos selectores de revisión agregados en el
decimosexto corte, el inventario alcanzó 18 nativos y 61 de plugins. Los dos
selectores del reemplazo seguro agregados en el decimoséptimo corte elevan el
inventario a 18 nativos y 63 de plugins. Los dos selectores del editor estructural
de familias agregados en el decimoctavo corte elevaron el inventario a 18 nativos
y 65 de plugins. El selector de familia para artículos agregado en el decimonoveno
corte eleva el inventario a 18 nativos y 66 de plugins. El vigésimo agrega cinco
usos en socios —tres consumidores y dos selectores de clase—: el inventario actual
queda en 18 nativos y 71 de plugins; las 89 fuentes quedaron declaradas, incluidas
unidades, categorías, marcas, etiquetas, familias de variantes y las cuatro clases
de definiciones de socios.
El sexto corte agregó inactivación/reactivación versionada y auditada para las
cuatro definiciones simples, con PostgreSQL, reactor, imagen, health y Playwright
focal verdes. El séptimo corte agregó el ciclo versionado y auditado de perfiles
tributarios; PostgreSQL, reactor, imagen, health y Playwright también quedaron
verdes. El octavo corte agregó la revisión explícita de tratamiento, descripción y
vigencia sin cambiar identidad; aplicación, renderer y PostgreSQL quedaron verdes,
y `verify`, imagen, health y Playwright responsive también quedaron verdes. El
noveno corte agregó la consulta visual, autorizada y responsive del historial de
revisiones tributarias, aislada por empresa; PostgreSQL, reactor, imagen, health y
Playwright quedaron verdes. El decimosexto corte agregó revisión e historial
append-only de definiciones simples mediante V2 privada, manteniendo código e
identidad. Sus 71 pruebas de módulo, 17 pruebas PostgreSQL, `verify` 24/24,
imágenes de aplicación y migrador, migración idempotente, health y Playwright
responsive quedaron verdes. Los digests candidatos son
`sha256:65ac722b741008c64aff97085b91932ec05868ef7c215229c822a9024727827a`
para la aplicación y
`sha256:eb85e9b3a368c1283ee218191a9c5b3440a0675b1b12d765e1ee5410dc524058`
para el migrador.

El decimoséptimo corte implementó el reemplazo seguro de las cuatro definiciones
simples mediante V3: identidad sucesora nueva, origen inactivo e inmutable,
vínculo privado del mismo tipo/empresa y referencias existentes conservadas. Sus
digests candidatos son
`sha256:a21616f9bf182ff99f1aabdcf806a7426334216699e48d116d724161555de987`
para la aplicación y
`sha256:1062a6fbc34160dd17fa0f29f03780396e080bec23105c725944de90235fe9e8`
para el migrador. Aplicación, PostgreSQL 19/19, migración real e idempotente,
imágenes, health y Playwright responsive quedaron verdes. Antes de iniciar
`purchasing` debían resolverse países/monedas, sus fuentes normativas y la
estrategia de listas grandes. J11-S8-C03 resolvió propiedad, procedencia y
revalidación del subconjunto inicial; la publicación completa y la estrategia de
listas grandes continúan pendientes. El décimo corte migró los 18 selectores nativos a
un propietario de plataforma explícito, mostró origen/clase mediante un componente
Faces común y filtró enlaces con autoridad global; sus 342 pruebas de regresión y
el gate integral de 24/24 módulos quedaron verdes. El undécimo corte agregó
retorno de un uso para selectores de plugins, ligado a sesión/usuario/empresa, sin
valores de negocio en la URL; restauró el borrador seguro y refrescó opciones. Su
regresión, gate integral, imagen verificada, health y Playwright en 1280/720/375 px
quedaron verdes. El duodécimo corte extendió el mismo retorno a los 11 usos nativos
administrables mediante whitelist, POST, UUID opaco, contexto de sesión acotado,
reautorización y restauración específica por formulario. Su build `verified` de
24/24 módulos, 24 pruebas de arquitectura, health, logs, Playwright 1/1 y seis
capturas nativas en 1280/720/375 px quedaron verdes. El decimotercer corte agregó
inactivación/reactivación versionada y auditada para tipos de canal; PostgreSQL
12/12, módulo 43/43, `verify` 24/24, imagen, health, Playwright y capturas
responsive quedaron verdes. El decimocuarto corte agregó el mismo ciclo
versionado, aislado por empresa y auditado para familias de variantes, conservando
atributos e identidad; servicio 8/8, PostgreSQL 11/11, módulo 67/67, `verify`
24/24, imagen, health y Playwright responsive quedaron verdes. El decimoquinto
corte agregó revisión de nombre e historial visible append-only de tipos de canal
mediante V3 privada; módulo 46/46, PostgreSQL 19/19, `verify` 24/24, imágenes,
migración idempotente, health y Playwright responsive quedaron verdes. El
decimoctavo corte agregó revisión completa del nombre y de 1 a 8 atributos de una
familia, historial append-only y versionado de asignaciones mediante V4 privada.
El módulo quedó 77/77, PostgreSQL 22/22, `verify` 24/24, migración V3→V4 e
idempotencia, imágenes, health y Playwright responsive verdes. El decimonoveno
corte valida la asignación neutral y versionada de familias a artículos: módulo
81/81, PostgreSQL 23/23, reactor 24/24, imagen final, health y Playwright 1/1 en
1280/720/375 px verdes. El vigésimo corte amplía `business_partners` a V4 y
administra identificación, tipo y propósito de dirección junto con canales;
módulo 51/51, PostgreSQL 21/21, reactor 24/24, migración real/idempotente, health y
`BusinessPartnersVisualIT` 1/1 en 1280/720/375 px quedaron verdes. J11-S8-C03
agrega dos selectores normativos —país y moneda—, por lo que el inventario vigente
pasa a 18 nativos y 73 de plugins, 91/91 con fuente y propietario.
`reference_data` es la fundación R0 número veintinueve del catálogo global y no
renumera ERP 1–19. Docker/Compose, health/OIDC y Playwright responsive de C03
quedaron verdes. Faltan publicación completa, listas grandes y los gates formales
de recongelación/cierre. Sprint 8
continúa abierto y no se autoriza iniciar `purchasing`.
G0 del decimosexto corte validó 280 Markdown sin enlaces rotos, errores UTF-8,
mojibake ni filtraciones de secretos. G0 del decimonoveno corte repitió el control
sobre 287 Markdown y 1.330 enlaces locales, también sin hallazgos. G0 del vigésimo
corte validó 288 Markdown, 1.336 enlaces y 1.180 archivos de texto sin errores
UTF-8, mojibake, enlaces rotos ni filtraciones de los cuatro secretos locales.

La ampliación del plan y el gobierno posterior se validaron documentalmente el
2026-08-01 sobre 252 archivos Markdown y 993 enlaces locales: cero errores UTF-8 y
cero enlaces rotos. No se ejecutó Maven, Docker ni Playwright porque estas
decisiones no modifican código, descriptores, migraciones, composición ni interfaz
ejecutable.
