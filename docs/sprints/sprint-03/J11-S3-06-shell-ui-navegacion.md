# J11-S3-06 — Shell UI y navegación por empresa

- Estado: Completada
- Dependencia: `J11-S3-05` implementada para la candidata

## Objetivo

Crear el primer shell navegable que muestre sesión, empresa activa y menús permitidos a partir de contribuciones ya filtradas por empresa y autorización.

## Alcance

- layout server-side con Jakarta Faces 4.1;
- decisión explícita sobre PrimeFaces u otra biblioteca antes de agregarla;
- páginas de acceso no habilitado, selección empresarial y shell;
- encabezado con usuario, empresa activa y logout;
- selector seguro para usuarios multiempresa;
- menú generado desde `CompanyContributions` y permisos efectivos;
- navegación estable mediante IDs/rutas públicas, no includes internos;
- Material Design 3 aplicado sobre JSF mediante tokens y estilos propios centralizados;
- layouts responsive compactos, medios y expandidos;
- estados de carga, vacío, denegación y error sin detalles sensibles.

## Fuera de alcance

- diseñar una SPA o almacenar tokens en el navegador;
- editor de menús, roles o usuarios;
- dashboard con métricas de negocio ficticias;
- incluir XHTML aportado mediante rutas arbitrarias de plugins;
- considerar un menú oculto como autorización.

## Criterios de aceptación

- **CA-01:** un visitante no autenticado no ve shell ni datos empresariales.
- **CA-02:** usuario sin membresía obtiene un estado claro sin enumeración.
- **CA-03:** usuario con una membresía llega a su empresa autorizada.
- **CA-04:** usuario multiempresa puede cambiar únicamente entre membresías activas.
- **CA-05:** encabezado muestra identidad de presentación y empresa sin usar esos textos como claves.
- **CA-06:** logout invalida sesión y limpia contexto empresarial.
- **CA-07:** el menú contiene solo plugins efectivos y permisos concedidos.
- **CA-08:** cambiar empresa recalcula el menú completo.
- **CA-09:** navegación usa contratos públicos y no importa controladores o vistas internas de plugins.
- **CA-10:** links directos vuelven a pasar por autorización aunque no aparezcan en el menú.
- **CA-11:** páginas de error no muestran stacktrace, token, claim, UUID ajeno ni SQL.
- **CA-12:** la UI es utilizable en compacto (`0–599px`), medio (`600–839px`) y expandido (`840px` o más), sin overflow horizontal normal y permite navegación por teclado.
- **CA-13:** si se agrega biblioteca visual, versión, licencia, necesidad y dependencia quedan centralizadas y documentadas.
- **CA-14:** no se presenta ninguna capacidad ERP ficticia como terminada.
- **CA-15:** los flujos Playwright quedan identificados para G6 acumulado.
- **CA-16:** Material Design 3 se aplica mediante tokens del shell sobre Jakarta Faces, sin agregar una biblioteca o permitir CSS/JavaScript arbitrario desde plugins.

## Gates

- G1: shell compilable/empaquetable y abrible como candidata visual.
- G6 diferido: Playwright, accesibilidad básica y navegación negativa en `J11-S3-08`.
- G0 documental inmediato.

## Estado provisional aplicado

Se usó `Implementada pendiente de validación` hasta superar G6.

## Resultado implementado

La candidata usa Jakarta Faces 4.1 provisto por WildFly. No se agregó PrimeFaces ni
otra biblioteca visual, por lo que esta historia no incorpora una versión, licencia o
superficie de mantenimiento adicional. [ADR-0007](../../adr/0007-material-design-responsive-sobre-jsf.md)
fija Material Design 3 como sistema de diseño y los rangos compacto, medio y
expandido como requisito de todas las pantallas.

El shell vive en `web-shell` como recursos Servlet 6.1 bajo
`META-INF/resources`. Incluye:

- entrada protegida `/app/index.xhtml` y welcome file hacia esa ruta;
- estado de selección para usuarios multiempresa;
- selección automática ya resuelta por `TrustedAccessService` para una empresa;
- encabezado con nombre de presentación local, empresa activa y logout OIDC;
- cambio de empresa que limpia y vuelve a validar el contexto;
- menú producido por plugins efectivos e intersectado con permisos vigentes;
- estado vacío cuando la sesión es válida pero no existe navegación autorizada;
- estados genéricos de denegación, error y preparación sin datos internos;
- tokens Material 3 para roles de color, forma, elevación y estados;
- layouts compacto, medio y expandido, foco visible, navegación por teclado, skip
  link y reducción de movimiento;
- ruta central `/app/view.xhtml?route=...`, sin incluir XHTML privado de plugins.

La proyección `TrustedNavigationView` se crea por request. Transporta únicamente el
nombre de presentación, opciones empresariales ya autorizadas y menús que conservan
su plugin propietario, permiso y ruta pública. No se guarda en sesión. Una ruta
directa debe existir en esa proyección actual y, si declara permiso, vuelve a ejecutar
`requireAuthorization(pluginId, permissionId)`.

Como `core.company` todavía no modela un nombre comercial, el shell muestra una
etiqueta técnica segura `Empresa · XXXXXXXX` derivada de una empresa ya autorizada.
No inventa un nombre empresarial ni usa la etiqueta para autorizar. Incorporar un
nombre comercial será una evolución de datos separada cuando exista el caso de uso.

La pantalla de la ruta autorizada declara expresamente que es una demostración
técnica. La representación de `ComposedScreen` y las diferencias visuales A/B siguen
perteneciendo a `J11-S3-07`.

## Matriz preparada para J11-S3-08

| Flujo | Resultado esperado |
|---|---|
| visitante sin login | OIDC intercepta antes del shell |
| usuario sin empresa | estado genérico de acceso no habilitado |
| una empresa | entrada directa al workspace |
| varias empresas | selector con solo opciones autorizadas |
| alterar UUID del selector | `403`, sin cambiar contexto |
| cambiar empresa válida | cabecera y menú se recomponen por request |
| permiso revocado | menú desaparece y el link directo se rechaza |
| plugin desactivado | no aporta menú ni ruta |
| ruta desconocida/manipulada | estado genérico sin stacktrace o IDs |
| logout | contexto limpiado y delegación a `/app/logout` OIDC |
| viewport escritorio/móvil | navegación y formularios utilizables |
| teclado/reduced motion | foco visible, skip link y transiciones anulables |

## Validación candidata ejecutada

Con JDK 21 y pruebas omitidas expresamente:

```powershell
.\mvnw.cmd -B -DskipTests -pl kernel-infrastructure-jakarta -am package
.\mvnw.cmd -B -DskipTests -Pwith-screen-customization-plugins `
  -pl web-shell,distribution/logixone-war -am package
.\mvnw.cmd -B -DskipTests -pl distribution/logixone-war -am package
```

Los tres cortes finalizaron con `BUILD SUCCESS`. El perfil visual ensambló doce
proyectos y la variante base nueve, retirando correctamente los tres JAR de referencia
cuando el perfil no estaba activo. Maven informó `Tests are skipped`.

`index.xhtml`, `view.xhtml` y `web.xml` se analizaron como XML correcto. El JAR de
`web-shell` contiene las dos vistas y `shell.css`; el WAR contiene el JAR y el
descriptor. Las fuentes no importan implementaciones de plugins ni contienen
PrimeFaces, `javax.faces`, tokens o una empresa aceptada desde headers/cookies.

Ese estado provisional fue superado por `J11-S3-08`: los cuatro secretos locales se
aprovisionaron fuera de versión, la imagen oficial se construyó en modo `verified` y
la URL Faces quedó recorrible. El aviso de demo se emitió después de comprobar login,
empresa, menú y pantalla, sin publicar credenciales.

## Hito de aviso de demo

Se notificará inmediatamente al responsable de producto cuando coincidan estas
condiciones observables:

1. Compose esté arrancado con una imagen de la candidata y secretos externos;
2. exista una URL local accesible;
3. una identidad ficticia pueda completar login;
4. el shell muestre empresa y menú autorizados;
5. la pantalla de `J11-S3-07` permita observar la personalización A/B.

Ese aviso identifica que la demo puede verse; no equivale a completar los gates de
`J11-S3-08`.

## Validación acumulada

`J11-S3-08` arrancó la URL Faces real y Playwright comprobó acceso protegido,
selector, denegación, navegación, cambio de empresa y logout coordinado. G6 quedó
verde. Evidencia: [gates G2–G6](../../evidence/J11-S3-08-validacion-demo-cierre.md).

## Siguiente paso

La historia está completada. El Sprint continúa con G7 de `J11-S3-08`.
