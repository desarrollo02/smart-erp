# ADR-0021 - Aplicación, autorización y auditoría de `commercial_catalog`

- Estado: Aceptado
- Fecha: 2026-07-30
- Historia: `J11-S7-04`
- Decisiones relacionadas: ADR-0016, ADR-0019 y ADR-0020

## Contexto

El catálogo ya tiene contratos públicos, dominio y persistencia privada, pero no
existe una capa autorizada que pueda invocar la futura interfaz. Exponer
repositorios o recibir empresa/permisos desde el navegador rompería el contexto
confiable. Además, la futura alta necesita consultar y administrar definiciones
controladas sin escribir directamente las tablas V1.

## Decisión

### Autorización

El descriptor declara cuatro permisos públicos:

1. `commercial_catalog.view`: buscar, consultar, convertir y cotizar;
2. `commercial_catalog.items.manage`: registrar/modificar ítems, asignaciones y su
   ciclo de vida;
3. `commercial_catalog.prices.manage`: administrar listas, entradas y sus estados;
4. `commercial_catalog.definitions.manage`: administrar unidades, categorías,
   marcas, etiquetas, perfiles tributarios y familias de variantes.

Cada llamada recibe una prueba neutral derivada de
`AuthorizedCompanyOperation`. Aplicación exige plugin y permiso exactos antes de
leer o mutar un repositorio. La empresa procede únicamente del contexto
autenticado revalidado por el kernel.

### Casos de uso

- directorio paginado, detalle, conversión y cotización por empresa;
- alta con código manual o secuencia transaccional;
- identidad, identificadores, unidades, clasificación, perfil, variante y ciclo
  de vida del ítem con versión esperada;
- lista, entrada de precio, inactivación y ciclo de vida con versión esperada;
- alta/listado controlado de definiciones necesarias para esos recorridos.

Los contratos públicos síncronos continúan pequeños y no representan por sí solos
una autorización de usuario. Los adaptadores entrantes obtendrán primero el
permiso exacto; los consumidores entre plugins serán autorizados por sus propios
casos de uso y conservarán snapshots.

### Auditoría y transacción

Toda mutación exitosa, sin cambio o rechazada registra `PLUGIN_OPERATION` mediante
`TechnicalAudit`, con actor, empresa, plugin, permiso, operación, tipo/ID técnico,
versiones, código estable y correlación. No se registran nombres, descripciones,
identificadores, importes, tasas ni valores de atributos.

El adaptador CDI define el límite JTA. Persistencia del plugin y auditoría central
confirman o revierten juntas. Las consultas usan transacción `SUPPORTS`.

## Alternativas descartadas

- Un único permiso `manage`: impide separar precios y definiciones maestras.
- Permisos específicos para cada botón: aumenta vocabulario sin una frontera de
  riesgo distinta en este primer contrato.
- SQL o fixtures desde la UI: omite autorización, validación, auditoría y versión.
- Publicar repositorios en `commercial-catalog-api`: filtra modelo privado.
- Auditar nombres, códigos escaneables o importes: agrega datos comerciales no
  necesarios a una capacidad transversal.

## Consecuencias

- J11-S7-05 podrá construir selectores y formularios sobre casos de uso reales.
- Ocultar una acción no autoriza; el servidor revalida cada interacción.
- Un plugin inactivo no obtiene una prueba válida del kernel y la aplicación
  además rechaza plugin/permiso incorrectos.
- No se agrega menú, pantalla, endpoint ni composición física en esta historia.

## Verificación

1. permiso/plugin/empresa incorrectos fallan antes del repositorio;
2. comandos usan versión y secuencia transaccional;
3. búsquedas, conversiones y precios permanecen aislados por empresa;
4. auditoría no contiene datos comerciales;
5. conflictos JPA/SQL se convierten a resultados estables;
6. PostgreSQL/JPA, seguridad negativa, ArchUnit y reactor quedan verdes.
