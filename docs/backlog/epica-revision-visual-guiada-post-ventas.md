# Épica — Revisión visual guiada post-Ventas

- Estado: Planificada por decisión de producto
- Fecha de incorporación: 2026-08-15
- Momento: Sprint inmediatamente posterior al cierre técnico de `sales`
- Numeración tentativa: Sprint 12, a confirmar cuando se planifique `sales`
- Precedencia: después de `sales` y antes de iniciar `logistics`
- Tipo: Sprint transversal de revisión, explicación, ajuste y aceptación visual
- Roadmap: [plugins productivos](epica-roadmap-plugins-productivos.md)

## Objetivo

Recorrer con el responsable de producto cada pantalla real disponible al terminar
el plugin de Ventas, explicar cómo funciona, responder sus consultas e implementar
los ajustes que indique. El proceso se repite pantalla por pantalla hasta terminar
el recorrido completo y registrar su aceptación.

La expresión **captura real** significa una imagen obtenida de la aplicación
Jakarta Faces ejecutándose con el baseline candidato y datos ficticios
controlados. No se acepta un mock, una presentación, un diseño aislado ni una
imagen creada únicamente para aparentar una capacidad inexistente.

## Criterios de entrada

- `sales` está implementado y sus gates automatizados aplicables están verdes;
- la distribución física con Ventas está construida y desplegada mediante la
  infraestructura oficial;
- aplicación, identidad, base de datos y migraciones están saludables;
- existe un fixture reproducible con datos ficticios y sin secretos;
- las rutas, menús, permisos y pantallas efectivamente compuestos pueden
  inventariarse desde el baseline real;
- cualquier validación independiente anterior aún pendiente está identificada y
  no se presenta como completada.

## Taller visual en caliente

RV-00 preparará un modo de desarrollo local para que el responsable de producto
mantenga abierta la pantalla que está revisando y vea el resultado después de
guardar cada cambio, sin reiniciar manualmente toda la plataforma ni repetir el
recorrido desde el inicio. El objetivo de retroalimentación será:

| Tipo de cambio | Mecanismo y objetivo de retroalimentación |
|---|---|
| XHTML, CSS, tokens, distribución o responsive propiedad del shell | actualización controlada y recarga automática de la pantalla en hasta 5 segundos |
| Java, handler, permiso o contrato de pantalla | compilación incremental con el Wrapper, redespliegue automático y recarga después del health check, con objetivo de hasta 60 segundos |
| dominio, migración, dependencia o composición física de plugins | flujo normal de materialización, pruebas, reconstrucción y despliegue; no se modifica en caliente |

El taller conservará la ruta, el ancho y, cuando sea seguro, el borrador de la
pantalla. Si un redespliegue invalida la sesión o el estado, restaurará el fixture
y la ruta reproducible antes de presentar el resultado. Un error de compilación o
un health check fallido mantendrá visible la última candidata saludable e
informará el fallo; nunca presentará una aplicación rota como ajuste terminado.

Este modo será exclusivo del ambiente local de desarrollo, limitado a loopback y
sin secretos. El workspace y Git seguirán siendo la fuente de verdad: no se
editarán archivos construidos ni el interior de un contenedor. Tampoco habrá
carga dinámica, instalación en caliente ni reemplazo de JAR de plugins. Después
de aceptar cada corte, la evidencia y los gates se obtendrán de una imagen
inmutable reconstruida desde la materialización versionada, con el modo en
caliente ausente o desactivado en pruebas compartidas y producción.

## Alcance de pantallas

RV-00 congelará el inventario al entrar al Sprint. Incluirá todas las pantallas
navegables del shell y de los plugins físicamente presentes en el perfil de
demostración vigente al terminar Ventas, aunque una pantalla no haya cambiado en
ese plugin. No incluirá módulos meramente planificados, rutas técnicas sin tarea
de usuario ni pantallas de plugins ausentes.

El inventario registrará por pantalla:

- propietario, ruta, opción de menú y propósito;
- audiencia, permiso y precondiciones;
- estado inicial y estados alternativos que deben revisarse;
- recorrido anterior y siguiente dentro del flujo de trabajo;
- selectores, fuente de datos y ruta de administración;
- anchos y evidencias visuales requeridos;
- estado de revisión y decisión del responsable de producto.

## Método obligatorio, una pantalla por vez

1. **Preparar la pantalla real.** Levantar el baseline fijado, ingresar con un
   usuario ficticio autorizado y preparar el estado reproducible de la tarea.
2. **Tomar la captura.** Generar al menos una captura real en 1280 px y las
   variantes 720 y 375 px cuando el diseño cambie por rango. Agregar vacío,
   selección, error, denegación o confirmación cuando sean relevantes.
3. **Presentar una sola pantalla.** Mostrar la captura al responsable de producto
   antes de avanzar a otra pantalla.
4. **Explicar su funcionamiento.** Describir en lenguaje claro objetivo, acceso,
   datos, acciones, permisos, estados, validaciones, tablas afectadas, resultado
   esperado, errores y recuperación, responsive y accesibilidad.
5. **Resolver consultas.** Responder las dudas del responsable de producto y
   registrar las preguntas y respuestas que cambien o aclaren el comportamiento
   esperado.
6. **Recibir ajustes.** El responsable de producto indicará qué debe cambiar. La
   decisión se registra con alcance, razón y criterio observable; no se infiere un
   rediseño no solicitado.
7. **Implementar y previsualizar el ajuste.** Realizar un cambio coherente y
   mostrarlo en la misma pantalla mediante actualización visual o redespliegue
   incremental automático, según su tipo. El responsable de producto puede
   seguir consultando y ajustando mientras se conserva la última candidata sana.
8. **Validar, reconstruir y fotografiar.** Actualizar pruebas y manuales,
   materializar el índice, ejecutar inmediatamente los gates proporcionales al
   riesgo y reconstruir la aplicación real. Conservar captura anterior/posterior
   de esa candidata reproducible para comparar el resultado.
9. **Solicitar aceptación de la pantalla.** Sólo se pasa a la siguiente cuando el
   responsable de producto la declare aceptada o difiera expresamente un punto con
   razón, responsable y destino acordados.

Si una solicitud cambia arquitectura, seguridad, propiedad de datos,
compatibilidad o alcance de un plugin, la pantalla se detiene y se crea la
decisión o historia necesaria antes de implementar. La conversación de revisión
no autoriza saltar esas fronteras.

## Estados de seguimiento

| Estado | Significado |
|---|---|
| Pendiente | todavía no se presentó la captura real |
| En revisión | la pantalla fue explicada y existen preguntas abiertas |
| Ajuste solicitado | producto definió un cambio todavía no implementado |
| Implementada pendiente de revisión | el ajuste está desplegado y espera nueva revisión |
| Aceptada por producto | captura, explicación y resultado fueron aprobados |
| Bloqueada | existe una decisión o dependencia explícita que impide continuar |

## Contenido mínimo de la explicación

Para que la revisión no se limite a apariencia, cada pantalla debe explicar:

- qué objetivo permite cumplir y qué queda fuera de alcance;
- quién puede verla y qué permisos exige cada acción;
- significado, origen, obligatoriedad y formato de cada campo;
- fuente y propietario de cada selector, incluidos inactivos y ruta Administrar;
- estados del recurso y acciones disponibles en cada estado;
- confirmaciones, concurrencia, idempotencia y datos técnicos ocultos;
- efectos de crear, modificar, confirmar, cancelar o consultar;
- tablas propias afectadas y contratos públicos usados, sin exponer secretos;
- mensajes, recuperación ante errores y canal de soporte;
- comportamiento con teclado, foco, lector de pantalla y movimiento reducido;
- adaptación compacta, media y expandida sin overflow horizontal normal.

## Historias del Sprint

| Orden | Historia | Resultado esperado |
|---:|---|---|
| 1 | RV-00 | baseline, fixture, inventario y taller visual en caliente verificados; orden de recorrido aceptado |
| 2…N | RV-`nn` | una historia secuencial por cada pantalla inventariada |
| N+1 | RV-C01 | recorrido integral, regresión, evidencias y aceptación final |
| N+2 | RV-C02 | documentación de cierre, fotografía de plugins, PDF y decisión de instalador |

La cantidad y los identificadores finales se materializan en RV-00 porque
dependen de las pantallas realmente compuestas después de Ventas. No se fija hoy
un número inventado.

## Criterios de aceptación

- **RV-CE01:** RV-00 enumera todas y sólo las pantallas navegables del perfil real.
- **RV-CE02:** cada pantalla tiene capturas reales versionadas y reproducibles.
- **RV-CE03:** cada explicación cubre el contenido mínimo y define términos antes
  de usarlos.
- **RV-CE04:** preguntas, respuestas y decisiones relevantes quedan registradas.
- **RV-CE05:** cada ajuste implementado corresponde a una indicación explícita del
  responsable de producto y conserva trazabilidad antes/después.
- **RV-CE06:** cada cambio ejecuta pruebas mínimas, módulo, arquitectura,
  integración, seguridad y Playwright cuando sean aplicables.
- **RV-CE07:** 375, 720 y 1280 px, más 599/600/839/840, conservan operación,
  accesibilidad y ausencia de overflow horizontal normal.
- **RV-CE08:** empresa, autorización, plugin, estado y concurrencia se revalidan en
  servidor; ocultar una acción no sustituye seguridad.
- **RV-CE09:** manuales de usuario y desarrollador reflejan el comportamiento
  aceptado de cada pantalla.
- **RV-CE10:** ninguna pantalla avanza a `Aceptada por producto` sólo por tener
  pruebas automáticas verdes.
- **RV-CE11:** el recorrido final reproduce los flujos conectados entre pantallas
  sin mocks ni datos reales de clientes.
- **RV-CE12:** `logistics` no inicia hasta que todas las pantallas estén aceptadas
  o exista una decisión expresa de producto que reprograme un pendiente.
- **RV-CE13:** un cambio puramente visual aparece automáticamente en la pantalla
  abierta en hasta 5 segundos; un cambio Java elegible se presenta después de
  compilación, redespliegue y salud verde, con objetivo de hasta 60 segundos.
- **RV-CE14:** un fallo conserva la última candidata saludable, muestra el estado
  del taller y no borra silenciosamente una sesión o un borrador seguro.
- **RV-CE15:** el modo en caliente funciona sólo en desarrollo local, no introduce
  carga dinámica de plugins y no forma parte de la imagen final aceptada.
- **RV-CE16:** las capturas de aceptación, pruebas y cierre provienen de la imagen
  inmutable reconstruida, no de un overlay o artefacto temporal del taller.

## Evidencia por pantalla

Cada RV-`nn` conservará:

- baseline, digest, fecha, ambiente, ruta y rol ficticio;
- captura inicial y capturas responsive/estados pertinentes;
- explicación presentada;
- preguntas y respuestas;
- ajustes solicitados y criterio observable;
- archivos modificados y pruebas ejecutadas;
- captura posterior y comparación;
- decisión final del responsable de producto.

Las imágenes se guardarán bajo
`docs/evidence/screenshots/<historia>/` y la evidencia textual bajo
`docs/evidence/`. No contendrán contraseñas, tokens ni datos personales reales.

## Condición de salida

El Sprint termina únicamente cuando el inventario completo fue recorrido en su
orden, cada pantalla quedó `Aceptada por producto`, el recorrido integral y los
gates automatizados están verdes, los manuales coinciden con la aplicación real y
se completaron los entregables obligatorios de cierre. Las dudas sin respuesta o
los ajustes indicados pero no revisados mantienen abierto el Sprint.

Antes del cierre se desactiva el taller en caliente, se reconstruye la distribución
desde el corte versionado y se repite el recorrido relevante contra esa imagen. La
rapidez de la conversación visual no sustituye compilación limpia, pruebas,
seguridad, evidencia ni reproducibilidad.

Al cierre se aplican además la demo visual, fotografía de plugins, PDF verificado
y pregunta explícita sobre instalador Windows exigidos por el repositorio.
