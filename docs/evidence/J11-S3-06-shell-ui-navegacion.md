# Evidencia J11-S3-06 — Shell UI y navegación por empresa

- Fecha: 2026-07-28
- Estado: Completada; runtime y Playwright verdes en [J11-S3-08](J11-S3-08-validacion-demo-cierre.md)
- Política aplicada: no se ejecutaron pruebas automatizadas antes de la demo visual

## Resultado

Se implementó el primer shell server-side de Logixone con Jakarta Faces 4.1. El
resultado presenta selección empresarial, sesión, empresa activa, logout, menú
autorizado y estados seguros. No se agregó una biblioteca visual externa y no se
simuló un dominio ERP productivo.

La adenda de producto incorporó Material Design 3 como sistema visual sobre Jakarta
Faces y responsive obligatorio. Se registró
[ADR-0007](../adr/0007-material-design-responsive-sobre-jsf.md), se centralizaron
tokens `--md-sys-*` y se separaron los rangos compacto `0–599px`, medio `600–839px`
y expandido desde `840px`. Esto no incorpora una dependencia o licencia de software
adicional.

## Contratos y aplicación

`kernel-application.security.access` agrega:

- `TrustedCompanyOption`: empresa ya autorizada con etiqueta de presentación;
- `TrustedMenuItem`: menú con plugin propietario, ID, clave pública, ruta y permiso;
- `TrustedNavigationView`: proyección request-scoped del shell;
- `TrustedNavigationAccess`: resultado permitido o denegado cerrado;
- `TrustedAccessService.navigation`: relectura de usuario, membresía, empresa,
  composición y permisos antes de proyectar navegación.

`TrustedAccessPort` y `TransactionalTrustedAccess` publican/adaptan la consulta. La
auditoría incorpora `RESOLVE_NAVIGATION`. La proyección no se almacena en sesión.

## Frontera web

- `TrustedWebAccess.navigation()` exige primero contexto actual y limpia la sesión si
  la proyección deja de ser válida.
- `ShellViewBean` es request-scoped, maneja estados y protege la ruta directa.
- `ShellTextCatalog` traduce sólo claves públicas conocidas y usa fallback genérico.
- Los modelos JSF exponen getters de presentación, no entidades o DTO de plugins.
- `index.xhtml` implementa selección, workspace, cambio de empresa, logout y menú.
- `view.xhtml` exige una ruta presente en el menú actual y vuelve a autorizar permiso.
- `shell.css` usa recursos locales, tokens Material 3, tres rangos responsive, foco,
  estados interactivos y reduced motion.

`web.xml` configura Faces en `*.xhtml`, estado en servidor, cookie HTTP-only y welcome
file. Las restricciones OIDC de `/app/*` y `/api/*` permanecen vigentes.

## Dependencias y licencias

No se agregó PrimeFaces ni dependencia Maven. Jakarta Faces 4.1 ya forma parte del
baseline Jakarta EE 11 provisto por WildFly. No cambió POM/BOM ni existe una licencia
nueva que inventariar.

## Compilaciones ejecutadas

JDK: `.tools/jdk/jdk-21.0.11+10`.

### Proyección y adaptador

```text
mvnw.cmd -B -DskipTests -pl kernel-infrastructure-jakarta -am package
```

Resultado: siete proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `35.835 s`; pruebas
omitidas explícitamente.

### Perfil visual funcional más A/B

```text
mvnw.cmd -B -DskipTests -Pwith-screen-customization-plugins \
  -pl web-shell,distribution/logixone-war -am package
```

Resultado: doce proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `8.679 s`. El WAR incluyó
`reference-plugin`, `reference-customization-a` y `reference-customization-b`.

### Variante base sin plugins

```text
mvnw.cmd -B -DskipTests -pl distribution/logixone-war -am package
```

Resultado: nueve proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `8.187 s`. Maven retiró
los tres JAR de referencia del directorio ensamblado y el WAR final no contiene
ninguno. En los tres comandos Surefire informó `Tests are skipped`.

### Adenda Material Design 3 y responsive

El primer intento de empaquetado usó accidentalmente el JDK 8 global del equipo.
Maven Enforcer lo rechazó antes de compilar mediante `RequireJavaVersion`; no se
interpretó como resultado del código ni se relajó la regla. Se repitió con
`JAVA_HOME=.tools/jdk/jdk-21.0.11+10`:

```text
mvnw.cmd -B -DskipTests -pl web-shell,distribution/logixone-war -am package
```

Resultado: nueve proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `7.451 s`; pruebas
omitidas explícitamente. El JAR contiene las dos vistas y `shell.css`.

## Validación estructural

`index.xhtml`, `view.xhtml` y `WEB-INF/web.xml` se abrieron con el parser XML de
PowerShell sin errores. `jar tf` confirmó:

```text
META-INF/resources/app/index.xhtml
META-INF/resources/app/view.xhtml
META-INF/resources/resources/logixone/shell.css
WEB-INF/lib/web-shell-0.1.0-SNAPSHOT.jar
WEB-INF/web.xml
```

La inspección estática confirmó:

- cero imports de implementaciones `py.com.logixone.plugins.*` desde kernel/web;
- cero PrimeFaces o `javax.faces`;
- 107 referencias a tokens `--md-sys-*`, rangos CSS compacto/medio y cero recursos
  HTTPS cargados por `shell.css`;
- empresa del formulario convertida a UUID canónico y revalidada server-side;
- ruta directa limitada, comparada exactamente y ligada al menú actual;
- permisos requeridos vuelven a pasar por `requireAuthorization`;
- ninguna vista incluye token, claim, SQL, UUID ajeno o capacidad ERP ficticia.

### Gate documental G0

Después de actualizar todas las fuentes Markdown se aplicó decodificación UTF-8
estricta, detección de caracteres de reemplazo y resolución de enlaces locales desde
cada documento:

```text
MARKDOWN_FILES=93 LOCAL_LINKS=284 BROKEN_LINKS=0 BAD_ENCODING=0
```

Resultado: G0 correcto para `J11-S3-06`.

## Estado de runtime y aviso visual

Docker Engine 29.6.2 está disponible. No se inició Compose porque faltan los archivos
locales ignorados `keycloak-admin-password.txt` y `oidc-client-secret.txt`, no existe
todavía una identidad/dataset ficticio autorizado y el Dockerfile oficial ejecuta
`verify`, cuya ejecución fue diferida por decisión de producto.

No se creó una imagen alternativa que eludiera ese gate y no se presentará un XHTML
empaquetado como demo visible. El aviso solicitado se emitirá cuando una URL arrancada
permita login, shell y pantalla A/B; ese hito está registrado en Sprint 3 y
`J11-S3-07`.

## Matriz pendiente

`J11-S3-08` debe ejecutar JUnit/ArchUnit y Playwright para visitante, cero/una/varias
empresas, manipulación de selector/ruta, permiso revocado, plugin desactivado, menú
vacío, logout, teclado, viewports `375px`/`720px`/`1280px`, límites de breakpoint,
overflow horizontal y ausencia de detalles sensibles.

## Documentación actualizada

- historia y estado del Sprint 3;
- criterio de aviso en `J11-S3-07`;
- arquitectura versión 14;
- estrategia de pruebas versión 11;
- guía de implementación `1.0-rc9`;
- runbook del shell e índices documentales.

No se regeneró el PDF porque Sprint 3 continúa abierto.

## Conclusión

La historia queda `Implementada pendiente de validación`. El siguiente trabajo
autorizado es `J11-S3-07`; la demo visual aún no fue anunciada como disponible.
