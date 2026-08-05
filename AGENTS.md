# AGENTS.md

## Alcance

Estas instrucciones aplican a todo el repositorio `LogixoneJakarta11`. Un archivo `AGENTS.md` ubicado en un subdirectorio puede agregar reglas más específicas para ese módulo, sin contradecir las decisiones arquitectónicas globales.

## Propósito del proyecto

Construir desde cero un ERP con Jakarta EE 11, organizado como monolito modular basado en plugins. El sistema debe poder crecer por capacidades sin volver a crear el acoplamiento del sistema legado.

Los proyectos legados ubicados en `C:\cosme\multienvios\miaterra` y
`C:\cosme\felsina\ingeniolafelsina` son exclusivamente bases de conocimiento de
solo lectura. Se pueden consultar para identificar comportamiento, reglas de
negocio, datos, permisos, pantallas, reportes, procesos e integraciones, pero no
deben modificarse desde este proyecto.

No copiar código legado de forma mecánica. Antes de implementar una capacidad, convertir el comportamiento observado en requisitos, casos de uso, decisiones documentadas y pruebas de caracterización. No introducir dependencias `javax.*`.

## Baseline técnico

- Java 21 LTS.
- Jakarta EE 11.
- WildFly 41.
- Maven multimódulo y Maven Wrapper.
- PostgreSQL.
- JUnit 5 para pruebas unitarias.
- ArchUnit para límites arquitectónicos.
- Testcontainers y REST Assured para integración.
- Playwright para pruebas de interfaz cuando exista una UI navegable.
- Docker e infraestructura como código como forma oficial de construir y ejecutar el sistema.

Las versiones deben estar centralizadas en el POM padre o en el BOM del proyecto. Las APIs de Jakarta proporcionadas por WildFly deben usar alcance `provided`. No empaquetar implementaciones del servidor dentro de la aplicación.

Toda modificación del baseline debe justificarse mediante un ADR en `docs/adr/`.

## Arquitectura de plugins

El sistema comienza como un monolito modular desplegado en un WAR. Un plugin es un módulo Maven empaquetado como JAR e incorporado físicamente a la distribución.

- No implementar carga dinámica de JAR, hot install, OSGi ni classloaders personalizados salvo que un ADR futuro lo autorice.
- Agregar o retirar físicamente un plugin requiere reconstruir y redesplegar la distribución.
- Un plugin presente puede activarse o desactivarse por empresa en tiempo de ejecución.
- Desactivar o retirar un plugin no debe eliminar automáticamente sus tablas, migraciones ni datos.
- El kernel no puede depender de implementaciones de plugins.
- Un plugin no puede importar clases internas, DTO internos ni entidades de otro plugin.
- Los plugins se comunican mediante contratos públicos, identificadores, puertos y eventos.
- No se permiten relaciones JPA entre entidades pertenecientes a plugins diferentes.
- Un plugin debe declarar identidad, versión, compatibilidad, dependencias, capacidades, permisos, menú y migraciones.
- El registro debe rechazar identificadores duplicados, dependencias ausentes, ciclos y versiones incompatibles.
- Un plugin desactivado para una empresa no debe aportar menús, permisos operativos, tareas ni endpoints funcionales para esa empresa.

El módulo `plugin-api` debe ser Java puro. No puede depender de `jakarta.*`, WildFly, Hibernate, JSF, PrimeFaces ni módulos de infraestructura.

## Responsabilidades del kernel

El kernel es dueño únicamente de capacidades transversales:

- identidad y sesión;
- empresas y contexto empresarial;
- seguridad y autorización;
- configuración;
- auditoría;
- registro y activación de plugins;
- resolución de dependencias y compatibilidad;
- composición de contribuciones técnicas.

La lógica propia de ventas, inventario, transporte, facturación u otros dominios debe residir en plugins, no en utilidades compartidas ni controladores centrales.

## Persistencia y migraciones

- El núcleo y cada plugin son dueños de sus tablas y migraciones.
- Utilizar inicialmente una instancia PostgreSQL con separación lógica por esquemas o namespaces claramente documentados.
- Ningún plugin debe leer o escribir directamente las tablas privadas de otro plugin.
- Referenciar datos externos por identificador y resolverlos mediante contratos públicos.
- No utilizar actualización automática del esquema de Hibernate en entornos compartidos. El esquema debe validarse y evolucionar mediante migraciones versionadas.
- Las migraciones aplicadas son inmutables y deben conservar identidad, versión y checksum.
- Todo cambio destructivo de datos necesita estrategia de respaldo, compatibilidad, migración y recuperación documentada.
- Los secretos, credenciales y datos sensibles nunca se versionan.

### Documentos comerciales y referencia SIFEN

- Antes de diseñar factura, nota de crédito, nota de débito, remisión u otro
  documento fiscal/comercial, revisar el manual técnico SIFEN proporcionado y la
  versión oficial vigente aplicable.
- Usar SIFEN como referencia de estructura, cardinalidades, relaciones, ciclos de
  vida y datos que conviene preservar; no copiar mecánicamente el XML, nombres de
  nodos o reglas fiscales como modelo interno del ERP.
- Separar el documento comercial canónico de su representación SIFEN. El dominio
  debe poder existir y evolucionar sin depender de una versión particular del XSD,
  web service o proveedor fiscal.
- Persistir relacionalmente cabecera común, participantes históricos, ítems,
  impuestos, pagos, totales, referencias, transporte y extensiones por tipo. No
  usar una tabla única con cientos de columnas nulas, EAV ni XML/JSON como única
  fuente operativa.
- Conservar aparte los artefactos fiscales inmutables, versión del formato, CDC,
  firma, envíos, respuestas y eventos necesarios para auditoría y reproducción.
- Los datos de emisor, receptor, direcciones, descripciones y condiciones deben
  quedar como snapshots del momento de emisión; cambios posteriores en maestros no
  pueden reescribir documentos históricos.
- La lógica y tablas de estos documentos pertenecen a plugins funcionales, no al
  kernel. Los cruces entre plugins usan identificadores y contratos públicos, nunca
  relaciones JPA entre esquemas privados.
- Una edición antigua del manual puede orientar la descomposición conceptual, pero
  no autoriza cumplimiento fiscal. Antes de implementar o certificar la integración
  se deben verificar manual, XSD, catálogos y reglas oficiales vigentes y registrar
  sus versiones y checksums.

## Docker e infraestructura como código

- Usar builds multi-stage.
- Fijar las imágenes base por digest, además de documentar la etiqueta legible asociada.
- Construir una sola imagen de aplicación y promover exactamente su digest entre desarrollo, pruebas y producción.
- Mantener la configuración específica del entorno fuera de la imagen.
- Inyectar secretos mediante el mecanismo externo definido para el entorno; nunca mediante `Dockerfile`, Compose versionado o código fuente.
- Incluir health checks de vida y disponibilidad.
- Persistir PostgreSQL en volúmenes explícitos.
- Probar la configuración Compose, la construcción limpia, las migraciones, el arranque, la salud y la recreación de contenedores.
- No depender de recursos creados manualmente que no estén declarados como código o documentados como prerrequisito.

## Descargas y cachés del proyecto

- Todo archivo descargado para construir, probar, ejecutar o diagnosticar este proyecto debe almacenarse dentro de `C:\cosme\LogixoneJakarta11\.tools\`.
- Usar `.tools/downloads/` para archivos originales, `.tools/jdk/` para JDK extraídos, `.tools/maven-wrapper-home/` para distribuciones del Wrapper y `.tools/maven-repository/` para dependencias Maven.
- Las descargas parciales deben usar `.tools/tmp/`; no dejar descargas del proyecto en `C:\tmp`, el perfil del usuario u otras carpetas externas.
- Verificar tamaño y checksum publicados antes de usar un binario descargado.
- Documentar origen, versión, checksum, ubicación y resultado de validación.
- `.tools/` es local y no se versiona. Tampoco debe copiarse a imágenes Docker ni artefactos de distribución.
- No descargar nuevamente un artefacto ya validado si existe en `.tools/`; reutilizarlo por checksum.

### Bootstrap reproducible de Maven en Windows

- `mvnw.cmd` es el único punto de entrada canónico para Maven en Windows y, cuando
  existe, selecciona automáticamente el JDK validado
  `.tools/jdk/jdk-21.0.11+10`, aunque el `JAVA_HOME` global apunte a Java 8 u otra
  versión.
- El mismo Wrapper fija `MAVEN_USER_HOME` en `.tools/maven-wrapper-home`; no se
  debe repetir manualmente la preparación de `JAVA_HOME`, `PATH` o
  `MAVEN_USER_HOME` antes de cada comando Maven. También aísla `TEMP` y `TMP` en
  `.tools/tmp` para que Maven, JUnit y los plugins no dependan del perfil global.
- Ejecutar directamente `mvnw.cmd --version`, `mvnw.cmd test` o
  `mvnw.cmd verify`. Sólo se permite forzar un entorno incorrecto al probar
  deliberadamente la regresión del bootstrap.
- Si el JDK local existe y `mvnw.cmd --version` informa un Java distinto de 21 o
  un Maven home fuera de `.tools`, tratarlo como un defecto bloqueante del
  Wrapper; no continuar repitiendo el ajuste manual de la terminal.
- Si el JDK local no existe, aprovisionarlo y validarlo mediante el procedimiento
  documentado bajo `.tools/`; no descargarlo en el perfil del usuario ni ocultar
  la ausencia recurriendo al Java global.

## Flujo obligatorio de cambios y pruebas

Trabajar mediante cambios pequeños y coherentes. Después de cada cambio de código, ejecutar inmediatamente la prueba más pequeña que pueda demostrarlo. No iniciar el siguiente cambio mientras exista una prueba relevante fallando.

Secuencia mínima:

1. Implementar un único cambio coherente.
2. Ejecutar las pruebas del módulo con Maven Wrapper, por ejemplo `mvnw.cmd -pl <modulo> -am test` en Windows o `./mvnw -pl <modulo> -am test` en Linux.
3. Ejecutar `mvnw.cmd verify` o `./mvnw verify` al completar un corte coherente.
4. Si se afectan arquitectura o plugins, ejecutar las pruebas ArchUnit y construir la distribución con el plugin de referencia presente y ausente.
5. Si se afectan Docker, configuración, persistencia o despliegue, construir la imagen y ejecutar las pruebas Compose, migraciones, health checks y smoke tests.
6. Registrar el resultado y la evidencia relevante en la historia o documento del Sprint.

No omitir, desactivar ni relajar una prueba para conseguir un build verde. Corregir la causa o documentar el bloqueo antes de continuar.

### Excepción temporal aprobada para la candidata visual de Sprint 3

Por decisión del responsable de producto del 2026-07-28, durante `J11-S3-01` a `J11-S3-07` las pruebas automatizadas pueden acumularse y ejecutarse después de terminar la candidata de demo visual.

- Se permiten compilaciones, empaquetados y arranques necesarios para construir y observar la candidata.
- Cada historia de código debe conservar el estado `Implementada pendiente de validación`; no se considera terminada ni verde.
- Una prueba que sí se ejecute y falle continúa siendo un bloqueo: no puede ignorarse ni presentarse como diferida.
- No se permite cerrar el Sprint, promover una imagen, publicar la guía `1.0` ni desplegar a producción con gates pendientes.
- `J11-S3-08` debe ejecutar de forma acumulada pruebas de módulo, `mvn verify`, ArchUnit, PostgreSQL/Testcontainers, JTA, Keycloak/OIDC, Docker/Compose, health, persistencia, seguridad negativa y Playwright.
- Después del cierre de Sprint 3 vuelve a regir el flujo incremental normal salvo una nueva decisión explícita.

### Excepción temporal aprobada para la administración visual de Sprint 4

Por decisión del responsable de producto del 2026-07-28, durante `J11-S4-01` a
`J11-S4-07` se puede completar implementación y documentación dejando únicamente
las pruebas automatizadas como pendientes hasta el gate acumulado `J11-S4-08`.

- Cada historia de código debe usar el estado `Implementada pendiente de pruebas`.
- El estado implica que no quedan decisiones, código, migraciones o documentación
  conocidos pendientes dentro del alcance de la historia; solamente falta ejecutar
  su matriz de pruebas.
- Una prueba que sí se ejecute y falle continúa siendo un bloqueo y debe corregirse
  antes de avanzar.
- No se puede cerrar Sprint 4, promover una imagen, desplegar a producción ni
  declarar terminado el kernel con pruebas pendientes.
- `J11-S4-08` debe ejecutar acumuladamente módulos, `mvn verify`, ArchUnit,
  PostgreSQL/Testcontainers, JPA/JTA, OIDC, Docker/Compose, seguridad negativa y
  Playwright de administración.
- Después del cierre de Sprint 4 vuelve a regir el flujo incremental normal salvo
  otra decisión explícita.

La excepción modifica el calendario de pruebas, no los criterios de aceptación ni
la Definition of Done.

### Continuidad autorizada con validación independiente de Sprint 4 pendiente

Por decisión del responsable de producto del 2026-07-29, Sprint 5 puede avanzar
con las fundaciones transversales de plugins mientras la validación independiente
de `J11-S4-08` permanece pendiente.

- Los gates técnicos ya ejecutados de Sprint 4 conservan su resultado verde; no se
  reclasifican como pendientes.
- Sprint 4 continúa abierto y no se autoriza promover imágenes, publicar la guía
  `1.0` ni desplegar a producción.
- Sprint 5 vuelve al flujo incremental normal: cada cambio de código debe ejecutar
  inmediatamente su prueba mínima y una prueba fallida bloquea el avance.
- La autorización comprende composición, migraciones, plantilla y contratos
  transversales; no permite saltar el orden de plugins definido por ADR-0011.

Esta excepción modifica el momento de ejecución, no los criterios de aceptación ni la Definition of Done.

## Calidad y seguridad

- Mantener dominio y aplicación independientes de Jakarta, UI, base de datos y servidor siempre que sea razonable.
- Preferir contratos pequeños, explícitos y estables.
- Evitar clases utilitarias globales y controladores que acumulen responsabilidades de varios dominios.
- Validar entradas en los límites del sistema.
- Aplicar autorización en la capa de aplicación o servicio, no solamente ocultando elementos de interfaz.
- No registrar contraseñas, tokens, secretos ni datos personales innecesarios.
- Mantener logs estructurados con contexto de empresa, usuario, plugin y operación cuando corresponda.
- No agregar una dependencia sin justificar su necesidad, licencia, versión y superficie de mantenimiento.

## Interfaz Jakarta Faces, Material Design y responsive

- Jakarta Faces 4.1 es la tecnología server-side obligatoria para la interfaz. No convertir pantallas en una SPA ni sustituir JSF sin un ADR aprobado.
- Material Design 3 es el sistema de diseño visual del producto. Debe aplicarse sobre JSF mediante HTML semántico, componentes Faces, tokens CSS y patrones controlados por el shell.
- El uso de Material Design no autoriza por sí solo una biblioteca, Web Components, JavaScript remoto, fuentes o iconos externos. Toda dependencia visual adicional requiere versión centralizada, licencia, necesidad, compatibilidad con JSF, evaluación de seguridad y ADR.
- Cada pantalla nueva o modificada debe ser responsive desde su propia historia y funcionar, como mínimo, en los rangos de proyecto compacto (`0–599px`), medio (`600–839px`) y expandido (`840px` o más).
- En el ancho compacto, el contenido normal no debe exigir desplazamiento horizontal; acciones, formularios, tablas y navegación deben reordenarse o adoptar un patrón alternativo explícito.
- Toda vista debe usar los roles de color, tipografía, forma, espaciado, elevación y estados definidos centralmente. No duplicar temas ni introducir valores visuales globales desde plugins.
- La accesibilidad forma parte del componente: HTML semántico, labels, teclado, foco visible, contraste suficiente, estados que no dependan solo del color, mensajes comprensibles y respeto de `prefers-reduced-motion`.
- El shell es dueño del tema, layouts y renderers permitidos. Los plugins funcionales y de personalización aportan contratos neutrales; no inyectan XHTML, EL, CSS o JavaScript arbitrarios.
- Cada selector debe declarar fuente, propietario y una de estas clases: estado
  cerrado, catálogo empresarial, referencia operativa, catálogo normativo o
  composición/despliegue. No usar texto libre ni listas hardcodeadas para evitar
  modelar un catálogo empresarial.
- Un selector de catálogo empresarial o referencia administrable debe ofrecer una
  ruta visible `Administrar` o `Agregar` cuando el actor tenga permiso. La ruta
  pertenece al plugin dueño, revalida empresa y autorización en el servidor,
  conserva historia e inactiva sin borrar referencias. Al volver, el shell debe
  refrescar opciones y preservar únicamente un borrador seguro.
- Estados, operaciones, permisos y códigos normativos cerrados no admiten altas
  arbitrarias desde la UI. Deben explicar su origen y cambiar solamente mediante
  dominio/migración versionados, actualización oficial o composición física,
  según corresponda.
- Cada historia visual y cierre de Sprint debe revisar el inventario de selectores,
  incluyendo fuente, ruta, permiso, vacío, inactivos, listas grandes, teclado y
  responsive. Un catálogo empresarial huérfano bloquea la historia.
- Los criterios de aceptación de toda historia visual deben incluir los tres rangos responsive y los estados pertinentes. Playwright debe verificarlos antes de cerrar la historia o el gate acumulado autorizado.

## Documentación y decisiones

- Guardar decisiones arquitectónicas en `docs/adr/`.
- Guardar el conocimiento extraído del legado en `docs/knowledge-base/`.
- Guardar backlog, criterios de aceptación y evidencias en `docs/backlog/` y `docs/sprints/`.
- Mantener en `docs/implementation-guide/` la guía destinada a quienes implementan el ERP para una empresa. Toda historia que cambie onboarding, configuración, plugins, personalización, datos, despliegue, seguridad u operación debe evaluar y actualizar la guía en el mismo cambio.
- Mantener en `docs/user-guide/` el manual de usuario orientado a tareas y en
  `docs/developer-guide/` el manual técnico para desarrolladores. Una historia que
  cambie un recorrido, mensaje, permiso, límite o pantalla debe actualizar el
  manual de usuario; una historia que cambie arquitectura, contrato, módulo,
  migración, build, prueba, despliegue u operación debe actualizar el manual
  técnico.
- Mantener `docs/runbooks/levantar-logixone-visual-studio-code.md` como recorrido
  reproducible para Visual Studio Code. Debe declarar versión o fecha de
  verificación, extensiones requeridas con identificador y editor, JDK, Maven
  Wrapper, configuración local segura, construcción, ejecución oficial mediante
  Docker/Compose, health, pruebas, detención sin pérdida y diagnóstico.
- Actualizar la documentación en el mismo cambio que altere un contrato, comando, variable, arquitectura o procedimiento operativo.
- Ante ambigüedad que pueda cambiar arquitectura, seguridad, datos o compatibilidad, detenerse y solicitar decisión antes de implementar.

### Paquete documental obligatorio de cierre de Sprint

- Cada Sprint debe crear
  `docs/sprints/sprint-XX/estructura-plugins-y-dependencias.md` como fotografía
  inmutable del baseline que se pretende cerrar. El documento debe distinguir
  plugins funcionales, técnicos y de personalización; identificar versión,
  propietario, contrato público, esquema, menús, permisos, dependencias de plugin
  requeridas u opcionales y perfiles de composición física.
- La fotografía debe derivarse de POM, descriptores y migraciones reales. No puede
  representar como implementado un plugin solamente planificado ni confundir una
  dependencia Maven/técnica con una dependencia funcional declarada en el
  descriptor.
- Debe incluir al menos un gráfico legible de dependencias y composición, más una
  tabla o descripción textual equivalente para accesibilidad y exportación. El
  gráfico debe mostrar dirección y tipo de relación, plugins aislados y el camino
  común hacia WAR y migrador.
- Debe resumir cambios respecto del Sprint anterior, riesgos, dependencias futuras
  ya previstas y ausencia de accesos JPA o tablas cruzadas. La evidencia de cierre
  debe registrar que el gráfico y su alternativa textual fueron revisados.
- El manual de usuario debe organizarse por audiencia y objetivos, prerrequisitos,
  tareas paso a paso, resultados esperados, recuperación ante errores, permisos,
  límites, accesibilidad, glosario, versión y canal de soporte. Se redacta en
  lenguaje claro y se mantiene alineado, sin afirmar certificación, con
  ISO/IEC/IEEE 26514, IEC/IEEE 82079-1, ISO 24495-1 y WCAG 2.2.
- El manual técnico debe explicar arquitectura, módulos, contratos, anatomía de un
  plugin, persistencia, seguridad, UI, composición, pruebas, Docker, operación,
  documentación y Definition of Done. Debe enlazar los ADR como fuente de las
  decisiones y separar hechos vigentes de trabajo planificado.
- En cada cierre se revisan y actualizan, cuando el incremento los afecte, la guía
  de Visual Studio Code, el manual de usuario, el manual técnico y la guía de
  implementación. Aun cuando no necesiten cambios, la evidencia debe registrar la
  revisión y su justificación.

### Demo visual obligatoria de cierre de Sprint

- Todo Sprint debe terminar con una demo visual navegable construida desde el
  mismo baseline que se pretende cerrar. Una presentación, captura aislada, mock o
  descripción oral no sustituye la ejecución real del sistema.
- La demo debe mostrar el valor incremental del Sprint mediante Jakarta Faces,
  Material Design 3 y los contratos reales del producto. No se crea lógica o datos
  falsos únicamente para aparentar una capacidad inexistente.
- Un Sprint predominantemente técnico debe exponer una visualización segura y
  útil del resultado —por ejemplo estado operativo, configuración no sensible,
  diagnóstico, administración o recorrido técnico— sin revelar secretos ni abrir
  atajos de autorización.
- El cierre debe incluir un guion reproducible en `docs/runbooks/`, datos ficticios
  o procedimiento de preparación, ruta inicial, pasos, resultados esperados,
  limitaciones y restauración del estado de demostración.
- La demo debe ejecutarse y conservar evidencia visual en compacto (`375px`),
  medio (`720px`) y expandido (`1280px`), además de navegación, autorización,
  estados vacíos/error relevantes y ausencia de overflow horizontal normal.
- La evidencia de cierre debe registrar baseline o digest, ambiente, usuario/rol
  ficticio, rutas recorridas, resultado de cada paso y ubicación de capturas o
  artefactos, sin secretos ni datos reales.
- Una demo no reemplaza pruebas, revisión de seguridad, validación integral, guía,
  retrospectiva ni PDF. Si cualquiera de esos gates permanece pendiente, el Sprint
  sigue abierto aunque la interfaz sea demostrable.
- No se puede declarar cerrado un Sprint sin una demo visual ejecutada, reproducible
  y coherente con las capacidades y pendientes documentados.

### PDF obligatorio de cierre de Sprint

- Al finalizar cada Sprint, regenerar `docs/output/pdf/guia-estructura-repositorio-logixone.pdf` contra el baseline final que se pretende cerrar.
- El PDF debe identificar Sprint y fecha, explicar la arquitectura vigente, recorrer carpetas y archivos mantenidos, distinguir fuentes de artefactos generados y resumir capacidades implementadas, capacidades pendientes y siguiente trabajo autorizado.
- El PDF es un artefacto derivado para consulta; el código y los Markdown versionados continúan siendo las fuentes canónicas.
- Renderizar todas las páginas a imágenes, revisar visualmente portada, índice, encabezados, pies, tablas, diagramas, cortes y caracteres, y corregir cualquier defecto antes de aceptar el archivo.
- Reabrir el PDF final para comprobar número de páginas, metadatos, texto extraíble y ausencia de páginas vacías o caracteres dañados.
- Registrar en la evidencia de cierre la ruta, cantidad de páginas, tamaño, checksum SHA-256 y resultado de la revisión visual.
- Un Sprint no puede declararse cerrado mientras esta edición del PDF esté pendiente, desactualizada o sin verificación.

### Decisión de instalador Windows en cada cierre de Sprint

- Al finalizar cada Sprint, después de completar los demás gates del baseline
  candidato y antes de declarar el cierre, preguntar explícitamente al responsable
  de producto: `¿Crearemos un nuevo instalador Windows para este Sprint?` Registrar
  respuesta `SÍ` o `NO`, fecha, responsable y razón. Sin respuesta, el cierre queda
  pendiente de decisión.
- Con respuesta `SÍ`, regenerar el instalador contra exactamente ese baseline. Se
  convierte en el último gate técnico y el Sprint no se cierra hasta verificarlo.
- Con respuesta `NO`, no borrar ni reemplazar `installer/windows/current`. Registrar
  que el instalador anterior pertenece a otro baseline, no representa el Sprint
  nuevo y no puede entregarse como instalador de esa versión. El Sprint puede
  cerrar si todos los demás gates están verdes y la decisión quedó evidenciada.
- La primera plataforma soportada será Windows de 64 bits. No crear ni anunciar
  instaladores Linux hasta aprobar por separado distribuciones, paquetes,
  privilegios, servicios y matriz de pruebas.
- Antes de solicitar elevación o modificar la máquina, ejecutar un diagnóstico de
  solo lectura de sistema, arquitectura, CPU/RAM/disco, virtualización/WSL,
  Docker/Compose, permisos, reinicio pendiente, red/proxy/TLS, puertos, rutas e
  instalación previa. El resultado debe ser `COMPATIBLE`,
  `COMPATIBLE_CON_ADVERTENCIAS` o `BLOQUEADA`, con razones comprensibles.
- Una máquina bloqueada no se modifica. En una máquina compatible, mostrar antes
  del consentimiento la lista completa de componentes, versiones, descargas,
  tamaños, licencias, rutas, puertos, reinicios y acciones propuestas.
- Solicitar consentimiento explícito y elevar mediante UAC únicamente justo antes
  de la primera acción que realmente necesite privilegios. Rechazar o cancelar
  debe producir un resultado seguro, explicable y recuperable.
- Mostrar progreso por fase y registrar qué fue instalado, reutilizado, omitido o
  falló. Los logs no contienen secretos y cada error declara causa, efecto,
  recuperación y ubicación de evidencia.
- Descargar solo desde orígenes aprobados con versiones fijadas, hash o firma y
  licencia documentada. No desactivar UAC, antivirus, firewall, Secure Boot,
  políticas corporativas u otros controles para forzar la instalación.
- Detectar instalaciones, configuración, contenedores, volúmenes y datos
  existentes. Instalar, actualizar, reparar, cancelar o fallar no debe pisar ni
  eliminar datos; la eliminación destructiva queda fuera del flujo normal.
- El código fuente, manifiestos, pruebas y evidencias del instalador se mantienen.
  Cuando la respuesta sea `SÍ`, se reemplazan solamente los archivos derivados
  declarados dentro del directorio exclusivo `current`, después de resolver y
  verificar su ruta absoluta; nunca ejecutar una eliminación recursiva amplia ni
  borrar releases ya publicados.
- Construir primero en un directorio temporal, verificar versión, baseline/digest,
  licencias, SHA-256 y firma aplicable, y promover atómicamente a `current`. El
  directorio `current` debe contener una única edición vigente.
- Probar al menos VM Windows limpia compatible, máquina incompatible, puerto
  ocupado, requisito preinstalado, rechazo de UAC, cancelación, descarga/hash
  inválido, actualización/reparación y conservación de volúmenes después de
  recrear Compose.
- Para distribución externa se requiere firma Authenticode válida. Una iteración
  interna no firmada debe declararlo y no puede entregarse como instalador
  productivo.
- Registrar en la evidencia ruta, versión, Sprint, baseline/digest, tamaño,
  SHA-256, firma, terceros/licencias, ambientes, resultados de preflight,
  instalación, actualización, cancelación, health y persistencia.
- La fuente funcional y el procedimiento se mantienen en
  `docs/backlog/epica-instalador-windows-reproducible.md` y
  `docs/runbooks/metodologia-instalador-windows-cierre-sprint.md`.

## Git y protección del trabajo

- Preservar cambios existentes del usuario y evitar modificaciones ajenas a la historia activa.
- No reescribir historial ni usar operaciones destructivas.
- No crear commits, publicar ramas, abrir pull requests ni desplegar a producción sin autorización explícita del usuario.
- Cada entrega debe indicar archivos modificados, pruebas ejecutadas y resultados.

## Definition of Done

Una historia de código solo está terminada cuando:

- cumple sus criterios de aceptación;
- sus pruebas específicas están verdes;
- `mvn verify` está verde para el alcance correspondiente;
- respeta los límites arquitectónicos;
- no contiene secretos;
- mantiene reproducibilidad en Docker cuando aplica;
- incluye documentación y migraciones necesarias;
- conserva operativo el último baseline verde.

Un Sprint solo está terminado cuando, además, su validación integral está verde, la
demo visual obligatoria fue ejecutada y documentada, la retrospectiva y el siguiente
trabajo están registrados, la fotografía de estructura y dependencias de plugins
fue creada, las guías y manuales vigentes fueron revisados, y el PDF obligatorio de
cierre fue regenerado y verificado conforme a las secciones anteriores. También
debe haberse preguntado y registrado si se creará un instalador Windows. Si la
respuesta es `SÍ`, debe estar regenerado y probado contra el baseline final; si es
`NO`, el artefacto anterior permanece intacto y marcado como no representativo.

## Orden inicial de implementación

1. `J11-S1-01`: baseline y ADR arquitectónicos.
2. `J11-S1-02`: esqueleto Maven reproducible.
3. `J11-S1-03`: Docker e infraestructura como código.
4. `J11-S1-04`: contratos de plugins y validaciones.
5. `J11-S1-05`: kernel, descubrimiento CDI y plugin de referencia.
6. `J11-S1-06`: aplicación mínima y endpoints de salud.
7. `J11-S1-07`: pruebas integrales, evidencias y cierre del Sprint.

No adelantar historias posteriores para compensar una historia anterior que todavía no esté verde.

Las excepciones vigentes de Sprint 3 y Sprint 4 están documentadas arriba. Permiten
avanzar entre historias implementadas pendientes de sus gates acumulados, pero no
declarar cerrado un Sprint, ignorar una prueba fallida ni promover a producción.
