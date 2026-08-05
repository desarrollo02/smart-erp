# Demo de definiciones de socios - J11-S8-C02

- Estado: vigésimo corte ejecutado y revisado visualmente; historia y Sprint abiertos
- Fecha: 2026-08-04
- Perfil físico: `with-inventory-demo`
- Composición aislada: `logixone-bpd`, puertos 38080/10180 y volúmenes propios
- Usuario ficticio: `demo.empresas.ab`
- Permiso funcional: `business_partners.manage`
- Evidencia: `../evidence/screenshots/J11-S8-C02-partner-definitions/e2e/`

## Objetivo

Demostrar que una empresa administra tipos de identificación, tipos y propósitos
de dirección y tipos de canal desde **Definiciones de socios**, y que la ficha del
socio ofrece únicamente valores activos de la empresa. Código, clase e identidad
son estables; nombre y estado evolucionan mediante revisiones append-only.

País no forma parte de esta demo: continúa como código textual hasta decidir una
fuente normativa versionada. No presente este corte como cierre de J11-S8-C02.

## Artefactos verificados

| Artefacto | Digest | Tamaño |
|---|---|---:|
| `logixone/app:j11-s8-c02-partner-definitions` | `sha256:52a2c64e9f690900ca7fdf1b1ef0bd66fcc5b5688cad90c4825d2beb64e84af0` | 501.071.129 bytes |
| `logixone/migrator:j11-s8-c02-partner-definitions` | `sha256:c3cffe4b25f66ffbc187e313b79d3b622b547908eaf8c3de39e69da1e42cecf1` | 105.399.374 bytes |

V4 de `business_partners` tiene SHA-256
`481CBC4684F47FB559DA6F1EAFE8E9534DC7CC92BF314E11B8ED170B4F830D99`.
La primera ejecución aislada aplicó V1–V4; la segunda informó
`migrations_executed=0` y `schema_version=4`.

## Preparación segura

1. No reutilice los puertos 18080/8180 ni 28080/9180 de las otras candidatas.
2. Use sólo los cuatro archivos secretos locales declarados por Compose; nunca
   copie su contenido al runbook o a la terminal compartida.
3. Construya ambas imágenes con modo `verified` y perfil
   `with-inventory-demo`.
4. Levante `logixone-bpd` en 38080/10180 con volúmenes nuevos.
5. En una instalación limpia, ejecute el bootstrap global one-shot para el
   subject ficticio multiempresa, compruebe `status=CHANGED` y recree únicamente
   `app` con `LOGIXONE_SYSTEM_AUTHORITY_BOOTSTRAP_ENABLED=false`.
6. Repita el migrador y exija cero cambios. No use SQL manual y no ejecute
   `docker compose down --volumes` como recuperación.
7. Compruebe:
   - `http://localhost:38080/logixone/health/live`;
   - `http://localhost:38080/logixone/health/ready`.

Ambos endpoints deben responder HTTP 200 y `UP` antes de abrir la interfaz.

## Guion funcional

1. Inicie sesión con `demo.empresas.ab` y seleccione la primera empresa ficticia.
2. Habilite `business_partners` y conceda sus cuatro permisos al rol ficticio si
   el entorno está limpio; el E2E lo hace de forma idempotente por la UI.
3. Abra **Definiciones de socios** y pulse **Nueva definición**.
4. Registre una definición de cada clase: tipo de canal, tipo de identificación,
   tipo de dirección y propósito de dirección.
5. Para el tipo de canal, cree una nueva revisión del nombre, abra **Historial**,
   inactívelo y reactívelo. Verifique que código y clase no cambian.
6. Abra **Socios comerciales**, registre un socio ficticio y agregue:
   - una identificación con el tipo recién creado;
   - una dirección con tipo y propósito recién creados;
   - un canal con el nombre revisado.
7. Compruebe las etiquetas visibles seleccionadas y que **Agregar o administrar**
   vuelve al propietario del catálogo.
8. Deshabilite temporalmente el plugin desde administración, confirme la
   denegación segura de la ruta y vuelva a habilitarlo.
9. Repita el recorrido en 1280, 720 y 375 px; no debe existir overflow horizontal
   normal, pérdida de labels ni controles cortados.

## Ejecución automática

```powershell
$projectRoot=(Resolve-Path '.').Path

.\mvnw.cmd -Pvisual-e2e -pl tests/e2e-tests `
  "-Dit.test=BusinessPartnersVisualIT" `
  "-Dlogixone.business-partners.e2e=true" `
  "-Dlogixone.app-url=http://localhost:38080/logixone/faces/app/index.xhtml" `
  "-Dlogixone.admin-url=http://localhost:38080/logixone/faces/admin/index.xhtml" `
  "-Dlogixone.demo-user-password-file=$projectRoot/.tools/secrets/demo-user-password.txt" `
  "-Dlogixone.evidence-dir=$projectRoot/docs/evidence/screenshots/J11-S8-C02-partner-definitions/e2e" `
  "-Dlogixone.playwright.executable=$projectRoot/.tools/playwright/chromium-1228/chrome-win64/chrome.exe" `
  verify
```

## Resultado verificado y recuperación

El recorrido final terminó 1/1 verde en 54,28 s y produjo 23 PNG, 2.625.513 bytes.
Se revisaron originales representativos de alta, historial, identificación,
dirección y denegación en los tres rangos responsive, sin defectos visuales. La
ventana final tuvo cero coincidencias severas en aplicación y PostgreSQL; el
contenedor ejecutó exactamente el digest documentado y conservó bootstrap global
deshabilitado.

Si falla, conserve captura, hora, URL, mensaje y logs. Compruebe primero health,
empresa, activación y permisos. Un valor inactivo no debe ofrecerse en un alta
nueva; su detalle e historial deben seguir visibles. No edite tablas, migraciones
aplicadas ni revisiones para reparar la demo.
