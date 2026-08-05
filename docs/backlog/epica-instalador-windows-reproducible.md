# Épica - Instalador Windows reproducible por Sprint

- Estado: Primera implementación interna completada en Sprint 8; distribución
  externa y matriz independiente pendientes
- Fecha de incorporación: 2026-07-31
- Prioridad: entregable transversal condicionado a confirmación de producto en
  cada cierre
- Plataforma inicial: Windows de 64 bits
- Plataformas futuras: Linux, mediante épica y decisiones posteriores
- Decisión vigente: [ADR-0029](../adr/0029-confirmacion-instalador-por-cierre-sprint.md)

## Objetivo

Cuando producto responda `SÍ` al cierre de un Sprint, entregar después de congelar
el baseline funcional un instalador Windows nuevo que evalúe la máquina, explique
si puede ejecutar Logixone, solicite consentimiento y elevación cuando correspondan,
instale los prerrequisitos aprobados, monte el proyecto, muestre avance verificable
y compruebe el sistema.

El instalador es un artefacto derivado del baseline. Su código fuente, manifiestos,
pruebas y documentación se mantienen; únicamente se reemplaza el artefacto
generado marcado como `current` del Sprint anterior.

Sprint 8 materializó este alcance en `installer/windows/` mediante ADR-0026. La
edición `0.8.0-internal.1` diagnostica, presenta el plan, exige consentimiento,
instala/repara y valida health conservando volúmenes. Está `NotSigned` y se limita a
evaluación interna hasta completar Authenticode y la matriz de VM.

La evolución [WIN-I09](WIN-I09-seleccion-plugins-dependencias.md) agregará una
selección explícita de plugins con resolución transitiva de dependencias. No forma
parte de `0.8.0-internal.1` y debe validarse antes de anunciar composición
configurable a una empresa.

## Interpretación de “finalizar el Sprint”

El orden obligatorio será:

1. completar código, migraciones, documentación, pruebas y demo del incremento;
2. congelar el baseline candidato y regenerar el PDF de cierre;
3. preguntar si se creará un nuevo instalador y registrar `SÍ` o `NO`;
4. con `SÍ`, eliminar de forma acotada sólo los derivados declarados de `current`,
   construir contra el baseline exacto y ejecutar la matriz;
5. con `NO`, conservar `current` intacto y marcarlo como no representativo del
   Sprint nuevo;
6. registrar decisión, hashes, ambiente y resultado aplicable;
7. declarar formalmente cerrado el Sprint cuando los gates correspondientes estén
   satisfechos.

Por tanto, el instalador se genera después de terminar el incremento solamente con
respuesta afirmativa; en ese caso sigue siendo un gate previo al cierre formal.

## Alcance inicial

La primera versión será un bootstrapper Windows para ambientes locales de
desarrollo y demostración. Debe poder preparar el recorrido oficial basado en
Docker/Compose y Maven Wrapper sin instalar un WildFly paralelo.

Perfil inicial obligatorio:

- adquirir o verificar el paquete exacto del proyecto;
- verificar Java 21, Git cuando la adquisición lo requiera, Docker Desktop/Engine
  compatible, Compose y capacidades Windows/WSL necesarias;
- preparar `.tools/` y la configuración local sin secretos embebidos;
- construir con Maven Wrapper o usar los artefactos/imágenes exactos declarados;
- ejecutar migrador y aplicación mediante Compose;
- esperar liveness/readiness y mostrar las rutas de acceso;
- permitir detener la aplicación sin eliminar volúmenes.

Visual Studio Code y sus extensiones podrán ofrecerse como componente de
desarrollo seleccionado explícitamente. IntelliJ IDEA Ultimate u otro software
con licencia no se instalará silenciosamente: se detectará y se explicará su
tratamiento, licencia y opción de instalación independiente.

## Selección física de plugins planificada

Antes de solicitar consentimiento, el instalador mostrará el catálogo de plugins
disponible para el baseline y permitirá seleccionar la composición deseada. La
selección se resolverá desde los descriptores reales:

- elegir un plugin incorpora recursivamente todas sus dependencias `REQUIRED`;
- cada selección automática muestra qué consumidor la exige;
- una dependencia compartida se incorpora una sola vez con versión compatible;
- una dependencia requerida no puede quitarse mientras conserve consumidores;
- dependencias opcionales se sugieren, pero no se fuerzan silenciosamente;
- ausencias, ciclos, duplicados o incompatibilidad de versiones bloquean el plan;
- el conjunto cerrado y su huella deben ser idénticos en WAR y migrador.

La operación produce una composición física que requiere construcción o selección
de artefactos previamente construidos y redespliegue. No implementa carga dinámica
de JAR. La activación por empresa ocurre después y continúa gobernada por el
kernel. Retirar un plugin en una actualización no elimina sus tablas ni datos.

## Evaluación previa de la máquina

Antes de solicitar elevación o modificar el equipo, el instalador debe ejecutar
un diagnóstico de solo lectura y producir uno de estos estados:

- **COMPATIBLE:** puede continuar con los requisitos mínimos;
- **COMPATIBLE_CON_ADVERTENCIAS:** puede continuar, pero declara límites o
  acciones opcionales;
- **BLOQUEADA:** no realiza cambios y enumera cada requisito incumplido.

La matriz versionada debe revisar como mínimo:

| Área | Comprobación |
|---|---|
| sistema | versión/edición/build Windows y arquitectura soportados |
| hardware | CPU, RAM mínima/recomendada y espacio libre por unidad |
| virtualización | capacidad de CPU, estado de virtualización y requisitos WSL2/hipervisor |
| contenedores | Docker/Compose presente, versión compatible y motor accesible |
| permisos | usuario actual, posibilidad de elevación y políticas que puedan bloquearla |
| reinicio | actualización o instalación pendiente de reinicio |
| red | acceso requerido, proxy, DNS, TLS y registros de paquetes/imágenes |
| puertos | `18080`, `8180` y otros declarados libres o con conflicto explicado |
| rutas | ubicación elegida, permisos, longitud y caracteres admitidos |
| instalación previa | versión, configuración, contenedores, volúmenes y datos existentes |
| seguridad | firmas/hashes de paquetes y controles que no deben desactivarse |

Los mínimos exactos, versiones soportadas y reglas de compatibilidad deben vivir
en un manifiesto versionado, no dispersos en la interfaz.

## Experiencia obligatoria

El instalador debe mostrar una secuencia comprensible:

1. bienvenida, versión del Sprint y propósito;
2. diagnóstico con progreso y resultado por requisito;
3. selección de plugins, dependencias incorporadas automáticamente y explicación
   de cualquier bloqueo;
4. lista completa de acciones propuestas, descargas, tamaños, ubicaciones,
   licencias y reinicios posibles;
5. opciones permitidas y tratamiento de componentes ya instalados;
6. consentimiento explícito antes de cualquier cambio;
7. solicitud UAC justo antes de la primera acción que realmente la necesite;
8. progreso por fase, porcentaje o pasos deterministas y operación actual;
9. mensajes de error con causa, efecto, recuperación y ubicación del log;
10. validación final de migración, contenedores, liveness y readiness;
11. resumen de instalado/reutilizado/omitido/fallido y siguientes pasos.

Cancelar antes del consentimiento no deja cambios. Cancelar durante la ejecución
debe finalizar de forma segura o explicar qué componente quedó instalado y cómo
reanudar/revertir.

## Seguridad y datos

- No incluir contraseñas, tokens, certificados privados ni datos empresariales.
- Descargar solo desde orígenes aprobados usando versiones fijadas y hashes o
  firmas verificables.
- Mostrar y aceptar las licencias aplicables antes de instalar un tercero.
- No desactivar antivirus, firewall, UAC, políticas corporativas, Secure Boot o
  controles de ejecución para forzar compatibilidad.
- No reutilizar credenciales fuera del mecanismo normal del componente.
- Sanear logs y reportes para excluir secretos y datos personales innecesarios.
- Detectar configuración y volúmenes existentes antes de construir o levantar.
- Una actualización conserva PostgreSQL, Keycloak y datos por defecto; eliminarlos
  requiere un flujo separado, explícito y destructivo que no forma parte del
  instalador normal.
- Si hace falta reiniciar Windows, guardar estado seguro y continuar solo después
  de la confirmación del usuario.

## Reemplazo del instalador anterior

La regeneración no puede usar una eliminación recursiva amplia. Debe:

1. resolver la ruta absoluta del directorio de salida `current` definido por el
   proyecto;
2. comprobar que está dentro del directorio exclusivo de artefactos Windows;
3. inventariar y registrar nombre, versión y SHA-256 del artefacto anterior;
4. eliminar únicamente los archivos derivados declarados por el manifiesto;
5. construir el nuevo artefacto en un directorio temporal;
6. verificarlo y promoverlo atómicamente a `current`.

No se eliminan fuentes, pruebas, manifiestos, evidencias, hashes históricos ni
artefactos ya publicados en un repositorio de releases. El directorio local
`current` contiene una sola edición vigente.

## Artefactos esperados cuando la respuesta es SÍ

- instalador ejecutable Windows;
- manifiesto legible con Sprint, versión, baseline/digest y dependencias;
- archivo SHA-256;
- reporte de software de terceros y licencias;
- matriz de requisitos mínimos/recomendados;
- log de construcción sin secretos;
- reporte de pruebas sobre máquinas limpias e incompatibles;
- manual de uso, cancelación, actualización, reparación y diagnóstico.

Antes de distribuir fuera del equipo se requerirá firma Authenticode válida. La
primera iteración interna puede quedar explícitamente marcada como no firmada si
aún no existe certificado, pero no podrá presentarse como instalador productivo ni
enviarse a una empresa.

## Backlog técnico inicial

| Historia | Resultado |
|---|---|
| WIN-I01 | ADR de tecnología, perfiles, adquisición, firma y actualización |
| WIN-I02 | manifiesto de requisitos y diagnóstico de solo lectura |
| WIN-I03 | interfaz, consentimiento, UAC mínimo y progreso |
| WIN-I04 | instalación/reutilización verificable de prerrequisitos |
| WIN-I05 | montaje del proyecto, Compose, health y accesos |
| WIN-I06 | actualización/reparación, cancelación, rollback y datos persistentes |
| WIN-I07 | pipeline de regeneración `current`, hashes, licencias y evidencia |
| WIN-I08 | matriz en VM limpia, máquina incompatible y preinstalación existente |
| [WIN-I09](WIN-I09-seleccion-plugins-dependencias.md) | selector de plugins, cierre transitivo de dependencias y composición WAR/migrador idéntica |

Sprint 8 incorporará estas capacidades dentro de su historia final de instalador;
si el tamaño obliga a dividirlas, el Sprint no podrá cerrar con un bootstrapper
parcial presentado como completo.

## Criterios de aceptación

- **CE-01:** el diagnóstico ocurre antes de cualquier modificación o elevación.
- **CE-02:** una máquina bloqueada recibe razones y soluciones sin cambios.
- **CE-03:** el usuario ve y acepta todas las acciones antes de instalar.
- **CE-04:** UAC se solicita solo para acciones que lo requieren.
- **CE-05:** cada descarga se fija y verifica contra origen/hashes aprobados.
- **CE-06:** el progreso identifica fase, acción y resultado.
- **CE-07:** una instalación limpia termina con migración y health verdes.
- **CE-08:** actualizar/reparar no pisa configuración, volúmenes ni datos.
- **CE-09:** cancelar o fallar ofrece reanudación o recuperación documentada.
- **CE-10:** el instalador corresponde al mismo baseline/digest del cierre.
- **CE-11:** se reemplaza solo el artefacto local `current`, nunca las fuentes.
- **CE-12:** versión, tamaño, SHA-256, firma, licencias y pruebas quedan evidenciados.
- **CE-13:** el instalador anterior no permanece en el directorio `current`.
- **CE-14:** Linux queda explícitamente fuera hasta una decisión posterior.
- **CE-15:** el Sprint no se declara cerrado sin respuesta `SÍ` o `NO` registrada.
- **CE-16:** con `SÍ`, el instalador no puede quedar ausente, desactualizado o sin
  pruebas; con `NO`, `current` permanece intacto y se documenta como ajeno al
  baseline nuevo.
- **CE-17:** el instalador muestra los plugins disponibles y distingue selección
  directa, automática, obligatoria y opcional.
- **CE-18:** seleccionar un plugin incorpora recursivamente sus dependencias
  requeridas y explica el motivo de cada incorporación.
- **CE-19:** una composición inválida se bloquea antes de consentimiento, UAC o
  cambios en el equipo.
- **CE-20:** la huella y los JAR resueltos son idénticos en aplicación y migrador;
  actualización o retiro conserva migraciones y datos.

## Decisiones técnicas pendientes de implementación

Antes de escribir el instalador se deberá aprobar mediante ADR:

- tecnología de empaquetado/bootstrapping y licencia;
- requisitos mínimos y versiones Windows soportadas;
- topología, instalación, actualización, respaldo y recuperación del runtime local
  obligatorio para POS offline cuando esa capacidad entre en la distribución;
- componentes obligatorios/opcionales y perfiles de instalación;
- generación verificable del catálogo instalable desde descriptores y POM reales,
  resolución de versiones y estrategia de artefactos por composición;
- adquisición del proyecto o imágenes;
- formato del manifiesto y códigos de salida;
- firma Authenticode, custodia del certificado y publicación;
- estrategia de actualización, reparación y desinstalación.

Estas decisiones no impiden agregar la épica al plan, pero sí bloquean comenzar su
implementación de forma improvisada.
