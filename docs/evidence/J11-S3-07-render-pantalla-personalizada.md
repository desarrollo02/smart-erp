# Evidencia J11-S3-07 — Renderer JSF y personalización visual A/B

- Fecha: 2026-07-28
- Estado: Completada; runtime, revisión visual y pruebas verdes en
  [J11-S3-08](J11-S3-08-validacion-demo-cierre.md)
- Política aplicada: primero candidata visual; pruebas acumuladas después

## Resultado

Se materializó el primer renderer de `ComposedScreen` sobre Jakarta Faces y Material
Design 3. La ruta técnica `/reference` puede representar el mismo contrato funcional
con overlays diferentes para las personalizaciones A y B. No se agregó PrimeFaces,
JavaScript, recursos remotos ni un dominio ERP ficticio.

## Contrato neutral

`plugin-api` agrega `ScreenElementType` con tres valores cerrados:

- `DISPLAY_TEXT`;
- `TEXT_INPUT`;
- `ACTION`.

`ScreenElementDefinition` conserva el tipo y ofrece un constructor de compatibilidad
que trata definiciones anteriores como texto de presentación. El plugin funcional de
referencia declara explícitamente el tipo de sus tres elementos. El tipo viaja en
`ComposedScreenElement`; no contiene clases JSF, XHTML, CSS, JavaScript o nombres de
beans.

## Resolución confiable y auditoría

Se incorporaron:

- `TrustedScreenAccess`, resultado permitido o denegado sin pantalla parcial;
- `TrustedAccessPort.screen(...)` y su adaptador JTA;
- `TrustedAccessService.screen(...)`, que autoriza plugin/permiso, recompone la
  empresa y exige el `ScreenId` exacto;
- códigos cerrados `SCREEN_ACCESS_DENIED` y `SCREEN_COMPOSITION_INVALID`;
- operación de auditoría `RESOLVE_SCREEN` y campo público `screen_id`.

Una composición inválida, pantalla ausente, propietario distinto o permiso no vigente
termina en denegación genérica. El evento técnico conserva actor local, empresa,
plugin, permiso, pantalla y correlación; no agrega token, claim, cookie, issuer,
subject, nombre o secreto.

## Renderer JSF cerrado

`ShellScreenRegistry` conoce sólo:

- ruta `/reference` del plugin `reference_plugin`;
- pantalla `reference_plugin:dashboard`;
- regiones `main` y `actions`;
- slot `dashboard_extensions`;
- los tres tipos neutrales;
- textos públicos registrados;
- fragmentos `reference_custom_a:tax_notice` y
  `reference_custom_b:company_notice`.

Un tipo, texto, región, slot o fragmento desconocido rechaza la vista completa. El
registro produce `ShellScreenView`, `ShellScreenElementView` y
`ShellScreenFragmentView` request-scoped; no importa implementaciones de plugins.
`ShellViewBean` deja de usar el placeholder y solicita la pantalla únicamente después
de validar que la ruta pertenece al menú actual.

## Diferencia A/B materializada

- A reordena `summary` a la primera posición, lo renombra, agrega ayuda, lo vuelve
  obligatorio e inserta una tarjeta tributaria propia.
- B oculta `summary`, deshabilita `refresh` e inserta un aviso empresarial distinto.
- Ambas muestran ID/versión del contrato y una leyenda de demostración técnica.
- `shell.css` usa tokens Material 3 y adapta encabezado, cards, formulario, fragmentos
  y acciones a compacto, medio y expandido.
- Los modelos se reconstruyen por request; cambiar de empresa no conserva pantalla ni
  valores A/B como autorización cacheada.

## Empaquetados ejecutados

JDK: `.tools/jdk/jdk-21.0.11+10`.

### Distribución funcional con personalizaciones A/B

```text
mvnw.cmd -B -DskipTests -Pwith-screen-customization-plugins \
  -pl web-shell,distribution/logixone-war -am package
```

Resultado: doce proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `19.500 s`; fuentes y
tests se compilaron, Surefire informó `Tests are skipped`. El WAR incluyó el plugin
funcional y ambas personalizaciones.

Después de integrar el XHTML y la documentación finales se repitió el mismo
empaquetado A/B: doce proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `8.669 s` y pruebas
omitidas. Este segundo empaquetado es el que dejó el WAR candidato final en
`distribution/logixone-war/target/logixone.war`.

### Distribución base sin plugins

```text
mvnw.cmd -B -DskipTests -pl web-shell,distribution/logixone-war -am package
```

Resultado: nueve proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `8.552 s`; Maven retiró
los tres JAR de referencia del ensamblado base y Surefire informó
`Tests are skipped`.

## Validación estructural

- `index.xhtml`, `view.xhtml` y `web.xml` se abrieron con el parser XML sin errores;
- `shell.css` tiene 161 llaves de apertura y 161 de cierre;
- no existen imports desde `py.com.logixone.plugins.*` en kernel o shell;
- no se agregaron `javax.faces`, PrimeFaces, scripts o recursos HTTP externos;
- el WAR contiene `web-shell`, el plugin funcional y las personalizaciones A/B, y el
  JAR del shell contiene las clases del renderer, ambos XHTML y `shell.css`;
- el renderer usa tipos/IDs públicos y listas cerradas;
- el modelo request-scoped se prepara en `APPLY_REQUEST_VALUES` también en postback,
  antes de que una validación requerida pueda omitir `INVOKE_APPLICATION`.

## Diagnóstico de disponibilidad visual

Docker Engine `29.6.2` está accesible. Se inició únicamente PostgreSQL para leer el
estado técnico del volumen `logixone_postgres-data`: contiene Flyway V1 y todavía no
contiene `core.company`; luego se detuvo el contenedor sin eliminar el volumen.

La demo completa no se arrancó porque:

- faltan `keycloak-admin-password.txt` y `oidc-client-secret.txt` bajo
  `.tools/secrets/`;
- `compose.env.local` todavía corresponde al corte anterior y no declara OIDC;
- la imagen Keycloak fijada aún no está local;
- no existen V2/V3, empresas A/B, membresías, roles ni identidad ficticia;
- el Dockerfile oficial ejecuta `verify` y requiere un modo de candidata explícito
  antes de construir sin adelantar la matriz diferida.

No se creó una ruta alternativa, SQL manual o imagen no documentada para simular el
recorrido. Por ello todavía no se emitió el aviso de demo accesible.

## Matriz pendiente de J11-S3-08

- tipos/regiones/textos/slots/fragmentos desconocidos;
- composición inválida o pantalla ausente;
- pantalla de otro plugin y permiso revocado;
- A requerida/reordenada y B oculta/deshabilitada;
- cambio A↔B sin estado residual;
- teclado, foco, validación requerida y acción deshabilitada;
- `375px`, `720px`, `1280px` y límites `599/600/839/840px` sin overflow;
- login/logout, selector, rutas directas, health y persistencia.

## Gate documental G0

```text
MARKDOWN_FILES=94
LOCAL_LINKS=287
UTF8_FAILURES=0
BROKEN_LINKS=0
```

## Documentación actualizada

- historia y estado del Sprint 3;
- orden operativo de `J11-S3-08` para anunciar primero la candidata observable;
- arquitectura versión 15;
- estrategia de pruebas versión 12;
- guía de implementación `1.0-rc10`;
- runbook del shell versión 3 e índices documentales.

No se regeneró el PDF porque Sprint 3 continúa abierto.

## Conclusión

`J11-S3-07` queda `Implementada pendiente de validación`. El siguiente trabajo es
aprovisionar y arrancar la candidata en `J11-S3-08`; el aviso visual se emitirá apenas
login, shell y pantalla A/B sean realmente observables.
