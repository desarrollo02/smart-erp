# Evidencia J11-S9-06 — Integración y composición de `purchasing`

- Fecha: 2026-08-12
- Rama local: `sprint/09-purchasing`
- Estado: Implementada y validada automáticamente; validación independiente pendiente
- Corte reproducible completo: índice Git materializado en
  `.tools/tmp/validation/J11-S9-06-final`
- Corte reproducible de E2E final: índice Git materializado en
  `.tools/tmp/validation/J11-S9-06-final-e2e-3`
- URL local: `http://localhost:18080/logixone/`

## Baseline desplegado

| Artefacto | Etiqueta | Identidad local | Tamaño |
|---|---|---|---:|
| Aplicación | `logixone/app:j11-s9-06-purchasing-demo-r5` | `sha256:4e7e84da913b64ae08cdd72188640af5a023e824db67dfb0aecdc2d40c38fba8` | 501.507.736 bytes |
| Migrador | `logixone/migrator:j11-s9-06-purchasing-demo` | `sha256:7a03dca088e04b79b7e83c6568b982f2b5f728695ed3de800e1dbd8a0f4fcef8` | 105.812.331 bytes |

El contenedor `logixone-j11-s9-06-app-1` informó `healthy` con la etiqueta final.
La raíz pública `http://localhost:18080/logixone/` respondió `302` hacia la ruta
OIDC protegida `/logixone/faces/app/index.xhtml`; readiness respondió `200`.
PostgreSQL y Keycloak pertenecen al proyecto Compose aislado
`logixone-j11-s9-06`; no se usaron servicios del IDE ni instalaciones manuales
del usuario.

## Pruebas automatizadas verdes

| Gate | Resultado |
|---|---|
| Pruebas focales del shell | 8/8, sin fallos, errores ni omitidas |
| Reactor `-Pwith-purchasing-demo verify` | 28/28 módulos verdes |
| Suite unitaria acumulada materializada | 535/535 |
| Arquitectura y composición | 34/34 |
| PostgreSQL/Testcontainers de Compras | 7/7: 3 JPA y 4 migraciones |
| Runtime health/readiness | 2/2 |
| Runtime OIDC | 4/4 |
| Playwright Compras | 1 recorrido integral verde |
| Total de reportes JUnit materializados | 549 pruebas, 0 fallos, 0 errores, 0 omitidas |

Comandos principales:

```powershell
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-06-final\pom.xml -Pwith-purchasing-demo verify
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-06-final\pom.xml -pl plugins\purchasing -am "-Dlogixone.postgres.integration=true" verify
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-06-final\pom.xml -pl tests\integration-tests -am "-Dit.test=HealthEndpointsIT,OidcRuntimeIT" "-Dlogixone.base-uri=http://localhost:18080" "-Dlogixone.oidc-probe=true" verify
.\mvnw.cmd -f .tools\tmp\validation\J11-S9-06-final-e2e-3\pom.xml -Pvisual-e2e -pl tests\e2e-tests "-Dit.test=PurchasingVisualIT" "-Dlogixone.purchasing.e2e=true" verify
```

La prueba OIDC creó fixtures efímeros y los retiró al finalizar. Verificó token
válido, audiencia incorrecta, emisor incorrecto y token expirado. El gate JTA de
harness no se habilitó porque esa aplicación de prueba no forma parte de la imagen
productiva; la frontera JTA de Compras sí está cubierta por sus pruebas de módulo,
PostgreSQL y el flujo desplegado de confirmación de stock.

## Migraciones

La imagen final del migrador se ejecutó dos veces sobre la base aislada. Ambas
ejecuciones terminaron con `migrations_executed=0` y versiones esperadas:

- `core` V6;
- `plg_reference_data` V4;
- `plg_business_partners` V4;
- `plg_commercial_catalog` V4;
- `plg_inventory` V2;
- `plg_purchasing` V2;
- `plg_reference_plugin` V1.

Esto confirma validación de checksums e idempotencia sobre un esquema ya migrado.

## Recorrido E2E

El navegador automatizado usó `demo.empresas.ab` para operar y
`demo.empresa.a` como aprobador independiente en la misma empresa. El recorrido:

1. activó los cinco plugins requeridos y otorgó permisos de demostración;
2. creó proveedor, unidad, perfil tributario, producto, depósito y ubicación;
3. creó y envió una solicitud de diez unidades;
4. aprobó con una identidad diferente;
5. creó y emitió una orden;
6. recibió seis unidades en Inventario;
7. devolvió dos unidades al proveedor;
8. verificó `pedida 10`, `recibida 6`, `devuelta 2`, `pendiente 6`;
9. desactivó Compras, comprobó ocultamiento/denegación y restauró el plugin.

## Evidencia visual

Directorio: `docs/evidence/screenshots/J11-S9-06/e2e/`.

- workspace compuesto: 375, 720 y 1280 px;
- directorio de solicitudes: 375, 720 y 1280 px;
- solicitud enviada: 375, 720 y 1280 px;
- orden emitida: 375, 720 y 1280 px;
- recepción confirmada: 375 px;
- devolución confirmada: 720 px;
- seguimiento: 375, 720 y 1280 px, más comprobaciones 599/600/839/840;
- denegación con plugin desactivado: 375 px.

Playwright comprobó estructura accesible, navegación por roles/labels y ausencia
de overflow horizontal normal. Se revisaron directamente seis vistas
representativas de workspace, solicitud, orden, recepción, devolución y
denegación. Esa revisión corrigió el contraste de origen y del enlace
`Agregar o administrar` en la barra superior. El arranque de Chromium y el
recorrido tienen límites de tiempo explícitos; los botones de activación se
resuelven por nombre accesible exacto. Las capturas no contienen contraseñas,
tokens ni datos reales.

## Pendientes reales

- validación funcional/exploratoria independiente por otra persona;
- gate acumulado J11-S9-07, fotografía de plugins, PDF obligatorio y decisión de
  cierre;
- decisión explícita del instalador Windows en J11-S9-08.

No se declara Sprint 9 cerrado, no se promueve la imagen a producción y no se
considera aún una versión comercializable.
