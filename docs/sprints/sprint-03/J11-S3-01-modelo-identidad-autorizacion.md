# J11-S3-01 — Modelo neutral de identidad y autorización

- Estado: Completada
- Dependencia: `J11-S3-00` completada

## Objetivo

Materializar en Java puro los tipos, estados, puertos y políticas deterministas de usuario, identidad externa, membresía, rol y permiso aprobados en ADR-0006.

## Alcance

- `AppUserId` UUID e `ExternalIdentity` basada en issuer y subject normalizados;
- estados de usuario, membresía y rol cerrados por defecto;
- identidad, membresía, rol, permiso y asignaciones como valores inmutables;
- políticas puras para cero/una/múltiples membresías;
- cálculo de permisos concedidos sin consultar Jakarta, JPA o Keycloak;
- contratos de actor autenticado y contexto empresarial resuelto;
- puertos de aplicación para lectura y mutación futura;
- diagnósticos estables sin filtrar existencia de otras empresas.

## Fuera de alcance

- claims JWT, `SecurityContext`, sesión HTTP o configuración OIDC;
- entidades JPA, SQL, Flyway o transacciones;
- endpoints, XHTML o componentes visuales;
- almacenar passwords o tokens;
- otorgar permisos desde roles de Keycloak.

## Criterios de aceptación

- **CA-01:** `kernel-api` publica únicamente los identificadores y el contexto mínimo que necesitan consumidores autorizados.
- **CA-02:** issuer y subject vacíos, mal normalizados o excesivos se rechazan.
- **CA-03:** correo, username y nombre visible no participan en igualdad de identidad.
- **CA-04:** estados desconocidos o transiciones prohibidas fallan de forma tipada.
- **CA-05:** una membresía siempre referencia usuario y empresa sin relación JPA.
- **CA-06:** roles empresariales no pueden asignarse a una membresía de otra empresa.
- **CA-07:** códigos de permiso reutilizan el contrato público vigente y no importan implementaciones de plugins.
- **CA-08:** cero membresías deniega, una permite selección determinista y múltiples exigen elección explícita.
- **CA-09:** seleccionar una empresa no autorizada produce una denegación genérica.
- **CA-10:** permisos efectivos se calculan por roles vigentes y pueden intersectarse después con plugins efectivos.
- **CA-11:** las colecciones expuestas son inmutables y deterministas.
- **CA-12:** ningún módulo neutral importa `jakarta.*`, Keycloak, JWT, HTTP, JPA, Faces o PrimeFaces.
- **CA-13:** los nuevos diagnósticos y contratos quedan documentados en arquitectura y guía.
- **CA-14:** las pruebas unitarias previstas cubren positivos, negativos, inmutabilidad y aislamiento, aunque su ejecución se difiera a `J11-S3-08`.

## Gates

- G1: compilación o empaquetado necesario para integrar la candidata, registrado sin afirmar pruebas verdes.
- G2 diferido: unitarias y ArchUnit se ejecutan acumuladas en `J11-S3-08`.
- G0 documental después de cada actualización.

## Estado provisional aplicado

Al terminar código se usó `Implementada pendiente de validación`; no se marcó
`Completada` antes de G2.

## Resultado provisional

El modelo quedó materializado en 24 clases Java puras:

- `kernel-api`: `AppUserId`, `AuthenticatedActor` y `AuthenticatedCompanyContext`;
- `kernel-domain`: identidad OIDC canónica, usuario, membresía, rol, asignación, concesión, diagnósticos y políticas de selección/permisos;
- `kernel-application`: puertos de generación de ID, usuario, membresía y autorización empresarial.

`CompanyAccessPolicy` resuelve cero, una o varias membresías y nunca acepta una empresa solicitada sin membresía activa. `EffectivePermissionPolicy` falla cerrado ante cruces de usuario, empresa o rol e intersecta las concesiones persistidas con los permisos actualmente disponibles desde plugins efectivos.

En ese corte los módulos se empaquetaron con Java 21 y `-DskipTests`; Surefire
registró las pruebas como omitidas. Todavía no se habían ejecutado JUnit ni ArchUnit,
por lo que la historia conservó temporalmente ese estado intermedio.

## Validación acumulada

`J11-S3-08` ejecutó unitarias, casos de revocación y ArchUnit dentro del gate de 145
pruebas sin fallos ni omisiones. G2 quedó verde y reemplaza el estado provisional
anterior. Evidencia: [gates G2–G6](../../evidence/J11-S3-08-validacion-demo-cierre.md).

## Siguiente paso

La historia está completada. El Sprint continúa con G7 de `J11-S3-08`.
