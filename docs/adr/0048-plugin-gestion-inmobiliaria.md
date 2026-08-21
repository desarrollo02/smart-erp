# ADR-0048 — Plugin de gestión inmobiliaria

- Estado: Aceptado para planificación; alcance detallado e implementación no autorizados
- Fecha: 2026-08-15
- Decisión de producto: incorporar un plugin vertical reutilizable para gestión inmobiliaria
- Plugin propuesto: `real_estate`
- Fuente legado preferente: repositorio `C:\cosme\mega\miaterra`, rama
  `miaterra_master`, raíz `fuente/tag`
- Épica: [Gestión inmobiliaria](../backlog/epica-gestion-inmobiliaria.md)

## Contexto

El catálogo planificado no contiene todavía un plugin propietario del dominio
inmobiliario. La rama legado `miaterra_master` contiene un paquete `inmuebles` y
recorridos para proyectos, fracciones, lotes, edificios, alquileres, catastro,
mejoras, etapas, documentos, costos y presupuestos. También aparecen cruces con
ventas, contratos, cuentas por cobrar, tesorería, obras y datos geográficos.

Copiar ese módulo como una unidad preservaría acoplamientos que contradicen la
arquitectura de Smart ERP. Absorberlo en Inventario también sería incorrecto: un
lote o una unidad inmobiliaria posee identidad, situación jurídica, ubicación,
etapas y disponibilidad comercial propias; no es una existencia fungible en un
depósito.

## Decisión

### 1. Identidad, clasificación y conteo

Se agrega `real_estate` como plugin funcional vertical, reutilizable, opcional y
activable por empresa.

- No recibe el orden ERP 20 ni renumera la secuencia ERP 1–19.
- El catálogo global planificado aumenta de treinta y tres a treinta y cuatro
  plugins reutilizables.
- Su inclusión física dependerá de perfiles de composición inmobiliarios futuros;
  no se agregará a todas las distribuciones por defecto.
- Este ADR no crea módulos Maven, API, descriptor, esquema, migraciones,
  permisos, pantallas ni composición ejecutable.
- El trabajo activo de Sprint 10 y la precedencia de `sales` no cambian.

### 2. Frontera candidata

La primera caracterización deberá validar si `real_estate` será propietario de:

- proyectos o desarrollos inmobiliarios, etapas y fracciones;
- inmuebles, terrenos, lotes, edificios y unidades inmobiliarias;
- identidad catastral y registral, ubicación y atributos físicos;
- mejoras, documentos y evidencias propios del inmueble;
- estados y disponibilidad inmobiliaria con historia auditable;
- presupuestos y costos estimativos estrictamente inmobiliarios;
- relaciones entre proyecto, unidad y situación comercial mediante IDs públicos;
- inbox/outbox, idempotencia y proyecciones privadas del dominio.

Esta lista es una hipótesis de planificación, no un modelo aprobado. `RE-00` debe
confirmar términos, cardinalidades, ciclos, perfiles de negocio y datos reales
antes de diseñar la API o persistencia.

### 3. Propietarios que no se trasladan

La caracterización debe conservar estas fronteras:

- `business_partners` posee personas y organizaciones; Inmobiliaria referencia
  propietarios, clientes, vendedores, contratistas o administradores por ID y
  conserva snapshots sólo cuando la historia lo exige;
- `reference_data` publica referencias normativas compartidas; los catálogos
  empresariales específicos deben tener propietario y ruta de administración;
- `commercial_catalog` conserva productos, servicios, unidades comerciales,
  impuestos y precios maestros que no sean identidad del inmueble;
- `inventory` conserva artículos, depósitos, saldos y movimientos físicos; no
  almacena lotes inmobiliarios como stock;
- `purchasing` conserva solicitudes, órdenes, recepciones y devoluciones;
- `sales` conserva presupuestos, pedidos y compromisos de venta canónicos;
- `commercial_documents` conserva facturas, notas y snapshots comerciales;
- `treasury` conserva dinero y conciliación, `accounts_receivable` la deuda y
  cobranza, y `accounting` los asientos y mayores;
- un futuro dominio de obras o construcción deberá conservar su ejecución
  técnica; no se presumirá dentro de `real_estate` por existir referencias en el
  legado.

Reservas, contratos inmobiliarios, alquileres, cronogramas, mora, comisiones,
presupuestos de obra y administración de propiedades quedan como decisiones
explícitas de `RE-00`. No se asignan por semejanza con una tabla o pantalla legado.

### 4. Fuente legado y reproducibilidad

La fuente de conocimiento preferente para este módulo es la rama
`miaterra_master` del repositorio de solo lectura `C:\cosme\mega\miaterra`, cuya
raíz de código es `fuente/tag`.

El 2026-08-15 la referencia se verificó en el commit completo
`7dd043230efcb2d6b0a9855855acad7d9aaf5faa`. Como la rama puede avanzar, `RE-00`
debe registrar el commit completo que realmente analice, fecha, rutas, consultas
y evidencia. La investigación debe usar Git en modo de solo lectura, sin cambiar
la rama de trabajo del legado ni modificar archivos, y convertir lo observado en
requisitos, casos de uso, decisiones y pruebas de caracterización. No se copiarán
clases, entidades, XHTML, SQL ni dependencias `javax.*`.

### 5. Secuencia de planificación

`real_estate` queda fuera de la secuencia ERP numerada. Su implementación sólo
podrá priorizarse después de completar `RE-00`, aceptar las decisiones pendientes
y estabilizar las APIs públicas que su perfil aprobado necesite.

Como mínimo, el núcleo del inmueble requerirá contratos estables de
`business_partners` y `reference_data`. Los recorridos de comercialización,
facturación, cuotas, cobros o contabilidad no podrán implementarse con modelos
provisionales: esperarán las APIs propietarias correspondientes o se separarán en
fases explícitas.

### 6. Interfaz, seguridad y datos

La UI futura usará Jakarta Faces 4.1, Material Design 3, contratos neutrales y los
rangos 375/720/1280 px. Mapas, planos, archivos o visualizaciones geográficas no
autorizan JavaScript remoto ni una biblioteca sin evaluación de versión, licencia,
seguridad, accesibilidad, operación offline y ADR cuando corresponda.

Toda operación revalidará empresa, plugin efectivo, actor, permiso, estado y
versión en el servidor. Propietarios, documentos, coordenadas, precios, contratos
y evidencia podrán contener datos sensibles; `RE-00` debe clasificar acceso,
retención, exportación, auditoría y borrado permitido antes de persistirlos.

## Decisiones pendientes RE-D01 a RE-D12

| ID | Decisión requerida |
|---|---|
| RE-D01 | perfiles V1: desarrolladora, loteadora, inmobiliaria, alquileres, administración u otros |
| RE-D02 | vocabulario canónico e identidades de proyecto, inmueble, fracción, lote, edificio y unidad |
| RE-D03 | datos catastrales/registrales, fuente, vigencia, validación y jurisdicción |
| RE-D04 | ciclos de estado, disponibilidad, reserva y concurrencia sobre una unidad |
| RE-D05 | propiedad de mejoras, etapas, documentos, verificaciones y evidencias |
| RE-D06 | límite entre unidad inmobiliaria, Catálogo e Inventario físico |
| RE-D07 | propietario de reserva, oportunidad, presupuesto, pedido y contrato de venta |
| RE-D08 | propietario de alquiler, contrato, garantía, reajuste, ocupación y liquidación |
| RE-D09 | integración de cuotas, mora, cobranza, tesorería y contabilidad |
| RE-D10 | límite de costos, presupuestos, compras, contratistas y futura gestión de obras |
| RE-D11 | archivos, planos/mapas, datos personales, permisos, retención y exportación |
| RE-D12 | migración desde `miaterra_master`, calidad, reconciliación, corte y perfiles físicos |

Ninguna decisión se considera resuelta por aceptar este ADR.

## Consecuencias

### Positivas

- el dominio inmobiliario obtiene identidad y dueño explícitos;
- se evita modelar inmuebles como stock o deuda comercial;
- la rama legado queda identificada de forma reproducible y de solo lectura;
- ventas, cobros, tesorería, contabilidad y futuras obras conservan sus fuentes de
  verdad;
- un perfil inmobiliario puede componerse sin obligar a otras empresas a instalar
  el plugin.

### Costes y riesgos

- el legado mezcla varias capacidades y puede requerir más de un plugin después
  de `RE-00`;
- catastro, contratos y alquileres pueden variar por jurisdicción y exigir
  adaptadores normativos;
- disponibilidad, reserva y venta concurrentes requieren consistencia e
  idempotencia estrictas;
- planos, documentos y datos de propietarios elevan riesgos de privacidad,
  almacenamiento y autorización;
- implementar antes de estabilizar Ventas y Finanzas produciría duplicación o
  contratos provisionales.

## Alternativas descartadas

### Tratar inmuebles como artículos de Inventario

Se descarta porque depósito, saldo y movimiento físico no representan identidad
catastral, ubicación, historia, mejora o situación jurídica de una propiedad.

### Copiar el módulo `inmuebles` del legado

Se descarta porque sus cruces deben convertirse primero en requisitos y contratos
públicos; además introduciría diseño y dependencias incompatibles con Jakarta EE
11 y los límites de plugins.

### Incorporar toda la operación inmobiliaria en Ventas

Se descarta porque Ventas no debe poseer catastro, proyectos, lotes, edificios,
mejoras ni documentación propia del inmueble.

## Gates antes de implementar

- [ ] ejecutar `RE-00` contra un commit completo congelado de `miaterra_master`;
- [ ] aceptar RE-D01 a RE-D12 y decidir si un solo plugin sigue siendo suficiente;
- [ ] aprobar dominio, contratos públicos, esquema y perfiles físicos;
- [ ] inventariar selectores, propietarios, rutas de administración e inactivos;
- [ ] definir seguridad, privacidad, archivos, mapas y retención;
- [ ] caracterizar migración, conciliación y reversibilidad sin escribir el legado;
- [ ] confirmar dependencias requeridas/opcionales y ausencia de ciclos;
- [ ] aprobar una iteración propia sin desplazar silenciosamente ADR-0011;
- [ ] definir matriz Maven, ArchUnit, PostgreSQL/Testcontainers, JTA, Docker,
  seguridad negativa y Playwright responsive.

## Referencias

- [ADR-0002 — Arquitectura de plugins](0002-arquitectura-plugins.md)
- [ADR-0011 — Roadmap de plugins productivos](0011-roadmap-dependencias-plugins-productivos.md)
- [ADR-0013 — Eventos de integración y outbox](0013-eventos-integracion-outbox-por-plugin.md)
- [ADR-0016 — Autorización y auditoría](0016-autorizacion-y-auditoria-operaciones-plugin.md)
- [ADR-0028 — Gobierno de selectores](0028-gobierno-de-selectores-y-datos-administrables.md)
- [Épica — Gestión inmobiliaria](../backlog/epica-gestion-inmobiliaria.md)
