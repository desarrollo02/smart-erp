# J11-S3-07 — Render de pantalla y personalización A/B

- Estado: Completada
- Dependencia: `J11-S3-06` implementada para la candidata

## Objetivo

Renderizar en el shell una pantalla publicada por el plugin de referencia y aplicar visualmente, como última capa, la personalización exclusiva de cada empresa.

## Alcance

- adaptador UI desde `ComposedScreen` hacia componentes Jakarta Faces;
- renderer consistente con tokens y componentes Material Design 3 del shell;
- layout responsive para compacto, medio y expandido;
- registro cerrado de renderers por tipo público de elemento y fragmento;
- etiquetas, ayuda, visibilidad, habilitación, obligatoriedad, orden y slots permitidos;
- pantalla técnica de referencia sin datos de negocio productivos;
- resultados distintos para `reference_custom_a` y `reference_custom_b`;
- denegación completa ante composición inválida o plugin no efectivo;
- recursos y estilos aislados por contratos públicos;
- trazabilidad de pantalla, empresa y plugin sin datos sensibles.

## Fuera de alcance

- evaluar EL, clases, beans o rutas XHTML declaradas por plugins;
- permitir CSS/JavaScript global arbitrario;
- relajar validación, obligatoriedad o autorización del servidor;
- copiar una pantalla de facturación del legado;
- crear un motor visual genérico para todos los controles futuros.

## Criterios de aceptación

- **CA-01:** el renderer consume `ComposedScreen`, no entidades o internos del plugin.
- **CA-02:** solo tipos de elemento registrados por el shell pueden renderizarse.
- **CA-03:** una operación visual no autorizada produce denegación completa y no resultado parcial.
- **CA-04:** la definición funcional se aplica antes del único overlay empresarial.
- **CA-05:** empresa A muestra únicamente cambios y fragmentos A.
- **CA-06:** empresa B muestra únicamente cambios y fragmentos B.
- **CA-07:** cambiar entre A y B no conserva estado visual o datos de la empresa anterior.
- **CA-08:** un link directo vuelve a validar membresía, plugin, permiso y pantalla.
- **CA-09:** `HIDE` o `DISABLE` no evita que el servidor proteja la operación subyacente.
- **CA-10:** no aparecen rutas, EL, beans, CSS global o JavaScript arbitrario en el contrato.
- **CA-11:** pantalla ausente, versión incompatible o slot inválido fallan de forma segura.
- **CA-12:** la pantalla explica que es una demostración técnica y no facturación terminada.
- **CA-13:** la guía documenta cómo un implementador conecta un renderer permitido sin romper aislamiento.
- **CA-14:** los escenarios visuales A/B y negativos quedan preparados para Playwright.
- **CA-15:** cuando shell, pantalla A/B, login y URL estén observables, se avisa inmediatamente al responsable aunque la validación acumulada siga pendiente.
- **CA-16:** la pantalla estándar y las variantes A/B usan roles Material 3 centralizados sin transportar clases CSS en `plugin-api`.
- **CA-17:** la pantalla estándar y cada variante A/B se adaptan a `0–599px`, `600–839px` y `840px` o más sin overflow horizontal de página.
- **CA-18:** una personalización no puede inyectar XHTML, CSS o JavaScript para eludir el renderer JSF ni el tema del shell.

## Gates

- G1: pantalla visible en la candidata, sin declarar pruebas completas.
- G2/G6 diferidos: compositor/renderer y Playwright A/B en compacto, medio y expandido en `J11-S3-08`.
- G0 documental inmediato.

## Estado provisional aplicado

Se usó `Implementada pendiente de validación` hasta superar G2 y G6.

## Resultado implementado

`plugin-api` declara ahora el tipo visual neutral `ScreenElementType` con el conjunto
cerrado `DISPLAY_TEXT`, `TEXT_INPUT` y `ACTION`. El contrato no transporta clases,
componentes JSF, XHTML, CSS o JavaScript. El plugin funcional de referencia asigna
explícitamente esos tipos a `greeting`, `summary` y `refresh`; los overlays A/B
continúan modificando solamente propiedades previamente autorizadas.

La frontera confiable agrega `TrustedScreenAccess`. Una petición de pantalla:

1. vuelve a validar actor, sesión, empresa, plugin y permiso;
2. exige que `ScreenId` pertenezca al plugin autorizado;
3. recompone desde plugins actualmente efectivos;
4. rechaza atómicamente una composición inválida o una pantalla ausente;
5. registra `RESOLVE_SCREEN` con empresa, plugin, permiso y pantalla, sin datos
   personales o secretos.

`ShellScreenRegistry` relaciona la ruta pública `/reference` con
`reference_plugin:dashboard` y admite únicamente los tipos, regiones, slot, textos y
fragmentos conocidos. Una clave, región, tipo o fragmento sin renderer produce
denegación completa. El registro nunca importa clases de los tres plugins de
referencia.

`view.xhtml` consume exclusivamente el modelo producido desde `ComposedScreen`:

- A presenta `summary` primero, lo renombra como referencia tributaria, agrega ayuda,
  lo vuelve obligatorio e inserta `reference_custom_a:tax_notice`;
- B oculta `summary`, deshabilita `refresh` e inserta
  `reference_custom_b:company_notice`;
- ambas muestran el ID/versión del contrato y una leyenda inequívoca de demostración
  técnica, sin simular facturación;
- CSS Material 3 adapta encabezado, cards, formulario, fragmentos y acciones a los
  rangos compacto, medio y expandido.

Las variantes con y sin plugins empaquetaron inicialmente con JDK 21 y
`-DskipTests`. Ese resultado provisional fue reemplazado por el gate verificado de
`J11-S3-08`: Maven, WildFly/Keycloak, revisión visual y Playwright quedaron verdes y
la demo resultó accesible.

## Validación acumulada

`J11-S3-08` comprobó las variantes A/B, cambio sin filtración residual y viewports
`375px`, `720px` y `1280px` sin overflow horizontal. Las cuatro capturas finales se
revisaron visualmente. G2/G6 quedaron verdes. Evidencia:
[gates G2–G6](../../evidence/J11-S3-08-validacion-demo-cierre.md).

## Siguiente paso

La historia está completada. El Sprint continúa con G7 de `J11-S3-08`.
