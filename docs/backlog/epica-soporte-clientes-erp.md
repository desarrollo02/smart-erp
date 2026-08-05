# Épica — Soporte a clientes del ERP

- Estado: Planificada como plugin funcional `customer_support`
- Fecha: 2026-08-04
- Perfil: operaciones centrales del proveedor
- Decisión: [ADR-0036](../adr/0036-operaciones-proveedor-soporte-lanzamientos-conector.md)
- Prioridad: gate previo a ofrecer soporte integrado; no modifica Sprint 8

## Objetivo

Atender de manera segura y auditable a las empresas que compren o alquilen
Logixone, desde la apertura de un caso hasta su resolución, conservando cobertura,
SLA, comunicaciones, instalación afectada y relación con una corrección publicada.

## Alcance inicial

- clientes y contactos por referencias de `business_partners`;
- cobertura de soporte y snapshot del SLA aplicable;
- instalaciones, ambientes y versión declarada sin credenciales;
- consultas, incidentes, solicitudes y problemas;
- impacto, urgencia, prioridad, cola, asignación y escalación;
- conversaciones, adjuntos seguros, solución y satisfacción;
- solicitudes/resultados de diagnóstico del conector;
- artículos de conocimiento;
- portal del cliente y consola de agentes responsive.

## Límites

- no poseer personas, organizaciones, facturas, cobros o suscripciones;
- no leer tablas de `business_partners`, `recurring_billing` o releases;
- no almacenar credenciales de las instalaciones;
- no ejecutar comandos ni administrar remotamente un ERP;
- no convertir el plugin en CRM de ventas, marketing o prospección;
- no habilitar contactos externos mediante autoridad global o excepciones de
  aislamiento.

El esquema previsto es `plg_customer_support`. `business-partners-api` será una
dependencia requerida. La integración con `release-management-api` y contratos
comerciales será opcional y unidireccional.

## Historias propuestas

| Historia | Resultado |
|---|---|
| SUP-00 | confirmar identidad del portal, cobertura, SLA, horarios, privacidad, retención, adjuntos y canales |
| SUP-01 | crear `customer-support-api`, descriptor, dominio neutral e IDs públicos |
| SUP-02 | crear esquema privado, migraciones, repositorios, historial y concurrencia |
| SUP-03 | implementar casos, colas, SLA, asignación, escalación, permisos y auditoría |
| SUP-04 | implementar portal de cliente y consola de agente JSF Material 3 responsive |
| SUP-05 | integrar solicitudes de cambio y eventos de release mediante contratos públicos |
| SUP-06 | integrar protocolo central del conector, adjuntos y diagnósticos sanitizados |
| SUP-07 | componer perfil del proveedor, ejecutar gates, demo, manuales, runbook y PDF |

## Criterios de aceptación

- **SUP-CE01:** cada caso pertenece al contexto empresarial del proveedor y a un
  cliente/contacto autorizado, sin consulta global implícita.
- **SUP-CE02:** el SLA aplicado queda como snapshot y cambios posteriores no
  reescriben el vencimiento histórico.
- **SUP-CE03:** el cliente sólo consulta y comenta sus propios casos; agentes y
  supervisores requieren permisos distintos.
- **SUP-CE04:** impacto, urgencia, prioridad y estado son reglas cerradas,
  explicables y auditadas.
- **SUP-CE05:** adjuntos validan tipo real, tamaño, nombre, contenido malicioso,
  autorización, descarga y retención.
- **SUP-CE06:** repetir un mensaje o transición no duplica conversación,
  escalación, cambio presentado ni notificación.
- **SUP-CE07:** un diagnóstico conserva solicitud, consentimiento, categorías,
  checksum y resultado sin exponer secretos.
- **SUP-CE08:** soporte funciona sin `release_management` y sin
  `support_connector`; esos plugins agregan capacidades opcionales.
- **SUP-CE09:** presentar un defecto usa la API pública de releases y nunca escribe
  su esquema.
- **SUP-CE10:** desactivar o retirar el plugin conserva casos, adjuntos, SLA e
  historial.
- **SUP-CE11:** portal y consola cubren 375, 720 y 1280 px, teclado, foco, vacío,
  error, acceso denegado y adjunto rechazado.
- **SUP-CE12:** logs y auditoría no contienen mensajes completos, adjuntos, datos
  personales innecesarios, tokens ni credenciales.

## Decisiones pendientes antes de código

- modelo OIDC de contactos externos y recuperación de cuenta;
- relación entre cliente comercial, contrato, instalación y empresa operativa;
- horarios, calendarios, pausas y escalación exacta de SLA;
- residencia, cifrado, antivirus, tamaño y retención de adjuntos;
- canales de notificación y fuente de verdad si se integra correo;
- datos mínimos visibles para un agente y auditoría de lecturas sensibles.

No se inicia esta épica durante Sprint 8. Su Sprint futuro debe comenzar por
SUP-00 y resolver las decisiones de identidad y datos antes de crear persistencia.

