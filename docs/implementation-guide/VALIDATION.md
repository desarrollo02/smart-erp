# Validación independiente de la guía de implementación

- Edición a evaluar: `1.0-rc103`
- Historia de cierre vigente: `J11-S9-08` (ficha acumulada originada en `J11-S2-08`)
- Estado: Baseline congelado y ejecutable; pendiente de validador independiente
- Regla: completar este recorrido usando únicamente el repositorio y la [guía](README.md), sin instrucciones orales de sus autores

## Decisión de calendario

J11-S9-07 dejó G0–G6 verdes y J11-S9-08 agregó el instalador interno solicitado.
Esta ficha se ejecuta exactamente contra la guía `1.0-rc103`, las imágenes de
cierre J11-S9-07 y el instalador `0.9.0-internal.1`. La completará una persona que
no haya implementado las capacidades
evaluadas. La preparación y las pruebas automatizadas realizadas por los autores
no equivalen a este recorrido; todos los campos y casillas permanecen pendientes
hasta que el validador registre resultados propios.

## Quién puede validar

La persona validadora debe tener conocimientos suficientes para operar Java, Maven y
Docker, pero no haber implementado las capacidades de empresa, activación,
persistencia, identidad, autorización, administración, composición de pantallas,
migraciones de plugins, o dominio, aplicación, interfaz, composición y cierre de
`business_partners`, `commercial_catalog`, `inventory` o `purchasing` de Sprint
2 a Sprint 9.
Puede ser un implementador, desarrollador, responsable técnico o integrador que
cumpla esa independencia.

Antes de iniciar debe registrar:

| Dato | Valor a completar |
|---|---|
| Nombre o identificador del validador | PENDIENTE |
| Rol o perfil | PENDIENTE |
| Fecha | PENDIENTE |
| Sistema operativo | PENDIENTE |
| Experiencia previa con Smart ERP | PENDIENTE |
| Confirmación de que no implementó las capacidades evaluadas de Sprint 2 a Sprint 9 | PENDIENTE |

## Reglas del recorrido en limpio

1. comenzar en una terminal nueva, en la raíz del repositorio;
2. no consultar a los autores mientras se ejecuta cada paso;
3. registrar como hallazgo cualquier comando, concepto, archivo o decisión que requiera conocimiento no escrito;
4. no usar credenciales reales ni modificar el proyecto legado;
5. no marcar un resultado como aprobado si solo se infirió y no se comprobó;
6. detenerse ante cualquier paso destructivo cuyo alcance no sea inequívoco.

## Recorrido obligatorio

### A. Comprensión

- [ ] Pude explicar con mis palabras la diferencia entre kernel, plugin funcional y plugin de personalización.
- [ ] Entendí por qué cada empresa debe tener exactamente una personalización propia.
- [ ] Distinguí identidad/UI ya disponibles en la candidata de los dominios ERP y la administración productiva todavía pendientes.
- [ ] Pude clasificar los requisitos ficticios BOR-001 a BOR-005 sin ayuda externa.
- [ ] Identifiqué por qué una personalización no puede reemplazar XHTML, importar beans ajenos ni escribir tablas privadas de otro plugin.
- [ ] Distinguí un rol empresarial con `CompanyId` de un rol global de la instancia.
- [ ] Entendí que Keycloak autentica, pero no concede por sí solo permisos empresariales ni globales del kernel.
- [ ] Pude separar el participante y sus roles cliente/proveedor de crédito, precios, bancos, contabilidad, documentos y SIFEN.
- [ ] Entendí cómo BP-D01 a BP-D10 se materializan en dominio/API, tablas privadas, comandos, permisos y pantallas productivas.
- [ ] Pude explicar por qué `plg_business_partners` no tiene FK hacia `core.company`, por qué una identificación duplicada es advertencia y por qué V1 no se modifica.
- [ ] Pude explicar por qué `commercial_catalog` separa `view`, `items.manage`, `prices.manage` y `definitions.manage` y por qué sus pantallas no pueden sustituir esas guardas.
- [ ] Pude explicar cómo el perfil físico, el menú fusionado, los permisos y la validación en navegador de `commercial_catalog` se relacionan sin compartir entidades o XHTML.
- [ ] Pude explicar por qué Inventario depende de Catálogo, pero no lee sus tablas ni relaciona entidades JPA.
- [ ] Pude separar solicitud, orden, recepción y devolución de factura, deuda, pago, retención, costo y asiento.
- [ ] Entendí por qué Compras exige un aprobador distinto y por qué una devolución vuelve a abrir el pendiente del proveedor.
- [ ] Pude explicar cómo Compras usa APIs públicas y snapshots para proveedor, artículo, moneda, unidad y depósito.

### B. Ambiente y gate Maven

Seguí el capítulo 5 y ejecuté el gate del capítulo 11:

```powershell
.\mvnw.cmd -B -Pwith-purchasing-demo `
  "-Dlogixone.postgres.integration=true" clean verify
```

- [ ] El comando terminó con código `0`.
- [ ] Comprendí qué pruebas necesitan Docker/Testcontainers.
- [ ] No necesité descargar artefactos fuera de `.tools/`.

Resultado y observaciones:

```text
PENDIENTE
```

### C. Composición física

Construí la variante completa indicada por la guía:

```powershell
.\mvnw.cmd -B -Pwith-purchasing-demo `
  -pl migrator,distribution/logixone-war -am clean package
```

- [ ] El WAR fue creado en `distribution/logixone-war/target/logixone.war`.
- [ ] El ejecutable fue creado en `migrator/target/migrator-0.1.0-SNAPSHOT-executable.jar`.
- [ ] Pude identificar los cinco plugins productivos y los tres fixtures del perfil.
- [ ] Pude identificar los mismos ocho proveedores en el SPI del migrador y confirmé que no contiene `jakarta/`.
- [ ] Entendí que una empresa usa solo la personalización que tiene asignada aunque el catálogo físico contenga otras.

Resultado y observaciones:

```text
PENDIENTE
```

### D. Operación con Docker y Compose

Seguí los capítulos 10 y 11, y los runbooks enlazados, usando nombres y puertos de prueba que no colisionaran con otros entornos.

- [ ] Ambos Dockerfiles superaron `docker buildx build --check`.
- [ ] Construí o reutilicé conscientemente imágenes locales identificadas.
- [ ] Compose creó PostgreSQL, ejecutó el migrador antes de la aplicación y alcanzó readiness `UP`.
- [ ] Keycloak importó el realm sin usuarios/passwords versionados y quedó saludable antes de `app`.
- [ ] Verifiqué redirect web, `401` REST, login/logout y rechazo de issuer/firma/audience/expiración inválidos.
- [ ] Verifiqué que liveness no consulta Keycloak y readiness no lo sondea externamente.
- [ ] Pude explicar cuándo se crean ambos volúmenes y por qué `down` sin `--volumes` conserva sus datos.
- [ ] Entendí que `down --volumes` elimina PostgreSQL y el estado local de Keycloak del proyecto de prueba.
- [ ] Ejecuté bootstrap exacto/repetido/incompatible sin endpoint anónimo ni duplicados.
- [ ] Abrí las rutas administrativas permitidas y comprobé denegación genérica en las no autorizadas.
- [ ] Recorrí usuarios, membresías, roles empresariales y autoridad global sin usar SQL directo.
- [ ] Comprobé que una reducción de autoridad no puede eliminar al último administrador global efectivo.
- [ ] Consulté auditoría con filtros y paginación, confirmé que comienza en V5, que V6 agrega recursos de plugins y que no permite edición o borrado.
- [ ] Pude explicar por qué `business_partners.view`, `manage`, `roles.manage` y `lifecycle.manage` no son intercambiables.
- [ ] Pude explicar por qué `commercial_catalog.view`, `items.manage`, `prices.manage` y `definitions.manage` no son intercambiables.
- [ ] Creé un artículo/servicio y una lista/precio ficticios, desactivé el plugin, observé denegación y lo reactivé sin pérdida.
- [ ] Recorrí depósito, existencias, movimiento, reserva y conteo ficticios; confirmé disponibilidad y denegación del plugin inactivo.
- [ ] Creé una solicitud, la aprobé con otro actor, emití la orden, confirmé recepción/devolución y consulté seguimiento.
- [ ] Verifiqué en 375, 720 y 1280 px que Compras conserva acciones y no presenta overflow horizontal normal.
- [ ] Verifiqué que un adaptador pide `CurrentCompanyAuthorization` por operación y no guarda la prueba autorizada en sesión.
- [ ] Comprobé que la vista no expone issuer, subject OIDC, credenciales, tokens, SQL, stacktraces ni datos comerciales.
- [ ] No apareció ningún secreto en comandos, imágenes o logs copiados a esta ficha.

Nombres de proyecto, imágenes y resultados:

```text
PENDIENTE
```

### E. Caso Distribuidora Boreal

- [ ] Pude recorrer relevamiento, clasificación, composición, personalización, prueba, despliegue, persistencia, rollback y entrega.
- [ ] Pude señalar qué partes del ejemplo son ejecutables y cuáles son ilustrativas o futuras.
- [ ] Pude explicar cómo el overlay A cambia la pantalla sin conceder autorización ni omitir validación de negocio.
- [ ] Pude preparar una lista de evidencias para entregar la implementación ficticia a otra persona.

Resultado y observaciones:

```text
PENDIENTE
```

## Hallazgos

Registrar incluso dudas resueltas. La severidad es `bloqueante`, `mayor`, `menor` o `editorial`.

| ID | Capítulo/paso | Severidad | Hallazgo | Información que faltó | Resolución o decisión |
|---|---|---|---|---|---|
| VAL-01 | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |

## Criterio de aprobación

La guía pasa a `1.0` solamente si:

1. todos los bloques A–E fueron recorridos;
2. no queda un hallazgo bloqueante o mayor sin corregir o aceptar expresamente como límite;
3. los comandos ejecutables terminaron según lo documentado;
4. el validador pudo diferenciar capacidades reales de ejemplos y trabajo futuro;
5. toda ayuda necesaria quedó incorporada a la guía, no solo comunicada verbalmente.

## Dictamen

Completar una de estas opciones:

- [ ] **Aprobada:** puede publicarse como edición `1.0`.
- [ ] **Aprobada con observaciones:** puede publicarse una vez aplicadas las resoluciones registradas.
- [ ] **Requiere correcciones y un nuevo recorrido:** existen brechas que impiden aprender o ejecutar el procedimiento de manera autónoma.

Declaración del validador:

```text
PENDIENTE: confirmo que realicé el recorrido en limpio, que no implementé las capacidades evaluadas y que los resultados anteriores reflejan lo observado.
```
