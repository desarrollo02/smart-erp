# J11-S6-06 - Integración y composición física de `business_partners`

- Estado: Completa
- Fecha: 2026-07-29
- Gate: G5 composición y operación
- Dependencia: J11-S6-05 verde

## Objetivo

Incorporar `business_partners` al WAR y al migrador mediante una única selección
física reproducible, construir el par de imágenes Docker con el mismo perfil,
demostrar idempotencia y conservación de datos sobre los volúmenes existentes y
entregar a los implementadores un procedimiento de construcción y demostración.

## Composición aprobada para la demo

El perfil Maven `with-business-partners-demo` se declara exclusivamente en
`distribution/logixone-plugin-set` e incluye:

- `business-partners`, con su dependencia pública `business-partners-api`;
- `reference-plugin`, como fixture funcional transversal;
- `reference-customization-a` y `reference-customization-b`, una para cada empresa
  ficticia del ambiente de demostración.

WAR y migrador dependen de `logixone-plugin-set`; ninguno mantiene una segunda
lista de plugins. El perfil es una composición de desarrollo y demo. No representa
una distribución productiva genérica: al implementar una empresa real se debe
crear e incluir su plugin de personalización exclusivo y retirar fixtures que no
correspondan, conservando un único plugin de personalización asignado por empresa.

## Criterios de aceptación

1. el perfil se declara una sola vez en `logixone-plugin-set`;
2. el WAR contiene los cuatro plugins del perfil y el contrato público de
   participantes;
3. el migrador descubre las mismas cuatro definiciones mediante `ServiceLoader`;
4. una construcción limpia sin perfil deja cero JAR/proveedores de esos plugins;
5. ambos Dockerfiles aceptan el perfil en modo `verified` y rechazan combinaciones
   no declaradas;
6. `core` V6, `plg_business_partners` V1 y `plg_reference_plugin` V1 quedan
   idempotentes en PostgreSQL y Compose;
7. recrear solamente `app` conserva los volúmenes, las activaciones y los datos;
8. liveness/readiness quedan en HTTP 200 y los logs recientes no contienen errores;
9. la pantalla real sigue navegable y responsive en los siete anchos del proyecto;
10. prueba de contrato, reactor, PostgreSQL, Docker, Playwright, guía, runbook y
    evidencia quedan verdes en el mismo cambio.

### Corrección visual de aceptación

La primera candidata cumplía el contrato funcional, pero la revisión del responsable
de producto rechazó su organización: acumulaba directorio, alta, detalle y todas las
mutaciones en una única página demasiado extensa. La historia incorporó por ello
[ADR-0018](../../adr/0018-floorplan-erp-directorio-alta-ficha.md) sin modificar API,
persistencia ni permisos.

La candidata corregida debe demostrar además:

1. navegación lateral persistente en expandido y menú colapsable en medio/compacto;
2. modos separados `directory`, `create` y `detail`;
3. filtro compacto seguido por tabla en expandido y lista adaptable en
   medio/compacto;
4. ficha de lectura con pestañas para datos generales, identificaciones,
   direcciones, contacto, roles y ciclo de vida;
5. ausencia de `ScreenId`, versión optimista, slots y explicaciones técnicas en la
   superficie operativa;
6. recorrido real alta → directorio → ficha → pestaña → mutación, sin overflow de
   página en los tres rangos.

## Resultado

La historia quedó completa. La selección con y sin plugin se construyó e
inspeccionó; una prueba arquitectónica evita que WAR, migrador y Dockerfiles
diverjan. PostgreSQL/Testcontainers, el `verify` completo, ambos Dockerfiles y el
par de imágenes finalizaron verdes.

El migrador oficial se ejecutó dos veces sobre el volumen existente y aplicó cero
cambios en los tres esquemas. Sólo se recreó `logixone-app-1`; PostgreSQL y
Keycloak conservaron sus contenedores y volúmenes. Después del cambio siguieron
presentes cuatro participantes ficticios, incluido `BP-DEMO-001`, y la activación
de `business_partners` para la empresa A.

La nueva demo visual quedó disponible en
`http://localhost:18080/logixone/faces/app/view.xhtml?route=%2Fbusiness-partners&mode=directory`.
La corrección visual reemplazó la página continua por directorio, alta y ficha con
pestañas. Playwright repitió alta,
búsqueda, detalle, asignación de rol, estructura accesible y responsive en
375/599/600/720/839/840/1280 px. Sprint 6 continúa abierto: `J11-S6-07` debe
ejecutar el cierre integral, la demo oficial de Sprint, retrospectiva y PDF.

Evidencia: [J11-S6-06](../../evidence/J11-S6-06-integracion-composicion-business-partners.md).
Guion: [demo reproducible J11-S6-06](../../runbooks/demo-business-partners-j11-s6-06.md).
