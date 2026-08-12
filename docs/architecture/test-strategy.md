# Estrategia y matriz de pruebas

- Versión: 24
- Fecha: 2026-08-12
- Estado: Aceptada
- Historia: `J11-S1-01`, Sprint 2, `J11-S3-00` a `J11-S3-08`, `J11-S4-08`, `J11-S5-01` a `J11-S5-04`, `J11-S6-02`/`J11-S6-03`, `J11-S8-02` a `J11-S8-06` y `J11-S9-06`; [ADR-0007](../adr/0007-material-design-responsive-sobre-jsf.md), [ADR-0008](../adr/0008-logout-oidc-estabilidad-preview-wildfly.md), [ADR-0028](../adr/0028-gobierno-de-selectores-y-datos-administrables.md) y [ADR-0029](../adr/0029-confirmacion-instalador-por-cierre-sprint.md); reglas transversales de demo visual, PDF, selectores y decisión de instalador al cerrar cada Sprint

## Regla principal

Cada cambio coherente se prueba inmediatamente con el gate más pequeño que pueda demostrarlo. Si falla una prueba relevante, el trabajo se detiene, se documenta el fallo y se corrige antes de iniciar el siguiente cambio.

### Excepción temporal de Sprint 3

El responsable de producto decidió el 2026-07-28 terminar primero la candidata visual y ejecutar después la validación automatizada acumulada. Esta excepción aplica únicamente a `J11-S3-01` a `J11-S3-07`:

- permite diferir la ejecución, no eliminar pruebas ni criterios;
- las historias usaron `Implementada pendiente de validación` y no satisfacían Definition of Done antes del gate;
- cualquier fallo observado continúa deteniendo el alcance afectado;
- `J11-S3-08` ejecuta los gates completos antes de aceptar la demo o cerrar el Sprint;
- no se promueve ni despliega a producción una candidata pendiente.

La excepción ya agotó su propósito: `J11-S3-08` dejó G2–G6 verdes. Sigue vigente el
flujo normal para todo cambio posterior y G7 permanece obligatorio antes del cierre.

Los comandos Maven indicados aquí existen desde `J11-S1-02`. Requieren Java 21 y siempre deben ejecutarse mediante Maven Wrapper.

## Niveles de gate

| Gate | Propósito | Resultado requerido |
|---|---|---|
| G0 — Documentación | Estructura, metadatos, enlaces y coherencia | Cero enlaces locales rotos y decisiones trazables |
| G1 — Módulo | Feedback inmediato del cambio | Pruebas del módulo y dependencias necesarias verdes |
| G2 — Repositorio | Compatibilidad del conjunto | `mvn verify` verde |
| G3 — Distribución | Composición y límites | WAR con plugin de referencia presente y ausente |
| G4 — Contenedores | Infraestructura ejecutable | Build, migración, arranque, health y persistencia verdes |
| G5 — Sistema | Comportamiento visible | Smoke, seguridad negativa y E2E verdes |
| G6 — Cierre de Sprint | Entregable técnico consultable | PDF de estructura regenerado, renderizado, revisado y registrado por SHA-256 |
| GI — Instalador Windows | Montaje reproducible del baseline final cuando producto responde `SÍ` | preflight, instalación/actualización, health y persistencia verdes; artefacto `current` único |

## Matriz por tipo de cambio

| Cambio | Prueba inmediata | Gate de cierre |
|---|---|---|
| Solo documentación | Validar UTF-8, estructura, metadatos y enlaces locales | G0 |
| POM, BOM o Wrapper | Versión efectiva, modelo Maven y módulo afectado | G2 y build limpio con Wrapper |
| Dominio Java puro | JUnit 5 del módulo | G1 + G2 + ArchUnit |
| `plugin-api` o `kernel-api` | Unitarias de invariantes y superficie de dependencias | G1 + G2 + compatibilidad de API |
| Grafo o descriptor de plugins | Duplicados, ausencias, ciclos, rangos y orden determinista | G1 + G3 |
| Adaptador CDI/Jakarta | Integración en WildFly y caso negativo | G2 + G3 + G4 |
| Plugin nuevo | Unitarias, arquitectura y contratos | G2 + build presente/ausente + G4 |
| Activación por empresa | Casos activo, inactivo, dependencia y empresa equivocada | G2 + G5, incluidos casos de autorización negativa |
| Contexto empresarial | Empresa explícita, fuente confiable y ausencia de estado global | G2 + G5 con aislamiento negativo |
| Personalización obligatoria | Alta, unicidad, ausencia, categoría, reemplazo y cuarentena | G2 + G4 + G5 |
| Contrato de pantalla | Overlay permitido/prohibido, versión, orden final y atomicidad | G2 + G3 + ArchUnit |
| JPA o repositorio | Integración con PostgreSQL real mediante Testcontainers | G2 + validación de esquema |
| Migración | Aplicar en vacío, reaplicar, detectar checksum y actualizar desde versión anterior | G4 |
| Dockerfile o imagen base | Build limpio y análisis de configuración | G4, sin secretos y por digest |
| Compose o variables | `docker compose config` y matriz de variables | G4 |
| Endpoint REST | Integración HTTP con REST Assured | G4 + autenticación/autorización negativa |
| UI o navegación | Prueba de componente cuando aplique y revisión en compacto/medio/expandido | G5 con Playwright, teclado y overflow |
| Selector o catálogo administrable | fuente/propietario, alta o administración, retorno, inactivo, vacío, paginación y permiso negativo | G1 + G2 + PostgreSQL/JTA + G5 |
| Seguridad | Casos permitidos y denegados en servidor | G2 + G4 + G5 |
| Identidad OIDC | Identidad externa, issuer/audience, sesión, login y logout | G2 + runtime WildFly/Keycloak + G5 |
| Membresía y roles | Cero/una/múltiples empresas, revocación y concurrencia | G2 + PostgreSQL/JTA + G5 |
| Instalador Windows con respuesta `SÍ` | manifiesto/preflight y prueba mínima del componente | GI en VM limpia, incompatible y con instalación previa |

## Comandos estándar previstos

Windows:

```powershell
.\mvnw.cmd -pl <modulo> -am test
.\mvnw.cmd verify
docker compose config
docker compose build --pull
docker compose up --wait
```

POSIX:

```bash
./mvnw -pl <modulo> -am test
./mvnw verify
docker compose config
docker compose build --pull
docker compose up --wait
```

Los demás comandos exactos y perfiles se agregan al materializar cada gate. No se documentan como exitosos hasta haberlos ejecutado.

El gate PostgreSQL real implementado desde `J11-S2-03` es:

```powershell
.\mvnw.cmd -B -Ppostgres-integration `
  "-Dlogixone.postgres.integration=true" -pl migrator -am verify
```

El opt-in evita exigir Docker dentro del stage builder de las imágenes; el gate se ejecuta en el host o agente CI con Docker disponible.

El gate completo de repositorios desde `J11-S2-04` es:

```powershell
.\mvnw.cmd -B "-Dlogixone.postgres.integration=true" clean verify
```

El arnés JTA de runtime se construye solo con `-Pjta-runtime-harness`. Su prueba se activa adicionalmente con `-Dlogixone.jta-probe=true` contra una composición efímera; el arnés no puede aparecer en el WAR ni en la imagen normales.

## Pruebas arquitectónicas mínimas

ArchUnit debe rechazar como mínimo:

- cualquier `javax..`;
- Jakarta, JDBC (`java.sql`/`javax.sql`), PostgreSQL, Hibernate, WildFly, JSF o PrimeFaces dentro de `plugin-api`, `kernel-api`, dominios y APIs empresariales;
- dependencias del kernel hacia implementaciones de plugins;
- dependencias entre implementaciones de plugins;
- entidades o repositorios de un plugin usados por otro;
- lógica empresarial dentro de `distribution`;
- acceso de UI directamente a repositorios.
- dependencias desde un plugin funcional hacia una personalización;
- dependencias entre plugins de personalización;
- personalizaciones que importen implementaciones, entidades, repositorios o adaptadores UI internos de otro plugin.

Maven Enforcer debe comprobar Java, Maven, convergencia de dependencias y versiones no declaradas cuando se cree el build.

## Pruebas de composición

La distribución se verifica en tres variantes obligatorias, siempre como pareja
WAR + migrador construida desde `logixone-plugin-set`:

1. kernel y shell con `reference-plugin` presente;
2. kernel y shell sin `reference-plugin`.
3. kernel y shell con `reference-plugin` y las personalizaciones A/B presentes.

Ambas deben compilar y arrancar. La variante ausente no puede conservar clases, menú, migraciones o comportamiento del plugin por dependencias transitivas accidentales.

Sprint 2 agrega estas composiciones:

1. plataforma sin empresas y sin plugins, que continúa siendo un bootstrap válido;
2. dos empresas con el mismo plugin funcional y dos personalizaciones distintas;
3. personalización asignada ausente o incompatible, que deja solo a su empresa no disponible sin degradar readiness global;
4. descriptor físico inválido, que impide publicar el catálogo y deja readiness global en `DOWN`.

Cada variante verificará contenido exacto del WAR, proveedores SPI del migrador y
ausencia de Jakarta dentro del ejecutable one-shot. Los plugins de personalización
no asignados son válidos físicamente, pero no aportan contribuciones empresariales.

Desde `J11-S5-01`, PostgreSQL debe verificar además `core` primero, esquemas
`plg_*` en orden topológico, historial independiente, idempotencia, checksum y
conservación al retirar o desactivar un plugin.

`J11-S5-01` cerró ese gate con 12 escenarios PostgreSQL/Testcontainers sin fallos.
Las imágenes de aplicación y migrador se construyeron con la misma composición
A/B, Compose alcanzó health real y una reejecución aplicó cero migraciones en
`core` V5 y `plg_reference_plugin` V1. Después de recrear PostgreSQL, el marcador
técnico persistió. Una pareja base con cero plugins arrancó sobre el mismo volumen,
migró únicamente `core` y conservó esquema, historial y datos del plugin retirado.

`J11-S5-02` agregó 9 pruebas del generador y elevó el reactor base a 191 pruebas
verdes. Las 13 reglas arquitectónicas comprueban además que la herramienta use
sólo Java y contratos públicos. Un reactor temporal compiló el módulo generado y
demostró una composición simétrica: un único JAR en el WAR y un único proveedor en
el migrador. La variante base y la variante A/B completaron 18 módulos cada una;
Docker/Compose confirmó readiness y conservación del volumen anterior.

`J11-S5-03` fue una decisión documental: no agregó un evento ficticio, tabla,
dispatcher ni transporte porque aún no existe productor/consumidor empresarial.
ADR-0013 fija la futura matriz obligatoria: commit/rollback de estado + outbox,
entrega duplicada, inbox idempotente, concurrencia, reinicio, orden por sujeto,
versión incompatible, activación, bootstrap, reintentos, cuarentena y replay. Esos
gates se ejecutan con el primer intercambio real; no se declaran probados ahora.

`J11-S6-02` elevó el reactor base a 20 módulos y 212 pruebas verdes. Las 5 pruebas
de `business-partners-api`, 15 del plugin y una nueva regla ArchUnit verifican
identidad, contrato mínimo, roles, ciclo de vida, detalles, versión y ausencia de
frameworks. El módulo arquitectónico totaliza 14 pruebas. La inspección confirmó
API sin Jakarta/internos, un único CDI/SPI, cero migraciones y cero JAR del plugin
en el WAR base. PostgreSQL, runtime y UI no aplican hasta J11-S6-03/05/06.

`J11-S2-02` ejecutó la primera matriz neutral: 12 pruebas de `plugin-api`, 3 de `kernel-api`, 23 de dominio y 15 de aplicación. El gate integral totalizó 83 pruebas, incluidas 5 reglas ArchUnit, y verificó los WAR con cero/un `reference-plugin` y una sola copia de cada API compartida.

`J11-S2-03` agregó 2 pruebas unitarias al baseline y 7 escenarios PostgreSQL bajo el perfil explícito. El build limpio totalizó 85 pruebas; Compose agregó validaciones REST sobre actualización V1→V2, base vacía y rollback de aplicación.

`J11-S2-04` elevó el baseline normal a 86 pruebas, incluida una sexta regla ArchUnit. El gate limpio con PostgreSQL totalizó 99 pruebas: 7 escenarios de migración y 6 de repositorios JPA. WildFly agregó 4 pruebas runtime sin omitidas, dos REST y dos de commit/rollback JTA.

`J11-S2-05` elevó el baseline normal a 97 pruebas. El gate limpio con PostgreSQL totalizó 111: 7 escenarios de migración y 7 de persistencia/casos de uso, además de las 6 reglas ArchUnit. WildFly agregó 6 pruebas runtime sin omitidas: dos de salud y cuatro JTA que cubren commit/rollback, servicios de aplicación e independencia efectiva entre dos empresas.

`J11-S2-06` elevó el baseline normal a 104 pruebas: 24 de dominio, 30 de aplicación y 7 del módulo arquitectónico, incluidas 6 reglas ArchUnit. El gate limpio con PostgreSQL totalizó 118 al sumar 7 escenarios de migración y 7 de persistencia/aplicación. WildFly mantuvo 6 pruebas runtime sin omitidas y amplió el escenario de dos empresas para comprobar capacidades, permisos y menús aislados, además del orden funcional→personalización. Las imágenes base y con `reference-plugin` arrancaron por separado y pasaron salud real.

`J11-S2-07` elevó el baseline normal a 122 pruebas: 27 de dominio, 37 de aplicación, 16 de `plugin-api`, 4 de los plugins funcional/personalizaciones y 9 del módulo arquitectónico, incluidas 7 reglas ArchUnit. El gate limpio con PostgreSQL totalizó 136. WildFly mantuvo 6 pruebas runtime sin omitidas y comprobó dos resultados de pantalla distintos sobre el mismo contrato, además de commit/rollback. Los WAR de 0/1/3 plugins y las imágenes base/personalizaciones pasaron inspección y salud real.

Sprint 3 deberá agregar al cierre una matriz acumulada con Keycloak real, OIDC de WildFly, esquema `core` V3, bootstrap, cero/una/múltiples membresías, revocación, selección empresarial, autorización, shell y Playwright A/B. Ninguno de esos resultados se considera verde por haber sido planificado.

`J11-S3-01` incorporó 24 fuentes Java puras y empaquetó los 16 módulos con `-DskipTests`. La compilación y el ensamblado quedaron verdes y la inspección estática encontró 0 imports prohibidos y 0 entradas Keycloak en el WAR. Estos controles no sustituyen JUnit/ArchUnit: la historia permanece implementada pendiente de la matriz acumulada.

`J11-S3-02` agregó el recurso Flyway V3 con cinco tablas, cinco índices y FKs compuestas para aislamiento empresarial. V1/V2 conservaron sus hashes y el migrador empaquetó los tres recursos con pruebas omitidas. La aplicación real sobre PostgreSQL, reejecución, checksum Flyway y casos negativos permanecen deliberadamente pendientes de G3.

`J11-S3-03` agregó los adaptadores JPA/JTA, resultados tipados, consultas de estado actual y bootstrap interno. El reactor empaquetó 16 módulos con `-DskipTests`; la inspección encontró 0 imports Jakarta/Javax/Keycloak en API, dominio y aplicación. Permanecen pendientes en G2/G3 las pruebas del nuevo modelo de presentación, repositorios V3, idempotencia, concurrencia, cruces empresariales, permisos desconocidos, bootstrap idéntico/incompatible y rollback por auditoría. Las aserciones históricas que todavía esperan readiness V2 o dos migraciones deberán actualizarse antes de ejecutar el gate acumulado.

`J11-S3-04` fijó Keycloak 26.7.0 por digest ejecutable, agregó realm/cliente declarativos, volumen y red de identidad, configuró OIDC nativo de WildFly, separó restricciones web/REST y conectó el bootstrap externo posterior a migraciones. Nueve módulos relevantes empaquetaron con JDK 21 y `-DskipTests`; Compose, JSON y XML pasaron validación estática. Docker Desktop detenido impidió ejecutar la CLI y el runtime, por lo que login/logout, tokens negativos, health ante caída del IdP, secretos, bootstrap y recreación permanecen pendientes en G4/G5.

`J11-S3-05` agregó adaptación exclusiva del principal OIDC validado, referencia mínima
de sesión, selección server-side y guarda por plugin/permiso con relectura de estado.
Dos empaquetados con JDK 21 y `-DskipTests` finalizaron correctamente; el WAR integrado
contiene los cuatro módulos del flujo y `web.xml`. La matriz acumulada debe cubrir
cero/una/múltiples membresías, manipulación de empresa, actor de sesión distinto,
revocaciones, plugin desactivado, permiso de otro propietario, cambio/logout,
`401`/`403` genéricos y contenido seguro de auditoría. Ninguno de esos escenarios se
considera verde hasta `J11-S3-08`.

`J11-S3-06` agregó una proyección de navegación request-scoped, shell Faces, selector,
logout, menú filtrado y guarda de ruta directa. Empaquetaron las variantes base y
funcional+A/B con `-DskipTests`; tres documentos XML fueron parseados y se inspeccionó
el contenido de JAR/WAR. Playwright deberá cubrir login, cero/una/varias empresas,
cambio de contexto, menú vacío/permitido, manipulación de ruta y selector, logout,
teclado y los rangos responsive de
[ADR-0007](../adr/0007-material-design-responsive-sobre-jsf.md). Los viewports
representativos serán `375px`, `720px` y `1280px`, con casos de borde en `599px`,
`600px`, `839px` y `840px`; se comprobarán foco, estados, reduced motion y ausencia
de overflow horizontal de página. El runtime y esos escenarios no se consideran
verdes.

`J11-S3-07` agregó tipo neutral de elemento, resolución confiable de pantalla,
auditoría `RESOLVE_SCREEN` y registro cerrado del renderer JSF. Empaquetaron doce
módulos con los plugins funcional+A/B y nueve sin plugins usando `-DskipTests`.
La matriz acumulada debe comprobar tipos/regiones/textos/fragmentos desconocidos,
composición inválida, pantalla ausente, propietario distinto, permiso revocado,
estado A requerido/reordenado, estado B oculto/deshabilitado, cambio A↔B sin estado
residual y los siete anchos definidos. Ninguno se considera verde todavía.

`J11-S3-08` ejecutó y corrigió la matriz acumulada. El gate final con el perfil de
plugins A/B y PostgreSQL produjo 145 pruebas, 0 fallos, 0 errores y 0 omitidas; 7
reglas ArchUnit quedaron verdes. El runtime agregó 2 pruebas de health, 4 JTA y 4
OIDC; Playwright agregó 3 escenarios de cero/una/múltiples membresías, manipulación
negativa, selección/cambio, A/B responsive y logout. Los Dockerfiles pasaron
`buildx --check`, el WAR host/imagen quedó idéntico por SHA-256 y la recreación
conservó PostgreSQL y Keycloak. Esta certificación completa G2–G6; G7 continúa
pendiente de validación independiente y PDF.

## Pruebas de persistencia

- PostgreSQL debe ser una instancia real en contenedor, no una base de datos en memoria que oculte diferencias SQL.
- Las migraciones se aplican antes de JPA.
- La segunda ejecución no cambia el esquema ni reaplica migraciones.
- Una migración ya aplicada que cambie debe fallar por checksum.
- La recreación del contenedor de aplicación conserva datos.
- La recreación controlada de PostgreSQL con el mismo volumen conserva datos.
- JPA valida, pero no crea ni actualiza tablas.
- toda empresa persistida tiene `customization_plugin_id` no nulo y único;
- dos empresas no pueden compartir personalización y una sustitución fallida conserva la asignación anterior;
- consultas y mutaciones con `CompanyId` equivocado no revelan ni alteran activaciones o personalización ajenas.

## Pruebas de personalización de pantalla

- el propietario declara pantalla, versión, elementos, slots y operaciones permitidas;
- la personalización solo referencia identificadores públicos y se compone después de plugins funcionales;
- dos empresas obtienen overlays distintos y deterministas;
- referencias ausentes, versiones incompatibles, conflictos y operaciones prohibidas rechazan el overlay completo;
- ocultar o deshabilitar un componente no cambia autorización ni validación en el servidor;
- el renderer JSF aplica tokens Material 3 del shell y no acepta CSS o JavaScript global desde plugins;
- cada resultado estándar y A/B se verifica en compacto, medio y expandido sin filtración ni overflow de página;
- Playwright se ejecutó en el gate acumulado sobre el adaptador navegable
  materializado por `J11-S3-07`; cada pantalla futura debe ampliar la matriz para sus
  interacciones y límites exactos.

## Pruebas de auditoría persistente y panel administrativo

- V1→V6, actualizaciones desde V1–V5 y una ejecución repetida se prueban sobre PostgreSQL real;
- `core.audit_event` rechaza `UPDATE`/`DELETE`, respeta checks e índices y no
  incorpora backfill de logs;
- los cinco puertos históricos y el contrato neutral de plugins conservan categoría, operación, resultado e IDs
  técnicos aplicables sin identidad externa o secretos;
- una mutación confirma o revierte junto con su auditoría;
- decisiones permitidas y denegadas de acceso se confirman en transacción corta;
- paginación, orden estable, ventanas y filtros exactos se prueban sin aceptar JPQL,
  columnas u orden desde el cliente;
- `/admin/audit.xhtml` exige `kernel.audit.view` y no expone edición, borrado,
  exportación, SQL, stacktrace ni datos comerciales;
- rutas administrativas permitidas y denegadas verifican `no-store`, `nosniff`,
  anti-frame, referencia, capacidades y CSP;
- Playwright recorre estado vacío, resultados, filtros y paginación a
  375/720/1280 px.

Esta matriz pertenece al gate acumulado `J11-S4-08`; la compilación de
`J11-S4-07` no la declara ejecutada.

El procedimiento operativo, los comandos, motivos, resultados esperados y reglas
de parada están consolidados en el
[manual paso a paso de pruebas J11-S4-08](../runbooks/manual-pruebas-j11-s4-08.md).

## Evidencia obligatoria

Cada historia registra:

- fecha y ambiente;
- versión de Java, Maven, Docker y Compose cuando aplique;
- comando o procedimiento exacto;
- código de salida y resumen del resultado;
- fallos y correcciones;
- artefactos o digests generados;
- pruebas no ejecutadas y motivo.

Cuando una historia cambie la experiencia de implementación para una empresa, la evidencia también debe indicar:

- versión o capítulo de la [guía para implementadores](../implementation-guide/README.md) revisado;
- recorrido o ejemplo actualizado;
- persona o perfil independiente que validó las instrucciones cuando sea un corte entregable;
- diferencias encontradas entre la guía y el sistema y cómo se resolvieron.

La guía no se acepta solo por revisión de sus autores: por decisión de producto, `J11-S3-08` ejecutará el recorrido con alguien que no haya implementado las capacidades explicadas antes de publicar la edición `1.0`.

### Evidencia adicional obligatoria al cerrar un Sprint

Antes del PDF final, cada cierre debe ejecutar una demo visual sobre el baseline
real. La evidencia debe identificar:

- imagen, artefacto o digest exacto utilizado;
- ambiente reproducible y datos ficticios preparados;
- guion bajo `docs/runbooks/`, ruta de entrada y resultados esperados;
- capacidades del Sprint realmente mostradas y límites que continúan pendientes;
- autorización positiva/negativa y estados vacío/error relevantes;
- recorrido y evidencia en `375px`, `720px` y `1280px`;
- ausencia de secretos, datos reales, mocks engañosos y overflow horizontal normal;
- procedimiento para limpiar o restaurar el estado de demostración.

El gate de demo falla si solo existen diapositivas, una grabación no reproducible,
capturas aisladas o una interfaz que simula capacidades no implementadas. Playwright
debe automatizar el recorrido cuando la UI sea estable, pero la evidencia de cierre
también debe confirmar que el guion puede ser presentado por una persona.

Cada cierre regenera `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` después del último cambio del baseline. La evidencia debe registrar:

- Sprint y fecha declarados dentro del documento;
- correspondencia del inventario, arquitectura, estado y pendientes con el repositorio final;
- cantidad de páginas y tamaño del archivo;
- checksum SHA-256;
- renderizado de todas las páginas y resultado de la revisión visual;
- comprobación de metadatos, texto extraíble, páginas vacías y caracteres dañados.

G6 falla si el PDF fue generado antes del último cambio relevante, si conserva información de un Sprint anterior o si solo se comprobó extracción de texto sin revisar el renderizado.

Después de completar G6, el cierre pregunta si se creará un instalador Windows y
registra `SÍ` o `NO`. Con `SÍ`, GI se ejecuta después de congelar el baseline y su
matriz debe verificar:

- diagnóstico de solo lectura y estados compatible/advertencia/bloqueada;
- ausencia de cambios y UAC cuando la máquina está bloqueada o el usuario cancela;
- lista previa completa, licencias, consentimiento y elevación mínima;
- descarga con versión y hash/firma aprobados, incluido fallo cerrado por hash;
- instalación limpia hasta migración, Compose, liveness, readiness y ruta visual;
- requisito compatible ya instalado y requisito incompatible;
- rechazo UAC, cancelación segura, reanudación o recuperación;
- actualización y reparación sin perder configuración, volúmenes o datos;
- recreación con `down` sin `--volumes` y persistencia comprobada;
- versión, baseline/digest, tamaño, SHA-256, firma y terceros registrados;
- reemplazo acotado: solo una edición en `current` y fuentes intactas.

GI falla si el instalador representa un baseline anterior, modifica antes del
consentimiento, oculta acciones, desactiva seguridad, pisa datos, no explica una
incompatibilidad o no fue ejecutado en los ambientes requeridos. Un instalador
interno no firmado debe estar marcado como tal y no puede distribuirse a empresas.

Con respuesta `NO`, GI no se ejecuta: se comprueba que `current` no cambió, se
registra su baseline anterior y se prohíbe presentarlo como instalador del Sprint
nuevo. Sin respuesta registrada, el cierre permanece pendiente.

La evidencia debe ser reproducible y no contener secretos. Un mensaje “funciona” sin comando, resultado y alcance no satisface la Definition of Done.

## Política ante fallos

1. Detener la secuencia.
2. Conservar el error útil sin publicar secretos.
3. Identificar si la causa es código, prueba, entorno o especificación.
4. Corregir la causa; no desactivar el gate.
5. Repetir primero la prueba fallida y después el gate de cierre.
6. Registrar fallo, corrección y resultado final.

## Corte técnico de Sprint 5

`J11-S5-04` reconfirmó la matriz después de incorporar composición única,
migraciones de plugins y el generador neutral:

- reactor A/B: 18 proyectos, 191 pruebas, cero fallos, errores u omisiones;
- composición: tres JAR de plugin idénticos en WAR y proveedor del migrador;
- PostgreSQL/Testcontainers: 12/12 escenarios verdes;
- Docker/Compose: migración repetida con cero cambios, health semántico y volúmenes
  PostgreSQL/Keycloak conservados después de `down` sin `--volumes`;
- Playwright: 5/5 recorridos verdes y 22 capturas revisadas en 375, 720 y 1280 px;
- G7: recorrido independiente de la guía aún pendiente y fuera de la autoridad de
  los autores.

Sprint 6 vuelve al flujo incremental normal y deberá ampliar esta matriz con las
pruebas del primer plugin `business_partners`.

## Corte de datos J11-S6-03

La persistencia del primer plugin productivo aplicó la matriz normal, sin diferir
pruebas:

- 20 pruebas unitarias del plugin: dominio, snapshot, descriptor, recurso SQL,
  mapeos y ausencia de baja física;
- 4 escenarios Flyway/PostgreSQL: base vacía, idempotencia, ocho tablas,
  aislamiento, unicidad, primarios y retención;
- 9 escenarios JPA/PostgreSQL: validación sin DDL, round-trip completo, empresa
  equivocada, duplicados informativos, conflictos, reemplazo de primario y
  secuencias concurrentes;
- 12 reglas ArchUnit verdes, incluidas neutralidad de puertos/dominio y entidades
  JPA dentro de la infraestructura de su propietario;
- `mvn verify` del reactor completo: 20 módulos y 217 pruebas verdes.

El perfil explícito se ejecuta con:

```powershell
.\mvnw.cmd -B -pl plugins/business-partners -am verify `
  "-Dlogixone.postgres.integration=true"
```

La composición WAR/migrador quedó verificada en `J11-S6-06` con el perfil
`with-business-partners-demo`, construcción base sin plugins, PostgreSQL,
Docker/Compose y Playwright. La repetición integral y seguridad negativa pertenecen
al gate de cierre `J11-S6-07`.

## Corte neutral J11-S8-02

El primer corte de `inventory` volvió al flujo incremental normal:

- `inventory-api`: 4 pruebas de UUID, precisión, disponibilidad, snapshots y
  contratos de movimiento/reserva;
- `inventory`: 12 pruebas de descriptor, dependencia requerida, `GENERAL`,
  inscripción de productos, trazabilidad, negativos, transferencia, reserva y
  conteo;
- 19 reglas dirigidas ArchUnit, incluidas API/dominio neutrales y prohibición de
  internos de `commercial_catalog`;
- `mvn verify`: 24/24 módulos, 321 pruebas, cero fallos, errores u omisiones;
- inspección: API sin Jakarta, plugin sin migraciones y WAR base sin inventario.

PostgreSQL/Testcontainers comienza en J11-S8-03. Docker/Compose y composición
física comienzan en J11-S8-06; Playwright comienza con la UI de J11-S8-05. No se
presentan esos gates como ejecutados anticipadamente.

## Corte de persistencia J11-S8-03

La persistencia de inventario exige dos suites PostgreSQL separadas:

- Flyway migra V1, repite con cero cambios y comprueba nueve tablas, aislamiento,
  negativos, serie positiva, idempotencia, reversión y bloqueo de conteos;
- Hibernate valida con DDL deshabilitado y prueba round-trip de los seis puertos,
  snapshots históricos, empresa negativa, actualizaciones y escritor obsoleto.

El gate unitario cubre recurso SQL, descriptor, snapshots, nueve entidades,
ausencia de borrado y traducción de SQLSTATE. El perfil
`-Dlogixone.postgres.integration=true` ejecuta ambos IT con la imagen PostgreSQL
fijada por digest. Docker/Compose, JTA de casos de uso y Playwright continúan en
J11-S8-04 a J11-S8-06; no se presentan como ejecutados en este corte.

## Corte de aplicación J11-S8-04

El corte de aplicación ejecuta autorización negativa antes de cualquier I/O,
empresa actual no sustituible, reglas de stock, conversión pública, idempotencia,
auditoría y rollback JTA. La migración V2 y JPA se prueban sobre PostgreSQL real:

- 41 pruebas unitarias de `inventory`, incluidas estructura, movimientos, reservas,
  conteos, adaptadores CDI y frontera transaccional;
- 10 escenarios PostgreSQL para V1→V2, repetición, restricciones, aislamiento,
  recibos de reserva y `validate` de diez entidades;
- 24 pruebas de arquitectura, incluidas 20 reglas ArchUnit;
- `mvn verify`: 24/24 módulos y 351 pruebas, sin fallos, errores ni omisiones;
- inspección: `inventory-api` sin Jakarta y WAR base sin clases de inventario.

Docker/Compose y composición física continúan en J11-S8-06. Playwright comienza
con la UI de J11-S8-05. La ausencia de esos gates en este corte es alcance, no una
declaración de Sprint cerrado.

## Corte de interfaz J11-S8-05

La interfaz de inventario se verifica en dos niveles antes de componerla
físicamente:

- las consultas empresariales validan directorios de depósitos, artículos y
  conteos;
- doce pruebas de handlers cubren carga autorizada, comandos válidos, permisos
  separados y errores seguros;
- dos pruebas del descriptor validan menús, pantallas, rutas y permiso de acceso;
- tres pruebas del renderer validan las tres presentaciones, acciones y pestañas;
- cinco escenarios de `InventoryJpaValidationPostgreSqlIT` validan además las
  proyecciones nuevas sobre PostgreSQL 18.4;
- el módulo `inventory` ejecuta 56/56 pruebas unitarias verdes.

El gate final obtuvo 24/24 pruebas de arquitectura, incluidas 20 reglas ArchUnit, y
`mvn verify` construyó 24/24 módulos con 369/369 pruebas verdes. La composición
WAR/migrador, fixture, Docker/Compose, capturas y Playwright real siguen siendo
responsabilidad de J11-S8-06; no se simulan como parte de esta historia.

## Corte de composición y demo J11-S8-06

La selección física `with-inventory-demo` agrega los tres plugins productivos y
los tres fixtures a una única lista consumida por WAR y migrador. El gate combina:

- contrato Maven para presencia exacta, dependencia indirecta de WAR/migrador y
  aceptación del perfil por ambos Dockerfiles;
- inspección de `WEB-INF/lib` y descubrimiento de seis descriptores;
- PostgreSQL 18.4 con Flyway V1–V2, repetición idempotente y seis escenarios JPA,
  incluido el bloqueo de conteos bajo JPQL estricto;
- imágenes de aplicación y migrador construidas con el mismo perfil, Compose y
  conservación de volúmenes;
- `InventoryVisualIT` sobre OIDC y UI real: activación, permisos, sesión renovada,
  depósito, inscripción, entrada 12, reserva 3, disponibilidad 9, conteo
  contabilizado, denegación por desactivación y restauración;
- responsive en 375, 599, 600, 720, 839, 840 y 1280 px, con 23 capturas revisadas.

Una sesión ya autenticada no incorpora concesiones posteriores: la prueba cierra e
inicia sesión después de cambiar activación/permisos. El servidor sigue revalidando
empresa, plugin y permiso en cada comando. J11-S8-07 repetirá la matriz integral y
la demo oficial antes de congelar el baseline; esta historia no cierra el Sprint.

## Corte de datos de referencia J11-S8-C03

La fundación `reference_data` amplía `with-inventory-demo` a siete plugins
físicos. Sus gates específicos cubren:

- API Java pura, descriptor, pantalla de sólo lectura y migración V1;
- PostgreSQL 18.4 con procedencia, `BOOTSTRAP_SUBSET`, `PY/PYG/USD`, aislamiento
  empresarial e idempotencia;
- revalidación transaccional de país en `business_partners` y moneda en
  `commercial_catalog`;
- 28 pruebas de arquitectura, incluidas las dependencias requeridas, el orden de
  composición y el rechazo de consumidores sin proveedor;
- construcción presente/ausente de WAR y migrador desde una única selección;
- Playwright sobre `/reference-data` y los selectores consumidores en 375, 720 y
  1280 px.

El importador de publicaciones completas y la reconciliación de códigos no se
simulan: permanecen como continuidad RD-04 a RD-06.

## Corte de composición y demo J11-S9-06

La selección `with-purchasing-demo` agrega Compras a la composición acumulada y
usa la misma lista física en WAR, migrador y Dockerfiles. El gate exige:

- prueba contractual del perfil presente y ausente, orden de dependencias y
  finales de línea reproducibles;
- `mvn verify` de 28 módulos y ArchUnit;
- Flyway V1–V2 y JPA sobre PostgreSQL 18.4/Testcontainers, incluida búsqueda de
  números comerciales sin sensibilidad a mayúsculas;
- dos ejecuciones idempotentes del migrador final;
- health/readiness y matriz OIDC negativa sobre la imagen desplegada;
- `PurchasingVisualIT` con dos identidades, separación solicitante/aprobador,
  proveedor/catálogos/depósito ficticios, solicitud, orden, recepción, devolución,
  seguimiento, desactivación y restauración;
- responsive 375/720/1280 y límites 599/600/839/840, con evidencia visual.

El corte materializado registró 549 pruebas, cero fallos, errores u omitidas. Los
fallos intermedios bloquearon el avance hasta corregir transacciones de consulta,
resolución exacta, preservación de selectores dependientes, entrada raíz y
selectores E2E ambiguos. La aceptación humana
independiente no se sustituye por esta matriz y continúa pendiente para J11-S9-07.
