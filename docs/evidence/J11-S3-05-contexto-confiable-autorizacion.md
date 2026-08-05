# Evidencia J11-S3-05 — Contexto confiable, autorización y auditoría

- Fecha: 2026-07-28
- Estado: Completada; G2/G4 verdes en [J11-S3-08](J11-S3-08-validacion-demo-cierre.md)
- Política aplicada: no se ejecutaron pruebas automatizadas antes de la demo visual

## Resultado

Se conectó el principal OIDC validado por WildFly con el usuario local, la empresa
autorizada y la composición efectiva de plugins. La sesión conserva una referencia
mínima y ninguna decisión de acceso se reutiliza sin consultar el estado actual.

## Artefactos implementados

### Contratos y aplicación neutrales

- `kernel-api/.../CompanySessionReference.java`: referencia histórica de actor/empresa.
- `kernel-application/.../security/access/`: resultados tipados, códigos cerrados y
  `TrustedAccessService`.
- `kernel-application/.../security/audit/`: evento, operación y resultado de acceso.
- `TrustedAccessPort` y `AccessAuditPort`: límites neutrales para infraestructura.

### Adaptadores Jakarta

- `TransactionalTrustedAccess`: límite JTA que ensambla consultas actuales de
  seguridad y composición empresarial.
- `StructuredAccessAudit`: evento estructurado sin tokens, cookies ni claims.
- `ValidatedOidcPrincipal`: acepta solo `Principal` con `authType=OIDC` y reutiliza el
  issuer configurado para WildFly.
- `TrustedCompanySession`: `AppUserId`, `CompanyId` y revisión; no cachea autoridad.
- `TrustedWebAccess`: selección, revalidación y guarda de plugin/permiso.
- `TrustedWebAccessExceptionMapper`: respuestas públicas `401`/`403` genéricas.
- `TrustedContextResource`: `GET /api/company-context`, respuesta exitosa `204`.

`web-shell/pom.xml` declara directamente sus dependencias internas hacia contratos que
importa. No se agregó biblioteca externa ni cambió una versión o licencia.

## Propiedades de seguridad observables en código

- La identidad se forma solo desde el principal OIDC del contenedor.
- Ningún header, query, JSON, cookie o campo oculto proporciona actor o empresa.
- Un candidato de empresa se analiza como UUID canónico y luego se contrasta con
  membresías y empresas operacionales actuales.
- Cero membresías y toda denegación devuelven una colección de opciones vacía.
- Una empresa se selecciona automáticamente; varias requieren elección explícita.
- La autorización vuelve a leer usuario, membresía, rol, asignación y concesión.
- La composición vuelve inefectivo el permiso cuando su plugin no está disponible.
- Se comprueba que el permiso requerido pertenece al plugin requerido.
- Cambiar de empresa limpia el contexto anterior antes de validar el nuevo.
- La correlación se genera en el servidor y los diagnósticos detallados no salen al
  navegador.

## Compilación y empaquetado

JDK usado: `.tools/jdk/jdk-21.0.11+10`.

Primer corte, frontera web:

```text
mvnw.cmd -B -DskipTests -pl web-shell -am package
```

Resultado: seis proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `24.546 s`; las fuentes
de producción y prueba compilaron y Surefire informó `Tests are skipped`.

Corte integrado:

```text
mvnw.cmd -B -DskipTests \
  -pl kernel-infrastructure-jakarta,web-shell,distribution/logixone-war \
  -am package
```

Resultado: nueve proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `15.435 s`; WAR generado
en `distribution/logixone-war/target/logixone.war`; pruebas omitidas explícitamente.

Composición destinada a la demo:

```text
mvnw.cmd -B -DskipTests -Pwith-screen-customization-plugins \
  -pl distribution/logixone-war -am package
```

Resultado: doce proyectos `SUCCESS`, `BUILD SUCCESS`, Maven `5.300 s`; además de los
módulos base se ensamblaron `reference-plugin`, `reference-customization-a` y
`reference-customization-b`. Surefire volvió a informar `Tests are skipped`.

## Inspección estática

`jar tf` confirmó en el WAR:

```text
WEB-INF/lib/kernel-api-0.1.0-SNAPSHOT.jar
WEB-INF/lib/kernel-application-0.1.0-SNAPSHOT.jar
WEB-INF/lib/kernel-infrastructure-jakarta-0.1.0-SNAPSHOT.jar
WEB-INF/lib/web-shell-0.1.0-SNAPSHOT.jar
WEB-INF/web.xml
```

La primera inspección correspondió a la variante base sin plugins. Después, la
inspección del perfil de demo confirmó además exactamente:

```text
WEB-INF/lib/reference-plugin-0.1.0-SNAPSHOT.jar
WEB-INF/lib/reference-customization-a-0.1.0-SNAPSHOT.jar
WEB-INF/lib/reference-customization-b-0.1.0-SNAPSHOT.jar
```

La búsqueda en el nuevo límite web no encontró `getHeader`, `getParameter`,
`getCookies`, lectura de `Authorization`, access token o refresh token. La única
aparición `javax.*` en los módulos inspeccionados sigue siendo el tipo Java SE
preexistente `javax.sql.DataSource`; esta historia no agregó una API Jakarta antigua.

La raíz entregada no contiene metadatos `.git`; por ello no fue posible producir un
`git status`. Los artefactos se inventariaron por rutas y contenido del workspace.

### Gate documental G0

Después de actualizar historia, arquitectura, estrategia, guía, runbook, evidencia e
índices se decodificaron todos los Markdown como UTF-8 estricto, se buscaron
caracteres de reemplazo y se resolvieron los enlaces locales desde su documento de
origen:

```text
MARKDOWN_FILES=90 LOCAL_LINKS=263 BROKEN_LINKS=0 BAD_ENCODING=0
```

Resultado: G0 correcto para el alcance de `J11-S3-05`.

## Trazabilidad

| CA | Evidencia candidata | Pendiente en S3-08 |
|---|---|---|
| CA-01/02 | principal OIDC exclusivo y usuario activo consultado | login e identidades negativas reales |
| CA-03/04/05 | resultados cerrados para cero/una/múltiples empresas | matriz PostgreSQL/JTA + navegador |
| CA-06 | candidato validado server-side y sesión limpiada antes del cambio | manipulación HTTP real |
| CA-07 | guarda relee todos los niveles y valida propietario | unitarias/integración acumuladas |
| CA-08/09 | sesión mínima sin vistas/roles y revalidación por llamada | cambio y revocación concurrente runtime |
| CA-10 | permiso se cruza con plugins efectivos | desactivación real durante sesión |
| CA-11/12 | evento estructurado con IDs técnicos y correlación | inspección de logs runtime |
| CA-13 | mapper público genérico `401`/`403` | REST Assured y browser |
| CA-14 | matriz detallada en historia y runbook | ejecución y evidencia final |

## Documentación actualizada

- historia y estado del Sprint 3;
- arquitectura versión 12;
- estrategia de pruebas versión 9;
- guía de implementación `1.0-rc7`;
- runbook de contexto confiable y autorización;
- índices documentales y esta evidencia.

No se regeneró el PDF porque Sprint 3 no está cerrado. Su regeneración, renderizado,
revisión visual, extracción y checksum permanecen obligatorios en `J11-S3-08`.

## Conclusión

La candidata cumple el empaquetado permitido y la inspección estática del alcance. No
se declara ninguna prueba automatizada, G2 ni G4 como verde. El estado correcto es
`Implementada pendiente de validación`; el siguiente trabajo autorizado es
`J11-S3-06`.
