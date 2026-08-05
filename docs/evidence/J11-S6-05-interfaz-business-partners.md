# Evidencia J11-S6-05 - Interfaz de `business_partners`

- Fecha: 2026-07-29
- Resultado: Verde
- Alcance: contrato neutral, renderer JSF, plugin, PostgreSQL, Docker y Playwright

## Capacidades verificadas

- menú y ruta `/business-partners` condicionados por plugin/permiso efectivos;
- pantalla `business_partners:directory` `1.0.0` compuesta por el shell;
- alta, búsqueda, tabla, detalle y rol cliente sobre datos PostgreSQL reales;
- reautorización de lectura, administración, roles y ciclo de vida por acción;
- selección con identidad/versión consistente entre postbacks request-scoped;
- aviso de éxito, versión optimista `0 → 1` y auditoría técnica;
- slots `directory_extensions` y `detail_extensions` sin markup ejecutable;
- Material Design 3, semántica y responsive sin overflow horizontal.

## Pruebas de módulo y arquitectura

```powershell
.\mvnw.cmd -B -Pwith-screen-customization-plugins verify
```

Resultado: 20 módulos verdes en 1 min 45 s; 241 pruebas, 0 fallos, 0 errores y 0
omitidas. Dentro del corte: `plugin-api` 19, `business-partners` 30, `web-shell` 17
y arquitectura 15 pruebas verdes.

El `mvnw.cmd -B verify` sin perfil también quedó verde en 20 módulos y 1 min 43 s.
El WAR base retiró los JAR de referencia generados por el build anterior y conservó
cero JAR físicos de `business_partners`, demostrando que la API 0.4.0 no rompe la
distribución sin el plugin.

La prueba PostgreSQL focalizada ejecutó `BusinessPartnerJpaRepositoryPostgreSqlIT`
con cumplimiento JPQL estricto y 10 escenarios verdes. Verificó consulta sin
filtros, filtros dinámicos, aislamiento empresarial, round-trip y concurrencia.

## Playwright productivo

```powershell
.\mvnw.cmd -B -pl tests/e2e-tests `
  '-Dlogixone.e2e=true' `
  '-Dlogixone.business-partners.e2e=true' `
  '-Dit.test=BusinessPartnersVisualIT' `
  '-Dlogixone.app-url=http://localhost:18080/logixone/faces/app/index.xhtml' `
  "-Dlogixone.demo-user-password-file=<ruta-absoluta-al-secreto>" `
  "-Dlogixone.evidence-dir=<ruta-absoluta-a-J11-S6-05\e2e>" `
  verify
```

Resultado: 1 escenario, 0 fallos, 0 errores y 0 omitidos; 14,87 s de prueba y
24,890 s de build. El recorrido creó un código aleatorio, buscó exactamente su
fila, abrió el detalle, asignó cliente y comprobó versión `1`.

Anchos validados: `375`, `599`, `600`, `720`, `839`, `840` y `1280` px. En todos,
`document.documentElement.scrollWidth <= viewport + 1`. Las hojas Material Design
del shell se cargaron y cada control editable conservó label asociado.

Durante la preparación aparecieron dos fallos reales del test y se bloquearon antes
de continuar: una ruta relativa al secreto desde el módulo y dos selectores
demasiado amplios. Se corrigieron usando ruta absoluta y selectors exactos/acotados
a la fila; no se omitió ni relajó ninguna aserción funcional.

## Docker y runtime

- imagen local: `logixone/app:j11-s6-05-ui-local`;
- manifest local: `sha256:53b493f58bbdf0f02718a0e94072da43d92751e6d97b2664c669e504cd883aa5`;
- `logixone-app-1`: saludable en `127.0.0.1:18080`;
- PostgreSQL y Keycloak: volúmenes existentes conservados;
- el recreado afectó sólo al contenedor de aplicación, no eliminó datos.
- `GET /logixone/health/live`: HTTP 200, `UP`;
- `GET /logixone/health/ready`: HTTP 200, `UP`.

La consulta de auditoría append-only confirmó cinco mutaciones del recorrido. Las
últimas filas incluyen `REGISTER_BUSINESS_PARTNER` con versión resultante `0` y
`ASSIGN_BUSINESS_PARTNER_ROLE` con `0 → 1`, permisos `manage`/`roles.manage` y
`resource_type=business_partner`. No se consultaron ni registraron valores
comerciales sensibles en esa evidencia.

La composición usada para esta historia fue efímera y local porque el perfil
físico oficial está reservado a `J11-S6-06`. No se promovió imagen ni se modificó
la composición versionada.

## Evidencia visual revisada

- [compacto 375 px](screenshots/J11-S6-05/e2e/business-partners-compact-375.png)
- [medio 720 px](screenshots/J11-S6-05/e2e/business-partners-medium-720.png)
- [expandido 1280 px](screenshots/J11-S6-05/e2e/business-partners-expanded-1280.png)

Las tres imágenes fueron abiertas y revisadas: no hay cortes, caracteres dañados,
campos sin jerarquía, acciones fuera del contenedor ni overflow horizontal normal.

| Captura | SHA-256 |
|---|---|
| compacto | `3348297cf05fe9c549482ffabef9561938bd0f8de0cd207006d5d0a1f55df0ef` |
| medio | `bf0053035ae2543611739adf5f443c2cc67fce571e17664528819992f5aee414` |
| expandido | `050bf7f0f676ed931c9a725cf8e7c3e43013bfc3651ebc42c3ce59b2ac8b3d6b` |

## Documentación

`tmp/validate_docs.py` revisó 165 archivos Markdown: 0 enlaces locales rotos, 0
errores UTF-8, 0 archivos con mojibake y 0 coincidencias con secretos locales.

## Alcance pendiente

- composición física reproducible WAR/migrator en `J11-S6-06`;
- matriz con plugin presente/ausente y conservación de datos en esa composición;
- validación integral, demo final, retrospectiva y PDF de Sprint 6 en `J11-S6-07`;
- validación independiente transversal de la guía candidata.
