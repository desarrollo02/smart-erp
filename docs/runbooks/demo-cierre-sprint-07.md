# Demo visual de cierre de Sprint 7

- Estado: Ejecutada y disponible; cierre formal condicionado a G7 independiente
- Fecha: 2026-07-31
- Historia: `J11-S7-07`
- Perfil Maven: `with-commercial-catalog-demo`
- URL inicial: `http://localhost:18080/logixone/faces/app/index.xhtml`
- Evidencia: `docs/evidence/screenshots/J11-S7-07-closing/e2e/`

## Objetivo

Mostrar sobre WildFly 41 que dos plugins productivos se componen dentro del mismo
ERP: el shell fusiona sus menús, cada operación revalida empresa/plugin/permiso en
el servidor y los datos sobreviven a migración idempotente y recreación de la
aplicación. La interfaz es Jakarta Faces, Material Design 3 y responsive.

La demo no representa inventario, compras, ventas, facturación, SIFEN ni un
despliegue productivo.

## Baseline demostrado

- aplicación `logixone/app:j11-s7-07-closing`, digest local
  `sha256:769a078532b26e766675349c50b9dee0be134168aefcde1144b05dfc8e7f2975`;
- migrador `logixone/migrator:j11-s7-07-closing`, digest local
  `sha256:8343559f7accf81a4ef916415fd2248a51a70a29968ce012f5ba6e897e55bc0d`;
- WAR de 1040380 bytes y SHA-256
  `66F1021245186125BA98BE051B74FA6DD1CEEDD4530B2EA49AA80628055407E6`;
- plugins descubiertos: `business_partners`, `commercial_catalog`,
  `reference_plugin`, `reference_custom_a` y `reference_custom_b`;
- PostgreSQL `core` V6, `plg_business_partners` V1,
  `plg_commercial_catalog` V1 y `plg_reference_plugin` V1;
- Keycloak 26.7.0 y OIDC nativo de WildFly.

Los plugins `reference_*` son fixtures técnicos; una implementación real debe
crear una personalización exclusiva para su empresa.

## Preparación segura

1. Trabajar desde la raíz y comprobar la existencia de
   `infra/compose/compose.env.local` y los cuatro archivos de secretos bajo
   `.tools/secrets/`, sin mostrar sus contenidos.
2. Fijar las dos imágenes del mismo corte:

```powershell
$env:LOGIXONE_APP_IMAGE='logixone/app:j11-s7-07-closing'
$env:LOGIXONE_MIGRATOR_IMAGE='logixone/migrator:j11-s7-07-closing'
```

3. Validar Compose y ejecutar dos veces el migrador:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml config --quiet
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
```

La segunda ejecución debe informar cero migraciones para los cuatro propietarios.
No usar `down --volumes`.

4. Recrear únicamente la aplicación:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --no-deps --force-recreate `
  --wait --wait-timeout 240 app
```

5. Confirmar HTTP 200 y `UP` en:

- `http://localhost:18080/logixone/health/live`;
- `http://localhost:18080/logixone/health/ready`.

## Usuario y datos

Use `demo.empresas.ab`. Su contraseña se lee de
`.tools/secrets/demo-user-password.txt` y nunca se copia a documentación,
capturas o chats. Todos los nombres y códigos creados durante la presentación
deben ser ficticios y llevar un sufijo único.

Antes de presentar, la empresa A debe tener activos `business_partners` y
`commercial_catalog`. El rol empresarial del usuario necesita los cuatro permisos
de cada plugin. Ajuste activación y permisos sólo desde las pantallas
administrativas; no use SQL directo.

## Guion paso a paso

### 1. Explicar shell y menú fusionado

1. Inicie sesión, elija la empresa A y abra el espacio de trabajo.
2. Señale que «Socios comerciales», «Artículos y servicios» y «Listas de precios»
   aparecen juntos aunque provienen de plugins distintos.
3. Explique que el shell ordena contribuciones públicas autorizadas; no concatena
   XHTML ni permite que un plugin importe la UI interna de otro.

### 2. Mostrar personalización por empresa

1. Abra «Panel de demostración» en la empresa A y observe el overlay A.
2. Cambie a la empresa B y muestre que la misma pantalla base recibe el overlay B.
3. Aclare que cada empresa usa exactamente una personalización y que esa capa no
   concede permisos ni modifica tablas ajenas.

### 3. Recorrer Socios Comerciales

1. Vuelva a la empresa A y abra el directorio.
2. Busque un participante, abra su ficha y recorra resumen, datos generales,
   identificaciones, direcciones, contacto y roles/estado.
3. Registre un socio ficticio desde el alta separada y confirme que vuelve a una
   ficha navegable.

### 4. Registrar un artículo o servicio

1. Abra «Artículos y servicios» y pulse «Nuevo artículo o servicio».
2. Registre un servicio ficticio con código único, unidad base y alcances
   comerciales.
3. En la ficha agregue un identificador y asigne clasificación autorizada.
4. Regrese al directorio y búsquelo. Explique que el catálogo describe conceptos;
   todavía no existe stock ni documento comercial.

### 5. Administrar una lista y un precio

1. Abra «Listas de precios» y cree una lista ficticia con moneda, política de
   impuestos, escala y redondeo.
2. Abra la pestaña «Precios», seleccione el servicio y agregue un importe ficticio.
3. Muestre el resultado persistido y aclare que futuros documentos tomarán un
   snapshot; un cambio de lista no reescribirá historia.

### 6. Probar activación y denegación

1. Desde administración desactive `commercial_catalog` para la empresa A usando
   la versión de decisión visible.
2. Compruebe que sus menús dejan de aportarse y que una ruta directa muestra
   denegación genérica.
3. Reactive el plugin y confirme que artículos, listas y precios siguen presentes.
4. Repita conceptualmente la misma regla para `business_partners`; no es necesario
   desactivarlo si la demo automatizada ya dejó evidencia.

La demo debe terminar con ambos plugins activos.

### 7. Mostrar responsive

Repita directorio, alta y ficha en 375, 720 y 1280 px. En compacto la navegación
se colapsa, filtros/acciones se apilan y las tablas adoptan listas. Mantenga labels,
foco, mensajes y acciones sin desplazamiento horizontal normal.

## Resultados esperados

- login/logout OIDC y selección empresarial operativos;
- cinco plugins descubiertos y menús fusionados por contrato;
- operaciones persistentes reales de socios, artículos, listas y precios;
- permisos y activación aplicados por el servidor;
- denegación con plugin inactivo y recuperación sin pérdida;
- health en `UP` y UI usable en compacto, medio y expandido.

## Restauración y detención

1. Confirme que ambos plugins quedaron activos.
2. Cierre sesión.
3. Para detener sin borrar datos:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down
```

No añada `--volumes`. Para volver a un conjunto exacto de datos use el runbook de
backup/restauración; no borre filas manualmente del único volumen recuperable.

## Limitaciones

La ejecución automática oficial terminó con 7/7 escenarios y 47 capturas
revisadas. G0-G6 están verdes, pero G7 requiere que otra persona complete
`docs/implementation-guide/VALIDATION.md`. Hasta entonces no se publica la guía
`1.0`, no se promueven imágenes, no se declara cierre formal y no se autoriza
producción.
