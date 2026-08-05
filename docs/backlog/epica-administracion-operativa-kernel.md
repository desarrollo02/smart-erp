# Épica — Administración operativa segura del kernel

- Estado: Implementación y gates técnicos de Sprint 4 verdes; validación independiente G7 de la guía candidata pendiente
- Prioridad: Alta
- Inicio: Sprint 4
- Decisión base: [ADR-0009](../adr/0009-autoridad-administrativa-global-kernel.md)

## Propósito

Completar la frontera operativa inicial del kernel con una autoridad administrativa
global y pantallas Jakarta Faces seguras para administrar empresas, plugins,
personalizaciones, usuarios, membresías, roles y permisos sin SQL directo ni
privilegios inferidos desde Keycloak.

## Resultado de producto

Una persona autenticada y autorizada globalmente podrá:

- abrir un panel administrativo separado del workspace empresarial;
- registrar y cambiar el estado de empresas;
- consultar el catálogo físico de plugins;
- activar o desactivar plugins funcionales por empresa;
- asignar o reemplazar la personalización obligatoria de una empresa;
- administrar usuarios locales, membresías, roles y permisos empresariales;
- administrar autoridad global sin poder eliminar al último administrador;
- consultar auditoría segura y paginada;
- realizar estas operaciones en pantallas responsive Material Design 3 sobre JSF.

## Invariantes

1. Keycloak autentica; el kernel autoriza la administración global.
2. Un rol empresarial nunca concede autoridad global.
3. Toda mutación se ejecuta mediante un caso de uso tipado y transaccional.
4. Backing beans y XHTML no contienen reglas de negocio.
5. La empresa conserva exactamente una personalización exclusiva.
6. El panel no instala JAR ni modifica el catálogo físico en runtime.
7. No se puede revocar al último administrador global efectivo.
8. Toda mutación queda auditada sin secretos ni datos sensibles innecesarios.
9. Ocultar navegación nunca reemplaza la guarda del servidor.
10. Versiones obsoletas no sobrescriben cambios concurrentes.

## Fuera de alcance

- facturación, ventas, inventario u otro dominio ERP;
- API REST administrativa pública;
- instalación dinámica de plugins;
- editor libre de XHTML, CSS o JavaScript;
- gestión de contraseñas, MFA o federación desde Logixone;
- borrado automático de tablas o datos al desactivar un plugin;
- operación productiva, alta disponibilidad o promoción de imágenes.

## Hitos

1. autoridad global neutral y vocabulario de permisos;
2. migración `core` V4 y bootstrap del primer administrador;
3. persistencia JPA/JTA y casos de uso globales;
4. frontera web administrativa confiable;
5. administración visual de empresas, plugins y personalización;
6. administración visual de usuarios, membresías, roles y permisos;
7. auditoría y diagnóstico seguro;
8. validación acumulada y demo administrativa.

## Criterios de aceptación de la épica

- **CE-01:** una identidad sin permiso global no ve ni abre `/admin/*`.
- **CE-02:** roles de Keycloak y empresariales no conceden autoridad global.
- **CE-03:** el bootstrap global es one-shot, idempotente y cerrado por defecto.
- **CE-04:** no puede eliminarse el último administrador global efectivo.
- **CE-05:** empresas y activaciones se administran sin SQL directo.
- **CE-06:** reemplazar personalización conserva exclusividad y dependencias.
- **CE-07:** usuarios, membresías, roles y permisos respetan empresa y versiones.
- **CE-08:** las mutaciones quedan auditadas con el actor autenticado.
- **CE-09:** rutas y formularios manipulados fallan cerrados.
- **CE-10:** todas las pantallas son utilizables a 375, 720 y 1280 px.
- **CE-11:** la demo A/B continúa operativa sin regresiones.
- **CE-12:** la matriz acumulada queda verde antes de cerrar Sprint 4.

## Política temporal de pruebas

Por decisión de producto del 2026-07-28, `J11-S4-01` a `J11-S4-07` podrán quedar
`Implementada pendiente de pruebas`. El único pendiente aceptado al terminar cada
historia será su matriz automatizada; decisiones, código, migraciones y
documentación deben estar completos. `J11-S4-08` ejecutará el gate acumulado.

Una prueba ejecutada y fallida no se difiere: debe corregirse. La épica no se
considera completada con pruebas pendientes.
