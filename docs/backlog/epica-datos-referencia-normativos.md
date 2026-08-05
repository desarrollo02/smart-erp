# Épica — Datos de referencia normativos compartidos

- Estado: RD-05 implementado localmente; gates runtime y recongelación pendientes
- Fecha: 2026-08-05
- Plugin: `reference_data`
- API pública: `reference-data-api`
- ADR rector: [ADR-0038](../adr/0038-plugin-datos-referencia-normativos.md)

## Objetivo

Proveer países, monedas y futuras referencias normativas mediante publicaciones
inmutables, verificables y reutilizables, sin convertir el kernel en maestro ni
permitir que cada plugin mantenga listas paralelas.

## Orden de construcción

| Corte | Alcance | Estado |
|---|---|---|
| RD-00 | decisión, fuentes, licencias, contrato, amenazas y criterio de completitud | Aceptado |
| RD-01 | API Java pura, descriptor, esquema, migración y subconjunto `PY/PYG/USD` | Implementado y validado |
| RD-02 | pantalla de procedencia y consulta; permisos y seguridad negativa | Implementado y validado, incluido Playwright responsive |
| RD-03 | consumo transaccional desde `business_partners` y `commercial_catalog` | Implementado y validado |
| RD-04 | anulación por empresa, historia y auditoría | Implementado; gates runtime pendientes en J11-S8-C06 |
| RD-05 | importador reproducible, publicaciones completas, `N.A.` opcional y listas grandes paginadas | Implementado; gates runtime pendientes en J11-S8-C07 |
| RD-06 | PostgreSQL, composición, Docker, Playwright, documentación y recongelación | Gates del corte mínimo verdes; recongelación formal pendiente |

RD-01 a RD-03 forman el corte mínimo recomendado antes de iniciar `purchasing`.
RD-04 y RD-05 no pueden presentarse como terminados sólo por la existencia del
esquema: requieren los gates runtime y la recongelación formal.

## Criterios de aceptación del corte mínimo

- `reference_data` es descubierto como plugin funcional 1.0.0;
- declara esquema `plg_reference_data`, capacidades, permiso y pantalla neutral;
- cada publicación registra autoridad, URI, fecha, SHA-256, alcance y cantidad;
- el subconjunto inicial permanece identificado como incompleto e histórico;
- `reference-data-api` no depende de Jakarta ni de implementaciones;
- países y monedas se consultan por `CompanyId` y código estable;
- la pantalla muestra fuente y alcance sin permitir códigos arbitrarios;
- `business_partners` y `commercial_catalog` usan únicamente el contrato público;
- ambos revalidan el código en la transacción de alta;
- la composición física incluye el proveedor antes de los consumidores;
- no existen relaciones JPA, SQL ni imports internos entre plugins;
- pruebas unitarias, ArchUnit, migración PostgreSQL, WAR presente/ausente, Docker y
  Playwright quedan verdes antes de recongelar Sprint 8.

## Gates para una publicación completa

1. fuente primaria y licencia documentadas;
2. original conservado en `.tools/downloads/reference-data/` con tamaño y hash;
3. parser determinista y sin acceso de red en runtime;
4. unicidad de códigos y cardinalidad esperada;
5. diferencias clasificadas como alta, cambio o retiro;
6. conservación de ediciones anteriores y referencias históricas;
7. política de traducciones separada de la identidad normativa;
8. rollback y repetición idempotente verificados;
9. revisión humana antes de marcar la publicación `CURRENT`;
10. manuales sin afirmaciones de certificación no demostradas.

## Fuera del primer corte

- subdivisiones ISO 3166-2;
- tipos de identificación empresariales, que permanecen en
  `business_partners`;
- catálogos fiscales SIFEN, propiedad del adaptador fiscal correspondiente;
- tasas de cambio, propiedad futura de tesorería o servicio financiero;
- traducción completa de nombres oficiales;
- actualización automática desde internet;
- redistribución de colecciones sujetas a suscripción.

## Evidencia del corte actual

Los artefactos observados el 2026-08-04 permanecen fuera de Git en
`.tools/downloads/reference-data/`. Sus hashes y tamaños están registrados en
ADR-0038. El repositorio versiona la migración determinista derivada y sus
metadatos, no los archivos fuente descargados.

El corte ejecutable ya incluye `reference-data-api`, `reference-data`, su V1
privada con cinco tablas, la pantalla `/reference-data`, validación transaccional
de país/moneda y dependencia `REQUIRED` 1.x desde socios y catálogo. Las pruebas
de módulo, PostgreSQL 18.4, arquitectura, composición presente/ausente,
Docker/Compose, health/OIDC y Playwright responsive quedaron verdes. La publicación
RD-04 agrega localmente V2, política optimista, historia, auditoría y administración
neutral; PostgreSQL/Docker/Playwright aún deben validarlo. RD-05 agrega V3–V4,
`reference-data-api` 1.1.0, 248 países, 178 códigos únicos de moneda o fondo, 13
unidades menores `N.A.` como ausencia y búsqueda paginada en servidor con máximo
50. Sus pruebas focales están verdes, pero PostgreSQL/Compose/Playwright y la
recongelación documental siguen pendientes, por lo que Sprint 8 permanece abierto.
