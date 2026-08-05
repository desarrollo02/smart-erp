# Demo candidata de perfiles tributarios - J11-S8-C01

- Estado: ejecutada contra imagen candidata; recongelación formal pendiente
- Fecha: 2026-08-01
- Perfil físico: `with-inventory-demo`
- Usuario ficticio: `demo.empresas.ab`
- Permiso adicional: `commercial_catalog.definitions.manage`

## Objetivo

Demostrar que un administrador puede crear un perfil tributario interno y que el
nuevo perfil aparece luego en el selector de un artículo. La demo no acredita
cumplimiento tributario ni integración SIFEN.

## Preparación

1. Levante el perfil oficial con el procedimiento de Sprint 8.
2. Active `commercial_catalog` para la empresa ficticia.
3. Conceda al rol de demo `commercial_catalog.view`,
   `commercial_catalog.items.manage` y
   `commercial_catalog.definitions.manage`.
4. Ejecute el fixture idempotente del catálogo. Deben existir perfiles ficticios
   general, reducido y exento.

## Recorrido

1. Inicie sesión y seleccione la empresa de demostración.
2. Abra **Perfiles tributarios**. Explique que el menú sólo aparece con el permiso
   de administración de definiciones.
3. Muestre código, nombre, tratamiento interno, vigencia y estado de los perfiles
   ficticios. Aclare que no contienen la tasa oficial ni códigos SIFEN.
4. Pulse **Nuevo perfil**.
5. Registre:
   - código: `DEMO_VISUAL` más un sufijo único;
   - nombre: `Perfil visual` más el mismo sufijo;
   - tratamiento: `TAXED_DEMO`;
   - descripción: `Perfil ficticio sin equivalencia fiscal certificada`;
   - vigente desde: `2026-08-01T00:00:00Z`.
6. Confirme el mensaje **Perfil tributario registrado** y abra su ficha.
7. Abra **Artículos y servicios**, cree un concepto y seleccione el perfil recién
   creado.
8. Termine mostrando en la ficha del artículo la referencia tributaria asignada.

## Evidencia ejecutada

- Fecha y ambiente: 2026-08-01, Docker Desktop 29.6.2, WildFly 41,
  PostgreSQL 18.4 y Keycloak 26.7.0.
- Imagen candidata: `logixone/app:j11-s8-c01-candidate`, ID local
  `sha256:dc0371593ba637e602a3bb543ce15aff5679116f65836d2dc2f91f8c57a2ce2c`.
- Health: liveness y readiness HTTP 200/`UP`; aplicación, PostgreSQL y Keycloak
  saludables.
- Fixture: tres perfiles base ficticios por empresa; una base existente conserva
  revisiones ya aplicadas y recibe solamente altas/actualizaciones idempotentes.
- Playwright focal: 1 prueba, 0 fallos, 0 errores y 0 omitidas.
- Se comprobó directorio y alta en 375, 720 y 1280 px, labels, estilos cargados y
  ausencia de overflow horizontal normal.
- Se revocó temporalmente `definitions.manage`: desapareció el menú, la ruta
  directa quedó denegada y el permiso fue restaurado antes de terminar.
- Capturas: `docs/evidence/screenshots/J11-S8-C01/e2e/`.

Permanece pendiente resolver el gate transversal de selectores, asignar los digests
finales después de recongelar el baseline, regenerar el PDF de cierre y preguntar a
producto si se creará un instalador nuevo. El `current` anterior no representa
J11-S8-C01 y sólo se reemplaza si la respuesta es `SÍ`.

## Restauración

Los datos son ficticios y pueden conservarse para repetir la demo. No ejecutar
`DELETE`, `TRUNCATE`, `DROP` ni `docker compose down --volumes`. Si se requiere un
ambiente vacío, use una empresa ficticia nueva siguiendo la guía de implementación.
