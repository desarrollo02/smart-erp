# COOP-00 — Gobierno, alcance y matriz normativa

- Estado: Refinada; ejecución pendiente de prioridad y datos de la cooperativa
- Fecha de refinamiento: 2026-08-04
- Tipo: gobierno, caracterización y decisiones; sin código
- Épica: [Cooperativa de ahorro y crédito](epica-cooperativa-ahorro-credito-paraguay.md)
- ADR rector: [ADR-0037](../adr/0037-familia-cooperativa-ahorro-credito-paraguay.md)
- Arquitectura candidata: [límites y dependencias](../architecture/cooperative-savings-credit-boundaries.md)
- Base de conocimiento: [alcance regulatorio inicial](../knowledge-base/cooperative-savings-credit/regulatory-scope-analysis.md)

## Objetivo

Convertir la intención de administrar una cooperativa paraguaya en un alcance
verificable, con fuentes oficiales congeladas, decisiones de producto aceptadas,
propietarios de datos, permisos, migración, pruebas y gates suficientes para
autorizar el primer Sprint de `cooperative_membership`.

COOP-00 no crea Java, POM, descriptor, migración, endpoint, pantalla ni perfil de
composición. Tampoco autoriza cargar datos reales, captar ahorros, conceder
créditos o afirmar conformidad regulatoria.

## Entradas obligatorias

La cooperativa o responsable de producto deberá aportar, mediante un canal
autorizado y sin versionar secretos:

1. razón social, número de registro INCOOP, sector, tipo/nivel y condición actual;
2. estatuto vigente y constancia de aprobación;
3. reglamentos de socios, aportes, ahorro, crédito y gobierno;
4. manual de crédito, niveles de aprobación, garantías y cobranza;
5. manual/matriz LA/FT, responsables y última revisión;
6. plan de cuentas institucional homologado y cierres recientes;
7. inventario de informes, formatos, usuarios y canales INCOOP/SEPRELAD;
8. productos vigentes con contratos, tasas, cargos, calendarios y redondeos;
9. sucursales, cajas, bancos, monedas, horarios y cierres operativos;
10. fuentes de migración, volúmenes, fecha de corte y diferencias conocidas.

Si una entrada contiene datos personales, saldos o credenciales, primero se
aprobará clasificación, acceso, enmascarado, retención y ubicación segura. No se
adjuntan bases productivas ni secretos a Markdown, Git, logs o evidencias.

## Decisiones de producto a cerrar

| ID | Pregunta | Recomendación inicial | Estado |
|---|---|---|---|
| COOP-D01 | ¿Especializada o multiactiva, y qué tipo/nivel aplica? | modelar primero la actividad real reconocida; no inferir por nombre | Pendiente |
| COOP-D02 | ¿Cómo se mapea la entidad al modelo multiempresa? | una persona jurídica cooperativa por `CompanyId`; sucursales dentro del mismo contexto salvo evidencia contraria | Pendiente |
| COOP-D03 | ¿Qué tipos/categorías de socio y reglas de admisión existen? | iniciar sólo con categorías respaldadas por estatuto y conservar vigencia | Pendiente |
| COOP-D04 | ¿Cómo funcionan aportes, cuotas, integración y devolución? | plan versionado y libro append-only; nunca columna de saldo editable | Pendiente |
| COOP-D05 | ¿Qué órganos, elecciones, quórum y plazos se aplican? | derivar de ley + estatuto; snapshot de padrón y actos inmutables | Pendiente |
| COOP-D06 | ¿Cuál será el primer producto de ahorro? | un único producto sencillo en PYG para el piloto, si la cooperativa lo confirma | Pendiente |
| COOP-D07 | ¿Qué tasa, base de días, redondeo, fecha valor y restricciones usa? | configuración versionada aprobada; no asumir ACT/365 ni fórmulas del legado | Pendiente |
| COOP-D08 | ¿Cuál será el primer producto de crédito? | un préstamo amortizable acotado, sin variantes no confirmadas | Pendiente |
| COOP-D09 | ¿Cómo se evalúa y aprueba el crédito? | límites explícitos, maker-checker/comité y excepción justificada | Pendiente |
| COOP-D10 | ¿Qué garantías y retenciones se incluyen? | comenzar sólo por garantías documentadas; ahorros se retienen vía API pública | Pendiente |
| COOP-D11 | ¿Cómo se liquidan depósitos, retiros, desembolsos y pagos? | tesorería como dueño de caja/banco; idempotencia y conciliación diaria | Pendiente |
| COOP-D12 | ¿Qué modelo, responsables y restricciones LA/FT están vigentes? | no abrir/desembolsar sin decisión vigente y procedimiento de escalamiento | Pendiente |
| COOP-D13 | ¿Qué plan, reportes, frecuencias y canales regulatorios aplican? | generar/validar artefactos antes de automatizar transmisión | Pendiente |
| COOP-D14 | ¿Qué se migra y cómo se corta? | importación idempotente, doble corrida, conciliación monetaria y rollback | Pendiente |
| COOP-D15 | ¿Qué canales/integraciones entran al primer perfil? | excluir portal, tarjetas, ATM y conectores específicos del núcleo inicial | Pendiente |

Una recomendación no equivale a decisión aceptada. Cada estado cambiará a
`Aceptada`, `Modificada` o `Descartada` con fecha, responsable y justificación.

## Entregables

| Entregable | Contenido mínimo | Gate |
|---|---|---|
| registro normativo congelado | URL, archivo, versión, vigencia, tamaño, SHA-256 y modificatorias | G0 |
| matriz de trazabilidad | requisito–fuente–plugin–dato–regla–permiso–prueba–responsable | G1 |
| glosario | socio, aporte, ahorro, crédito, mora, previsión, órgano y sinónimos | G1 |
| catálogo inicial de productos | condiciones versionadas y ejemplos aprobados | G1 |
| mapa de dominios/contratos | IDs, puertos, eventos, dependencias y ausencias | G2 |
| clasificación de datos | finalidad, sensibilidad, acceso, retención y enmascarado | G3 |
| matriz de permisos/segregación | actor, acción, monto/límite, creador y aprobador | G3 |
| estrategia contable | submayores, hechos, mapeos, cierres y conciliación | G4 |
| inventario de migración | fuentes, volúmenes, calidad, mapeo, cuarentena y rollback | G4 |
| estrategia de pruebas | dinero, concurrencia, seguridad, regulación, Docker y UI | G5 |
| plan de demo/piloto | datos ficticios, recorridos, restauración y aceptación | G5 |

## Secuencia de ejecución

### 1. Descubrimiento y custodia

- confirmar fuentes oficiales y documentos internos aplicables;
- descargar únicamente en `.tools/` y verificar checksums;
- registrar responsables y permisos de acceso;
- crear copia de trabajo sanitizada cuando corresponda;
- identificar documentos ausentes, contradictorios o vencidos.

### 2. Caracterización neutral

- entrevistar producto, socios, caja, crédito, contabilidad, riesgos,
  cumplimiento, auditoría y tecnología;
- describir recorridos actuales sin copiar tablas o pantallas como diseño;
- separar requisito legal, regla estatutaria, política interna y conveniencia;
- registrar volúmenes, cierres, excepciones, errores y recuperación.

### 3. Cierre de decisiones

- resolver COOP-D01 a COOP-D15;
- aprobar productos y ejemplos numéricos;
- congelar propietarios, IDs y dirección de dependencias;
- validar privacidad, segregación y retención;
- registrar divergencias entre operación actual y requisito aceptado.

### 4. Diseño de aceptación

- preparar casos normales, límites, negativos y rectificaciones;
- construir balances esperados para aportes, ahorro y crédito;
- definir conciliación submayor–tesorería–contabilidad;
- diseñar pruebas de reportes y artefactos con versión exacta;
- definir demo, piloto, recuperación y rollback.

### 5. Autorización del siguiente incremento

Sólo después de G0–G5 verdes, producto podrá programar la caracterización/diseño
detallado de `cooperative_membership`. La autorización deberá identificar Sprint,
alcance, datos permitidos y responsables. No se crea automáticamente por terminar
este documento.

## Gates

| Gate | Evidencia | Condición verde |
|---|---|---|
| G0 fuentes | registro y archivos oficiales/internos | identidad, vigencia y checksum confirmados |
| G1 producto | COOP-D01–D15 y trazabilidad | decisiones aceptadas y sin contradicciones bloqueantes |
| G2 arquitectura | grafo, contratos y propietarios | acíclico, sin tablas/JPA/DTO cruzados |
| G3 seguridad | datos, permisos, segregación y amenaza | responsables aprueban controles y negativos |
| G4 dinero/datos | contabilidad, conciliación y migración | ejemplos cuadran y rollback es posible |
| G5 entrega | pruebas, demo, documentación y operación | siguiente incremento estimable y verificable |

Un gate `Pendiente` no se interpreta como verde. Una fuente incompleta, una
decisión sin responsable o una diferencia monetaria sin explicación bloquea el
avance.

## Criterios de aceptación

- **COOP00-CA01:** país, sector, tipo/nivel y estatuto aplicable están confirmados.
- **COOP00-CA02:** cada fuente tiene versión, vigencia, checksum y modificatorias.
- **COOP00-CA03:** COOP-D01 a COOP-D15 tienen estado, responsable y justificación.
- **COOP00-CA04:** cada requisito se traza a plugin, dato, regla, permiso y prueba.
- **COOP00-CA05:** socios, ahorros, créditos, tesorería y contabilidad conservan
  propietarios distintos.
- **COOP00-CA06:** el grafo es acíclico y no contiene acceso privado cruzado.
- **COOP00-CA07:** productos iniciales incluyen ejemplos monetarios aprobados.
- **COOP00-CA08:** maker-checker, límites y excepciones están definidos.
- **COOP00-CA09:** LA/FT tiene responsables, decisión vigente y escalamiento.
- **COOP00-CA10:** informes/canales regulatorios tienen fuente y alcance exactos.
- **COOP00-CA11:** migración concilia importes, intereses, mora y garantías.
- **COOP00-CA12:** privacidad, retención, auditoría y logs están clasificados.
- **COOP00-CA13:** pruebas cubren redondeo, fechas, concurrencia, repetición,
  reverso, cierre, fallo parcial y rectificación.
- **COOP00-CA14:** demo/piloto usa datos ficticios y tiene recuperación.
- **COOP00-CA15:** no se crea código ni se afirma certificación durante COOP-00.

## Estado de preparación al 2026-08-04

| Elemento | Estado |
|---|---|
| familia y propietarios | Aceptados por ADR-0037 |
| mapa arquitectónico candidato | Documentado; pendiente de congelación |
| inventario oficial inicial | Documentado; descargas/checksums pendientes |
| datos de una cooperativa concreta | No aportados |
| COOP-D01–D15 | Pendientes |
| contratos de `treasury` y `accounting` | No implementados |
| autorización de Sprint | No otorgada |

Por estas condiciones COOP-00 está refinada pero no ejecutada. El siguiente paso
requiere información y responsables de una cooperativa concreta, además de una
prioridad futura de producto.
