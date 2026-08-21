# Épica — Gestión inmobiliaria

- Estado: Planificada; RE-D01 a RE-D12 pendientes
- Plugin candidato: `real_estate`
- Clasificación: funcional vertical, reutilizable y opcional por empresa
- Prioridad: caracterizar y decidir; implementación sin autorizar
- Decisión: [ADR-0048](../adr/0048-plugin-gestion-inmobiliaria.md)
- Legado preferente: `C:\cosme\mega\miaterra`, rama `miaterra_master`, raíz
  `fuente/tag`, siempre en modo de solo lectura

## Objetivo

Administrar proyectos y unidades inmobiliarias con identidad, ubicación,
atributos, situación, documentación e historia propias, integrándose con los
plugins comerciales y financieros mediante contratos públicos sin duplicar sus
fuentes de verdad.

## Valor de negocio esperado

- conocer qué inmuebles y unidades existen, dónde están y cuál es su situación;
- organizar desarrollos, fracciones, etapas, lotes, edificios y mejoras;
- conservar catastro, documentos, verificaciones y cambios relevantes;
- ofrecer disponibilidad confiable a procesos comerciales sin vender dos veces
  la misma unidad;
- correlacionar ventas, alquileres, cuotas, cobros y costos sin trasladarlos a un
  esquema monolítico;
- migrar información del legado con trazabilidad y conciliación.

El valor y los perfiles exactos deben confirmarse en `RE-00`; esta épica no
presupone que desarrollo, corretaje, alquiler y administración entren juntos en
V1.

## Audiencias iniciales

| Actor | Necesidad por validar |
|---|---|
| responsable inmobiliario | administrar proyectos, unidades, estados y disponibilidad |
| catastro/documentación | mantener referencias, archivos, vigencia y verificaciones |
| comercial | consultar una unidad confiable y correlacionar el recorrido de venta o alquiler |
| administración/cobranzas | resolver contratos, cuotas y saldos desde el dominio propietario |
| dirección | observar disponibilidad, avance, costos y resultados con definiciones explícitas |
| auditoría/soporte | reconstruir cambios, documentos, actor, empresa y correlaciones |
| implementador/migración | mapear `miaterra_master` sin escribir tablas privadas ni copiar código |

## Alcance candidato sujeto a RE-00

- directorio y ficha de proyectos/desarrollos;
- fracciones, lotes, edificios y unidades con identidad estable;
- ubicación, atributos, tipo, uso, catastro y situación registral;
- etapas, mejoras, documentos, archivos y verificaciones;
- disponibilidad e historia de estado por empresa;
- costos y presupuestos estimativos propios del inmueble;
- consultas y proyecciones públicas para otros plugins;
- importación conciliada, idempotente y auditable desde el legado.

## Límites obligatorios

- no copiar clases, entidades, XHTML, SQL ni reglas por semejanza de nombres;
- no usar relaciones JPA, repositorios o joins hacia esquemas ajenos;
- no tratar un lote inmobiliario como existencia de `inventory`;
- no duplicar personas de `business_partners`, artículos de
  `commercial_catalog`, compras, presupuestos/pedidos de `sales`, facturas,
  dinero, deuda o asientos;
- no asumir que cronogramas, mora, contratos, alquileres, comisiones u obras
  pertenecen al plugin hasta aceptar RE-D01 a RE-D12;
- no modificar ni cambiar de rama el repositorio legado durante la investigación;
- no agregar mapas, visor, almacenamiento externo o librería visual sin evaluar
  licencia, seguridad, privacidad, accesibilidad y operación.

## Decisiones pendientes

Las decisiones RE-D01 a RE-D12 se definen en
[ADR-0048](../adr/0048-plugin-gestion-inmobiliaria.md). `RE-00` debe presentar
alternativas y evidencia para que producto las acepte, cambie o divida. Si la
caracterización demuestra dominios independientes —por ejemplo desarrollo,
alquiler o administración— deberá proponerse una familia con contratos y orden
interno propios mediante actualización del ADR; no se forzará un plugin único.

## Mapa de historias

| Orden | Historia | Entregable |
|---:|---|---|
| 1 | RE-00 | fuente congelada, caracterización, glosario, perfiles, RE-D01 a RE-D12 y mapa de propietarios |
| 2 | RE-01 | dominio neutral, ciclos, IDs y contratos públicos aprobados |
| 3 | RE-02 | esquema privado, migraciones inmutables, JPA y repositorios por empresa |
| 4 | RE-03 | aplicación, permisos, auditoría, concurrencia e idempotencia |
| 5 | RE-04 | integración pública mínima con participantes y referencias normativas |
| 6 | RE-05 | recorridos JSF Material 3, selectores gobernados y responsive |
| 7 | RE-06 | integraciones comerciales/financieras autorizadas por el perfil V1 |
| 8 | RE-07 | migración ensayada desde `miaterra_master`, conciliación y reversibilidad |
| 9 | RE-08 | composición, matriz integral, demo, manuales, PDF y decisión de instalador |

Las historias RE-01 a RE-08 son un bosquejo de planificación. RE-00 puede
dividirlas después de aceptar la frontera, pero no puede omitir sus resultados ni
adelantar código.

## Criterios de aceptación de la épica

- **CE-01:** cada empresa accede sólo a sus proyectos, unidades y documentos.
- **CE-02:** proyecto, inmueble y unidad usan identidades y cardinalidades
  aprobadas, no nombres heredados accidentalmente.
- **CE-03:** catastro y situación registral conservan fuente, vigencia y evidencia
  sin afirmar validez jurídica automática.
- **CE-04:** estados, reservas y disponibilidad son versionados, auditables y
  seguros ante concurrencia.
- **CE-05:** no existen FK, JPA, repositorios ni SQL hacia esquemas privados
  ajenos.
- **CE-06:** personas, artículos, compras, ventas, documentos, dinero, deuda y
  contabilidad permanecen en sus plugins propietarios.
- **CE-07:** `inventory` opera sin Inmobiliaria y no almacena unidades
  inmobiliarias como stock.
- **CE-08:** `real_estate` puede estar presente, ausente, activo o inactivo por
  empresa sin eliminar sus datos.
- **CE-09:** selectores declaran fuente/propietario y catálogos empresariales
  ofrecen administración autorizada, vacíos, inactivos y búsqueda escalable.
- **CE-10:** documentos y archivos aplican autorización, tamaño, tipo, malware,
  retención y minimización definidos.
- **CE-11:** importaciones identifican rama, commit, lote, checksum, mapeo,
  resultado y conciliación y son idempotentes.
- **CE-12:** errores parciales quedan recuperables; nunca se simula una venta,
  cobro, asiento o migración exitosa.
- **CE-13:** UI y operación funcionan con teclado, foco visible y sin overflow
  normal en 375, 720 y 1280 px.
- **CE-14:** composición WAR/migrador contiene exactamente el mismo conjunto de
  plugins y se prueba con `real_estate` presente y ausente.
- **CE-15:** demo, manuales y PDF distinguen capacidades implementadas,
  planificadas y pendientes de validación independiente.

## Matriz automatizada mínima futura

- dominio, estados, invariantes y concurrencia de disponibilidad;
- PostgreSQL/Testcontainers, Flyway, JPA `validate` e idempotencia;
- empresa ajena, permisos, plugin inactivo y datos sensibles;
- ArchUnit y cero acceso a implementaciones o esquemas ajenos;
- contratos públicos, eventos duplicados, desorden, reinicio y rechazo externo;
- migración repetible, cuarentena, conciliación y rollback de lote;
- archivos, límites, tipos permitidos y pruebas negativas;
- WAR/migrador con el plugin presente y ausente;
- Docker/Compose, health, persistencia y recreación;
- Playwright responsive, teclado, vacíos, errores e inexistencia de overflow.

## Dependencias y secuencia

- Requiere kernel vigente, `business-partners-api`, referencias normativas y los
  contratos transversales de autorización, auditoría y eventos.
- Dependencias de Ventas, Documentos, Tesorería, Cuentas por Cobrar, Contabilidad,
  Compras o un futuro plugin de obras se decidirán por perfil en `RE-00`.
- No cambia ERP 1–19 ni la prioridad de Sprint 10/Ventas.
- Ningún código se inicia hasta aceptar RE-D01 a RE-D12 y autorizar una iteración
  propia.

## Resultado esperado

Una composición inmobiliaria puede administrar una unidad y reconstruir su
historia sin convertir el plugin en dueño de clientes, stock, ventas, facturas,
cobros o asientos. Los datos migrados desde `miaterra_master` quedan vinculados a
un corte reproducible y conciliado.
