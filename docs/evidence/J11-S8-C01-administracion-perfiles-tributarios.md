# Evidencia J11-S8-C01 - Administración de perfiles tributarios

- Fecha: 2026-08-01
- Estado: implementación y gates técnicos afectados verdes; cierre derivado pendiente
- Historia: [J11-S8-C01](../sprints/sprint-08/J11-S8-C01-administracion-perfiles-tributarios.md)
- Demo: [runbook reproducible](../runbooks/demo-perfiles-tributarios-j11-s8-c01.md)
- Perfil físico: `with-inventory-demo`

## Resultado ejecutivo

`commercial_catalog` aporta ahora **Perfiles tributarios** como octava función del
menú fusionado. Un usuario con `commercial_catalog.definitions.manage` puede
buscar, registrar y consultar perfiles internos de la empresa activa. El perfil
nuevo aparece inmediatamente en el selector de un artículo sin reiniciar WildFly.

El corte no incorpora tasas oficiales, reglas de determinación ni códigos SIFEN.
La pantalla declara explícitamente que la correspondencia fiscal será
responsabilidad de un plugin fiscal posterior.

## Implementación verificada

- nuevo contrato neutral y ruta `/catalog/tax-profiles`;
- capability y menú declarados por el plugin, sin enlace escrito manualmente en el
  shell;
- handler CDI que revalida empresa, plugin y `definitions.manage` en cada carga y
  alta;
- consulta transaccional de definiciones existentes y alta mediante el caso de uso
  auditado;
- renderer Jakarta Faces/Material Design 3 reutilizado por el shell;
- fixture idempotente con perfiles general, reducido y exento inequívocamente
  ficticios;
- patrón de continuidad documentado para unidades, categorías, marcas, etiquetas
  y familias de variantes.

## Gates de código y composición

Con JDK 21.0.11+10 y Maven Wrapper 3.9.16 se ejecutaron:

```powershell
.\mvnw.cmd -B -pl plugins/commercial-catalog -am test
.\mvnw.cmd -B -pl web-shell -am test
.\mvnw.cmd -B verify
.\mvnw.cmd -B -Pwith-inventory-demo verify
```

Los dos reactores completos terminaron con código 0 y 24/24 módulos. El plugin de
catálogo ejecutó 47 pruebas y el shell 23; ArchUnit ejecutó 24 escenarios verdes.
La construcción Docker en modo `verified` repitió el reactor del perfil completo y
terminó verde.

El primer comando focal fue detenido por Maven Enforcer porque la consola apuntaba
al JDK 8 del sistema; se configuró el JDK 21 local antes de compilar. Otro intento
focal no ejecutó la prueba solicitada porque PowerShell interpretó una propiedad
Maven sin comillas; se repitió con argumentos citados y quedó verde. Ninguno de
esos dos incidentes se contabiliza como fallo del producto.

## Runtime, datos y salud

La imagen `logixone/app:j11-s8-c01-candidate` tiene ID local
`sha256:dc0371593ba637e602a3bb543ce15aff5679116f65836d2dc2f91f8c57a2ce2c`
y 500.849.886 bytes. Se recreó únicamente `app`; PostgreSQL, Keycloak y sus
volúmenes no se eliminaron ni recrearon.

Liveness y readiness respondieron HTTP 200/`UP`. Readiness informó `catalog`,
`configuration`, `database`, `migrations` y `oidc-configuration` en `UP`. Los tres
contenedores quedaron saludables y el scan de logs posterior encontró cero
coincidencias con `ERROR`, `SEVERE`, `Exception` o `Caused by:`.

El fixture se aplicó a las dos empresas ficticias mediante transacciones e
`INSERT ... ON CONFLICT`, sin `DELETE`, `TRUNCATE`, `DROP` ni modificación de
volúmenes. Las revisiones ya existentes se conservaron. La suite añadió perfiles
visuales con sufijos únicos para probar el alta real.

## Playwright y revisión visual

El recorrido focal final ejecutó 1 prueba, 0 fallos, 0 errores y 0 omitidas. Validó:

1. activación del plugin y concesión de los cuatro permisos del catálogo;
2. menú fusionado de perfiles tributarios;
3. directorio con múltiples tratamientos internos;
4. alta y detalle de un perfil ficticio;
5. disponibilidad inmediata del perfil en el selector de un artículo;
6. continuidad del flujo de artículos, clasificación, precios y dependencia con
   inventario;
7. revocación temporal de `definitions.manage`, ocultamiento del menú, denegación
   de la ruta directa y restauración del permiso;
8. ausencia de overflow horizontal en 375, 599, 600, 720, 839, 840 y 1280 px.

La evidencia contiene 17 PNG y 2.184.965 bytes en
`docs/evidence/screenshots/J11-S8-C01/e2e/`. Se revisaron los originales del
directorio en los tres rangos, el alta compacta y la denegación compacta. No se
observaron recortes, controles perdidos, desbordamiento horizontal ni información
interna en la denegación.

Checksums representativos:

- directorio 1280 px: `5C7CE9AEF31FB9A059072533F306536BDBC1A6675A25FAFEBE8E36A921C04005`;
- directorio 720 px: `CB5591EF63A562B4CD0F67BC0B3F6B3A9D09176165C82701BD2DCD224E6678EE`;
- directorio 375 px: `B3DC3C6BFC71D5F06B5D28AD36E2E22238A9729C2DD21B5AD5F93FEDFBB95CCC`;
- alta 375 px: `250365624E9E4820168AFA9ABD12CAD923260D94448375EF847DB02F890895C2`;
- denegación 375 px: `D50AB247127177822277390A01E250FD7E3912A422A933EA4CC29FAEBEE5BB16`.

## Incidente de la prueba negativa

La primera versión de la prueba de revocación falló porque el locator buscaba el
texto `demo_operator` en la tarjeta completa y coincidía también con tres
formularios que incluían ese rol como opción. El fallo ocurrió antes de revocar el
permiso. Se acotó el locator al código mostrado en el encabezado de la tarjeta y se
repitió el recorrido completo; terminó verde y restauró el permiso.

## G0 documental

El control final recorrió 244 archivos Markdown y 941 enlaces locales. El resultado
fue cero errores de codificación UTF-8 y cero enlaces rotos.

## Pendientes que impiden recongelar y cerrar Sprint 8

1. regenerar y verificar el PDF obligatorio contra J11-S8-C01;
2. congelar los digests definitivos de aplicación y migrador;
3. regenerar el instalador Windows `current` contra esos digests y repetir sus
   gates afectados;
4. completar matriz Windows externa, Authenticode para distribución y G7 humano.

Hasta entonces la imagen es candidata, el instalador anterior es obsoleto, no se
promueven artefactos y no se inicia el siguiente plugin.
