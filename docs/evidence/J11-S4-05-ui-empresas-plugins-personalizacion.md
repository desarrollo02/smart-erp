# Evidencia de J11-S4-05 — UI de empresas, plugins y personalización

- Fecha: 2026-07-28
- Estado: implementada pendiente de pruebas
- Historia: [J11-S4-05](../sprints/sprint-04/J11-S4-05-ui-empresas-plugins-personalizacion.md)

## Resultado

Se incorporaron las primeras operaciones visuales del panel administrativo:

- alta de empresa inactiva con personalización física obligatoria y libre;
- activación e inactivación con versión optimista;
- consulta de catálogo físico validado y de sólo lectura;
- consulta y cambio de estado deseado de plugins funcionales por empresa;
- reemplazo confirmado de la personalización exclusiva;
- estado efectivo, diagnósticos acotados y recuperación ante conflicto.

La UI usa Jakarta Faces y Material Design 3 responsive. No agrega REST
administrativo, carga dinámica de JAR, edición de manifests, SQL directo ni borrado
de datos.

## Frontera y seguridad

`CompanyAdministrationPort` es el contrato neutral que consume `web-shell`.
`TransactionalCompanyUseCases` lo implementa con JTA y delega las reglas en
`CompanyAdministrationService`, `PluginActivationService` y
`CompanyPluginResolver`. `CompanyAdministrationQueryService` devuelve proyecciones
inmutables; los beans web no reciben entidades JPA.

El filtro exige `kernel.company.manage` para `companies.xhtml` y
`kernel.plugin.manage` para `plugins.xhtml`. Las acciones vuelven a resolver el
permiso exacto. Reemplazar personalización exige `kernel.company.manage` aunque el
actor ya tenga `kernel.plugin.manage`.

Los IDs, versiones y selecciones enviados por JSF son candidatos no confiables. Se
parsean estrictamente y los casos de uso releen el estado. Inactivar empresa,
deshabilitar plugin y reemplazar personalización requieren confirmación explícita.

`CompanyAuditContext` distingue operaciones de sistema y de usuario autenticado.
Las acciones web registran el `AppUserId` local y la correlación generada por el
servidor; no incluyen token, cookie, issuer/subject, claims completos ni nombre.

## Compilación y empaquetado

La primera compilación se detuvo antes del código porque la consola heredó Java 8.
Se repitió con el JDK 21.0.11+10 ya validado en `.tools/jdk/`:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk/jdk-21.0.11+10').Path
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl kernel-infrastructure-jakarta -am compile -DskipTests
.\mvnw.cmd -pl web-shell -am compile -DskipTests
```

Ambos cortes terminaron `BUILD SUCCESS`. No ejecutaron pruebas.

El primer intento de omitir también la compilación de tests usó
`-Dmaven.test.skip=true`, pero PowerShell/Wrapper entregó `.test.skip=true` como una
fase inválida; el reactor no se inició. Se utilizó la bandera ya compatible:

```powershell
.\mvnw.cmd -Pwith-screen-customization-plugins `
  -pl distribution/logixone-war -am package -DskipTests
```

Resultado final: `BUILD SUCCESS`, doce de doce módulos. Las fuentes de test
compilaron y Surefire informó `Tests are skipped`; ninguna prueba fue ejecutada.

Artefacto:

- ruta: `distribution/logixone-war/target/logixone.war`;
- tamaño: `508092` bytes;
- SHA-256: `0A70120102F1CFBD88036D6DD8BCE960764F79A803AA8282ED9393882F8A08C8`;
- páginas incluidas: `admin/index.xhtml`, `admin/companies.xhtml` y
  `admin/plugins.xhtml`.

## Revisión estática

- XHTML administrativos mal formados: `0`;
- referencias JPA, infraestructura o plugins concretos desde `web-shell`: `0`;
- imports Jakarta en `plugin-api`, `kernel-api`, `kernel-domain` o
  `kernel-application`: `0`;
- imports `javax.*` introducidos en aplicación/web: `0`;
- las tres páginas administrativas quedaron dentro del WAR;
- no se modificaron V1–V4 ni se agregó una migración.

## Pruebas pendientes

No se ejecutaron JUnit, ArchUnit, PostgreSQL/Testcontainers, JPA/JTA runtime,
OIDC/WildFly, Docker/Compose, seguridad negativa ni Playwright. CA-01 a CA-14 y los
gates G2–G7 permanecen pendientes para `J11-S4-08`.

La compilación, el test-compile y el empaquetado no prueban comportamiento runtime,
responsividad real ni autorización negativa. Esta pantalla todavía no se anuncia
como demo administrativa validada.

## Siguiente paso

`J11-S4-06`: UI de usuarios, membresías, roles y permisos empresariales, reutilizando
la misma frontera global y reautorizando cada comando.
