# J11-S7-04 - Aplicación y seguridad de `commercial_catalog`

- Estado: Completada
- Sprint: 7
- Fecha de inicio: 2026-07-30
- Gate principal: G3 aplicación/seguridad
- ADR: [ADR-0021](../../adr/0021-aplicacion-autorizacion-auditoria-commercial-catalog.md)

## Objetivo

Crear casos de uso neutrales, transaccionales, autorizados y auditados sobre la
persistencia verde, sin abrir todavía una interfaz o endpoint.

## Alcance

- cuatro permisos públicos y prueba de autorización exacta;
- comandos de ítems, listas/precios, definiciones y ciclo de vida;
- consultas paginadas, detalle, definiciones disponibles y listas;
- adaptadores de directorio, conversión y cotización pública;
- códigos manuales o secuencia atómica;
- resultados estables para acceso, versión, unicidad, referencia y vigencia;
- auditoría técnica sin datos comerciales;
- límite CDI/JTA y pruebas unitarias/PostgreSQL/ArchUnit.

## Fuera de alcance

- menú, Jakarta Faces, Material Design, responsive y Playwright;
- composición WAR/migrador, imágenes y activación empresarial;
- importación masiva, promociones, inventario, costos, documentos y SIFEN;
- actualización destructiva o borrado físico de definiciones/historia.

## Criterios de aceptación

- **CA-01:** descriptor declara exactamente cuatro permisos del catálogo.
- **CA-02:** aplicación y puertos permanecen libres de Jakarta/JDBC/Hibernate.
- **CA-03:** autorización exige plugin, permiso y empresa confiables antes de I/O.
- **CA-04:** altas automáticas usan secuencia, nunca `MAX + 1`.
- **CA-05:** comandos de ítem/precio usan versión esperada y resultados estables.
- **CA-06:** definiciones se administran por casos de uso, no SQL desde UI.
- **CA-07:** directorio, detalle, conversión y cotización aíslan empresa.
- **CA-08:** mutaciones auditan resultado e IDs técnicos sin datos comerciales.
- **CA-09:** JTA agrupa mutación y auditoría; consultas usan `SUPPORTS`.
- **CA-10:** no se agregan menú, pantalla, endpoint ni composición física.
- **CA-11:** módulo, PostgreSQL, ArchUnit, reactor y documentación quedan verdes.

## Secuencia

1. fijar ADR, permisos, contextos, comandos y resultados;
2. implementar consultas/adaptadores públicos;
3. implementar comandos y definiciones;
4. agregar límite transaccional y auditoría;
5. validar seguridad negativa, PostgreSQL, arquitectura y reactor.

## Resultado

Los once criterios quedaron satisfechos. El descriptor publica exactamente los
cuatro permisos acordados; comandos, consultas, definiciones y contratos públicos
permanecen acotados por empresa; las mutaciones usan versión esperada, secuencia y
auditoría técnica; CDI/JTA aporta el límite transaccional sin abrir REST, JSF ni
menú.

La evidencia reproducible, comandos, conteos, incidencias corregidas y límites que
continúan pendientes están en [Evidencia J11-S7-04](../../evidence/J11-S7-04-aplicacion-seguridad-commercial-catalog.md).

`J11-S7-05` puede comenzar el directorio, alta y ficha visual sobre estos casos de
uso reales. El plugin todavía no está compuesto en el WAR y no existe una nueva
demo visual del catálogo en este corte.
