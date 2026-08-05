# Demo reproducible de plugins e inventario - J11-S8-06

- Estado: Disponible como candidata visual
- Fecha: 2026-08-01
- URL inicial: `http://localhost:18080/logixone/faces/app/index.xhtml`
- Perfil: `with-inventory-demo`
- Imagen de aplicación: `logixone/app:j11-s8-06-inventory-demo`
- Imagen de migrador: `logixone/migrator:j11-s8-06-inventory-demo`

## Qué demuestra

La composición actual ejecuta un WAR WildFly 41 con `reference_data`,
`business_partners`, `commercial_catalog` e `inventory` como plugins productivos
separados. El shell
Jakarta Faces fusiona sus menús mediante contratos neutrales. Cada pantalla vuelve
a comprobar empresa, plugin y permiso en el servidor.

La demostración cubre estructura de depósitos, inscripción de un producto,
movimiento de entrada, reserva, disponibilidad y conteo físico. No demuestra
compras, ventas, costos, valoración, contabilidad, documentos fiscales ni SIFEN.
Los plugins de referencia y personalización A/B son fixtures y no deben promoverse
a una empresa real.

## Prerrequisitos

1. Java 21 y Maven Wrapper configurados;
2. Docker Engine y Compose operativos;
3. `infra/compose/compose.env.local` y los cuatro secretos externos preparados;
4. puertos locales 18080 y 8180 disponibles;
5. usar exclusivamente usuarios y datos ficticios.

No ejecutar `down --volumes`, no editar migraciones aplicadas y no colocar
contraseñas en comandos, argumentos de build, capturas ni documentación.

## Construir el par de imágenes

```powershell
docker build --check --file infra/docker/Dockerfile .
docker build --check --file infra/docker/Dockerfile.migrator .

docker build --file infra/docker/Dockerfile.migrator `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/migrator:j11-s8-06-inventory-demo .

docker build --file infra/docker/Dockerfile `
  --build-arg LOGIXONE_BUILD_MODE=verified `
  --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo `
  --tag logixone/app:j11-s8-06-inventory-demo .
```

Aplicación y migrador deben construirse con el mismo perfil. Cambiar uno sin el
otro invalida la distribución.

## Migrar y arrancar sin borrar volúmenes

```powershell
$env:LOGIXONE_APP_IMAGE='logixone/app:j11-s8-06-inventory-demo'
$env:LOGIXONE_MIGRATOR_IMAGE='logixone/migrator:j11-s8-06-inventory-demo'

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator

docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --wait --wait-timeout 240
```

La segunda ejecución debe informar cero migraciones nuevas. `up` crea los
volúmenes nombrados sólo si faltan y reutiliza los existentes. Detener con `down`
conserva datos; nunca añadir `--volumes` como parte del recorrido normal.

## Preparar las definiciones ficticias del catálogo

El fixture controlado aporta dos unidades, una categoría, una marca y un perfil
tributario. No activa plugins, no concede permisos y no crea operaciones de
inventario.

Obtenga desde administración el UUID de la empresa ficticia A y defínalo:

```powershell
$companyId='<uuid-de-la-empresa-ficticia-a>'
Get-Content `
  plugins/commercial-catalog/src/test/resources/demo/seed-commercial-catalog-demo.sql `
  -Encoding UTF8 | docker compose `
    --env-file infra/compose/compose.env.local `
    -f infra/compose/compose.yaml exec -T postgres `
    psql -U logixone -d logixone -v company_id=$companyId
```

Repetir el comando conserva las mismas definiciones y no duplica filas.

## Activar y conceder permisos por la UI

1. inicie sesión con el usuario ficticio multiempresa;
2. abra **Administración > Plugins por empresa** y seleccione empresa A;
3. habilite, en orden, `reference_data`, `business_partners`,
   `commercial_catalog` e `inventory`;
4. abra **Administración > Seguridad empresarial** para la misma empresa;
5. seleccione el rol operador de demo;
6. conceda `reference_data.view`, los permisos funcionales necesarios de
   participantes, catálogo y los siete permisos de inventario;
7. cierre sesión e inicie una sesión nueva;
8. seleccione empresa A y compruebe el menú fusionado.

La renovación de sesión es obligatoria porque las autoridades efectivas se
capturan al autenticar. No crear activaciones o concesiones con SQL.

## Guion para presentar

1. Muestre el inicio y explique que **Socios comerciales**, **Artículos y
   servicios**, **Listas de precios**, **Existencias**, **Depósitos** y **Conteos**
   provienen de plugins independientes y se fusionan en un solo menú.
2. Abra **Artículos y servicios** y muestre el producto ficticio activo que se usará
   como referencia; inventario no consulta sus tablas privadas.
3. Abra **Depósitos**, cree un depósito con código único y muestre que se crea la
   ubicación obligatoria `GENERAL`.
4. Abra **Existencias**, inscriba el producto usando su unidad base y seleccione la
   ubicación `GENERAL` del depósito recién creado.
5. Contabilice una entrada de `12` unidades y muestre el libro trazable.
6. Cree una reserva de `3` unidades y consulte el resumen: físico `12`, reservado
   `3`, disponible `9`.
7. Abra **Conteos**, cree un conteo para ese depósito/ubicación, agregue la línea,
   inicie, registre `12`, revise y contabilice.
8. Muestre el conteo en estado **Contabilizado**, con una línea, una contada y cero
   diferencias.
9. Cambie entre 375, 720 y 1280 px: formularios, navegación y acciones deben
   reordenarse sin desplazamiento horizontal normal.
10. Como control negativo, deshabilite `inventory`, abra una ruta directa y muestre
    la denegación; luego vuelva a habilitarlo e inicie una sesión nueva.

## Verificación automática del recorrido

```powershell
$projectRoot=(Resolve-Path '.').Path

.\mvnw.cmd -Pvisual-e2e -pl tests/e2e-tests `
  "-Dit.test=InventoryVisualIT" `
  "-Dlogixone.inventory.e2e=true" `
  "-Dlogixone.app-url=http://localhost:18080/logixone/faces/app/index.xhtml" `
  "-Dlogixone.admin-url=http://localhost:18080/logixone/faces/admin/index.xhtml" `
  "-Dlogixone.demo-user-password-file=$projectRoot/.tools/secrets/demo-user-password.txt" `
  "-Dlogixone.evidence-dir=$projectRoot/docs/evidence/screenshots/J11-S8-06/e2e" `
  "-Dlogixone.playwright.executable=$projectRoot/.tools/playwright/chromium-1228/chrome-win64/chrome.exe" `
  verify
```

## Estado posterior y detención segura

El escenario usa sufijos aleatorios y deja sus datos ficticios en los volúmenes
persistentes. También restaura `inventory` habilitado. No borre filas mediante SQL;
para volver a un estado exacto use un backup controlado o futuros casos de uso de
archivo.

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down
```

El comando conserva PostgreSQL y Keycloak. Esta candidata no cierra Sprint 8 ni
autoriza producción; faltan J11-S8-07 y el instalador J11-S8-08.
