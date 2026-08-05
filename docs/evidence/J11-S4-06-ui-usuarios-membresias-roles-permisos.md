# Evidencia de J11-S4-06 — UI de usuarios, membresías, roles y permisos

- Fecha: 2026-07-28
- Estado: implementada pendiente de pruebas
- Historia: [J11-S4-06](../sprints/sprint-04/J11-S4-06-ui-usuarios-membresias-roles-permisos.md)

## Resultado

Se incorporaron dos superficies administrativas Jakarta Faces, Material Design 3
y responsive:

- `/admin/security.xhtml`, para usuarios locales, membresías, roles y permisos
  funcionales propios de una empresa;
- `/admin/system-authority.xhtml`, para roles, asignaciones y permisos globales de
  la instancia.

La primera usa `kernel.security.manage`; la segunda,
`kernel.system_administration.manage`. La separación no es solamente visual: los
casos de uso, puertos, comandos y proyecciones son distintos. Los roles
empresariales incluyen `CompanyId`; los roles globales no lo incluyen.

## Frontera y seguridad

`BusinessSecurityAdministrationPort` y `SystemAuthorityAdministrationPort` son
contratos neutrales consumidos por `web-shell`. Sus implementaciones JTA consultan
proyecciones inmutables y delegan las mutaciones a servicios de aplicación y
dominio. Los beans JSF no reciben entidades JPA ni importan infraestructura.

El filtro exige el permiso exacto al abrir cada ruta. Cada acción lo vuelve a
exigir, parsea estrictamente IDs, versiones, códigos y permisos, relee estado dentro
de la transacción y audita el `AppUserId` del actor con una correlación generada por
el servidor.

El alta local de usuario combina el subject ingresado con el issuer OIDC ya
validado/configurado; el navegador no puede elegir otro issuer. No se administran
contraseñas, MFA, realms, clientes ni roles internos de Keycloak.

Inactivar usuario, membresía o rol; desasignar roles; y revocar permisos requieren
confirmación visible. Las operaciones reductoras de autoridad global reutilizan la
política y el lock transaccional que impiden dejar la instancia sin un
administrador global efectivo. No se agregó borrado genérico.

## Persistencia y composición

No se agregó migración. Las consultas reutilizan V3 para usuarios, membresías,
roles y permisos empresariales, y V4 para autoridad global. Se incorporaron
listados deterministas y eliminaciones exactas de asignaciones/grants en los
repositorios JPA existentes. V1–V4 permanecen inmutables.

Una concesión funcional nueva sólo acepta permisos de la composición efectiva
actual de la empresa. Una concesión histórica cuyo plugin ya no es efectivo
permanece visible, se marca como no efectiva y puede revocarse.

## Compilación y empaquetado

Se reutilizó el JDK 21.0.11+10 validado bajo `.tools/jdk/`:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk/jdk-21.0.11+10').Path
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl kernel-infrastructure-jakarta -am compile -DskipTests
.\mvnw.cmd -pl web-shell -am compile -DskipTests
.\mvnw.cmd -Pwith-screen-customization-plugins `
  -pl distribution/logixone-war -am package -DskipTests
```

Los tres cortes terminaron `BUILD SUCCESS`. El empaquetado procesó doce de doce
módulos, compiló seis fuentes de prueba de infraestructura y una fuente de prueba
web, y Surefire informó `Tests are skipped`; ninguna prueba fue ejecutada.

Artefacto:

- ruta: `distribution/logixone-war/target/logixone.war`;
- tamaño: `573226` bytes;
- SHA-256: `D1EB63AB001FFDDC7C21FE98E838FFF6BC723CADEE1814A132452B791B4B90F6`;
- páginas administrativas incluidas: `index.xhtml`, `companies.xhtml`,
  `plugins.xhtml`, `security.xhtml` y `system-authority.xhtml`.

## Revisión estática

- los dos XHTML nuevos se reabrieron como XML: `2/2` válidos;
- referencias JPA, infraestructura o plugins concretos desde Java de
  `web-shell`: `0`;
- imports Jakarta en `plugin-api`, `kernel-api`, `kernel-domain` o
  `kernel-application`: `0`;
- no se introdujeron imports `javax.*`; el único encontrado en el alcance ampliado
  sigue siendo el preexistente `javax.sql.DataSource` de Java SE;
- ambas páginas quedaron dentro del WAR;
- no se modificaron V1–V4 ni se agregó migración.

## Pruebas pendientes

No se ejecutaron JUnit, ArchUnit, PostgreSQL/Testcontainers, JPA/JTA runtime,
OIDC/WildFly, Docker/Compose, seguridad negativa ni Playwright. CA-01 a CA-16 y los
gates G2–G7 permanecen pendientes para `J11-S4-08`.

La compilación, el test-compile, el XML bien formado y el empaquetado no prueban el
comportamiento runtime, la protección real contra autobloqueo, la responsividad ni
la autorización negativa. Estas rutas todavía no se anuncian como demo
administrativa validada.

## Siguiente paso

`J11-S4-07`: consulta visual de auditoría y endurecimiento de la zona
administrativa, conservando la minimización de datos, la paginación y la
correlación segura.
