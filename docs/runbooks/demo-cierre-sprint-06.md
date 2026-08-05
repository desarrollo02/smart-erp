# Demo visual de cierre de Sprint 6

- Estado: Ejecutada y disponible; cierre formal condicionado a validación independiente G7
- Fecha: 2026-07-30
- Historia: `J11-S6-07`
- Perfil Maven: `with-business-partners-demo`
- URL inicial: `http://localhost:18080/logixone/faces/app/index.xhtml`
- Evidencia: `docs/evidence/screenshots/J11-S6-07-closing/e2e/`

## Objetivo

Mostrar en una ejecución real de WildFly 41 que el primer plugin productivo
`business_partners` funciona dentro del monolito modular, conserva sus datos al
recrear la aplicación y respeta empresa, activación, permisos, Jakarta Faces,
Material Design 3 y los tres rangos responsive del producto.

La demo no es un mock y no representa todavía ventas, facturación, documentos
fiscales, SIFEN ni una implantación productiva.

## Baseline demostrado

- aplicación: `logixone/app:j11-s6-07-closing`, digest local
  `sha256:12e874125851bd304b41369a6b4d38f537014d4d398c7313bee8efbdc57b533d`;
- migrador: `logixone/migrator:j11-s6-07-closing`, digest local
  `sha256:45e18b0ef2dd8bebee5c84417c4b1e1a1eed8ca9e5517980c8ab81e4358e69b8`;
- WAR: 792107 bytes y SHA-256
  `D4BA9C2DF4CA29AAA59375B90775D59AFBA7EC8082A47347EE6CB960DC107095`;
- composición física: `business_partners`, `reference_plugin`,
  `reference_customization_a` y `reference_customization_b`;
- PostgreSQL: `core` V6, `plg_business_partners` V1 y
  `plg_reference_plugin` V1;
- identidad: Keycloak 26.7.0 y OIDC nativo de WildFly.

Los plugins `reference_*` son fixtures técnicos para demostrar dos empresas con
personalizaciones diferentes. No son personalizaciones reutilizables para una
empresa real.

## Preparación segura

1. Trabajar desde la raíz del repositorio.
2. Comprobar que `infra/compose/compose.env.local` y los cuatro secretos externos
   bajo `.tools/secrets/` existen; no mostrar sus contenidos.
3. Definir para la terminal actual las dos imágenes del mismo corte:

```powershell
$env:LOGIXONE_APP_IMAGE='logixone/app:j11-s6-07-closing'
$env:LOGIXONE_MIGRATOR_IMAGE='logixone/migrator:j11-s6-07-closing'
```

4. Validar Compose y ejecutar dos veces el migrador:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml config --quiet
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml run --rm migrator
```

Cada propietario debe informar cero migraciones en la segunda ejecución. No usar
`down --volumes`: eliminaría los datos de PostgreSQL y el estado local de
Keycloak.

5. Recrear únicamente la aplicación y esperar salud:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --no-deps --force-recreate `
  --wait --wait-timeout 180 app
```

6. Confirmar HTTP 200 y `UP` en:

- `http://localhost:18080/logixone/health/live`;
- `http://localhost:18080/logixone/health/ready`.

Readiness debe mostrar `catalog`, `configuration`, `database`, `migrations` y
`oidc-configuration` en `UP`.

## Datos y usuario de presentación

Use únicamente el usuario ficticio multiempresa `demo.empresas.ab`. La contraseña
se lee de `.tools/secrets/demo-user-password.txt` y nunca se copia al guion,
capturas, terminal compartida o chat.

Los escenarios automáticos dejan participantes ficticios con códigos aleatorios.
Para una presentación estable puede buscar `BP-DEMO-001` o crear un código nuevo
con prefijo `DEMO-`. No borre filas con SQL para limpiar la pantalla.

## Guion paso a paso

### 1. Explicar la composición empresarial

1. Inicie sesión y seleccione la empresa A.
2. Abra «Panel de demostración» y muestre que la personalización A cambia etiqueta,
   ayuda, obligatoriedad e inserta una tarjeta mediante contratos públicos.
3. Cambie a la empresa B y muestre que la misma pantalla base oculta un elemento y
   agrega contenido diferente.
4. Aclare que cada empresa tiene exactamente una personalización física exclusiva
   y que los plugins funcionales nunca dependen de ella.

### 2. Mostrar el directorio de Socios Comerciales

1. Vuelva a la empresa A y abra «Socios comerciales».
2. Señale la separación entre directorio, alta y ficha; ya no existe una página
   vertical con todos los formularios simultáneos.
3. Filtre por nombre o código y abra un resultado.
4. Explique que clientes y proveedores son roles independientes del mismo
   participante y que la lista no expone IDs de pantalla, slots ni versiones
   técnicas.

### 3. Registrar un participante ficticio

1. Pulse «Nuevo socio».
2. Ingrese un código único, tipo organización, nombre visible y nombres legales
   ficticios.
3. Registre y confirme que el sistema abre la ficha resultante.
4. Muestre el resumen y el control de versión optimista sin revelar identificadores
   internos innecesarios.

### 4. Completar la ficha por tareas

1. En «Datos generales», cambie código o nombres y guarde.
2. En «Identificaciones», agregue un tipo y número ficticios.
3. En «Direcciones», agregue una dirección y localidad ficticias.
4. En «Contacto», agregue un canal general, por ejemplo correo, y una persona de
   contacto ficticia.
5. En «Roles y estado», asigne cliente y proveedor, active/inactive cada rol y
   finalmente inactive/reactive el participante.
6. Regrese al resumen y confirme que roles, conteos y estado reflejan cada cambio.

Cada mutación revalida en el servidor la identidad, empresa, plugin y permiso; usa
JTA, versión optimista y auditoría. Ocultar un botón no sustituye esa autorización.

### 5. Demostrar desactivación sin pérdida

1. Abra la administración de plugins con el mismo usuario autorizado.
2. Seleccione la empresa A y desactive `business_partners` confirmando la versión
   de decisión actual.
3. Intente abrir la ruta funcional y muestre la denegación genérica; el menú deja
   de aportar la capacidad.
4. Reactive el plugin desde la administración.
5. Regrese al directorio y busque el participante creado: sus datos continúan.

La desactivación cambia el estado efectivo; no elimina esquema, migraciones ni
filas. La demo debe terminar con `business_partners` activo.

### 6. Mostrar responsive y accesibilidad

Repita directorio, alta y ficha en:

- compacto: 375 px;
- medio: 720 px;
- expandido: 1280 px.

En compacto, la navegación se colapsa, la tabla adopta lista y las pestañas se
reordenan. En todos los tamaños deben mantenerse labels, foco visible, acciones y
mensajes, sin desplazamiento horizontal normal.

## Resultados esperados

- login/logout OIDC y selección de empresa operativos;
- personalizaciones A/B diferentes sobre el mismo contrato base;
- alta, búsqueda, detalle, identificación, dirección, canal, contacto, roles y
  ciclo de vida persistidos;
- desactivación denegando la función y reactivación recuperándola sin pérdida;
- health en `UP` y composición de cuatro plugins;
- interfaz JSF Material Design 3 usable en 375, 720 y 1280 px.

## Restauración y cierre de la sesión

1. Confirme que `business_partners` quedó activo para las empresas de demo.
2. Cierre la sesión desde el shell.
3. Puede conservar la composición encendida. Para detenerla sin borrar datos:

```powershell
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml down
```

No añada `--volumes`. Si se necesita un estado de datos idéntico, restaure un
backup controlado conforme al runbook de PostgreSQL; no use SQL manual sobre el
único volumen recuperable.

## Limitaciones y estado de cierre

La ejecución oficial automatizada terminó con 6/6 escenarios Playwright y generó
35 capturas revisadas. G0-G6 técnicos pueden quedar verdes. G7 requiere que otra
persona complete `docs/implementation-guide/VALIDATION.md`; mientras siga
pendiente no se publica la guía `1.0`, no se promueven imágenes y no se declara
cerrado formalmente el Sprint.
