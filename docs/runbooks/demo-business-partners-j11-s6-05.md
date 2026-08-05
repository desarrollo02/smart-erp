# Demo técnica de `business_partners` - J11-S6-05

- Estado: Histórico; sustituido por el runbook reproducible J11-S6-06
- Fecha: 2026-07-29
- Duración sugerida: 10–15 minutos
- Alcance: primera UI productiva; no cierra Sprint 6

## Qué demuestra

La demo muestra que un plugin funcional puede declarar menú, pantalla, inputs,
tabla, detalle y acciones sin aportar XHTML. El shell Jakarta Faces aplica Material
Design 3 y responsive; cada operación vuelve a validar empresa, activación, permiso
y versión antes de usar PostgreSQL/JTA y auditoría.

## Prerrequisitos

1. La composición local `logixone/app:j11-s6-05-ui-local` está levantada y el
   contenedor `logixone-app-1` aparece `healthy`.
2. PostgreSQL, Keycloak y sus volúmenes existentes permanecen levantados.
3. La Empresa autorizada 1 tiene `business_partners` activo.
4. El rol del usuario ficticio `demo.empresas.ab` posee los cuatro permisos del
   plugin.
5. La contraseña se lee del secreto local configurado; nunca se copia al guion,
   captura, terminal compartida o presentación.

Esta composición fue deliberadamente local. Para la imagen con perfil físico
único usar el [runbook J11-S6-06](demo-business-partners-j11-s6-06.md).

## Comprobación previa

```powershell
docker compose --env-file infra\compose\compose.env.local `
  -f infra\compose\compose.yaml ps

Invoke-WebRequest `
  http://localhost:18080/logixone/health/ready `
  -UseBasicParsing
```

Resultado esperado: aplicación, PostgreSQL y Keycloak saludables; readiness HTTP
200 y estado `UP`.

## Recorrido paso a paso

1. Abra `http://localhost:18080/logixone/faces/app/index.xhtml`.
   Explique que WildFly/OIDC protege el shell antes de mostrar una empresa.
2. Inicie sesión con `demo.empresas.ab` y el secreto local.
   No muestre ni dicte la contraseña.
3. Elija **Empresa autorizada 1** y pulse **Continuar**.
   Señale que el navegador propone una empresa, pero el servidor revalida la
   membresía.
4. Muestre **Funciones disponibles**. Deben verse **Socios comerciales** y el panel
   técnico de referencia. Explique que el menú depende de plugin efectivo y
   permiso actual.
5. Abra **Socios comerciales**. Destaque:
   - ID `business_partners:directory` y versión `1.0.0`;
   - búsqueda, registro y resultados reales;
   - slots públicos de directorio/detalle;
   - ausencia de includes XHTML del plugin.
6. Busque `BP-DEMO-001`. Debe aparecer **Cliente Demo S.A.** como activo.
7. Para demostrar un alta nueva, use un código ficticio único, por ejemplo
   `DEMO-AAAAMMDD-01`, tipo **Organización** y nombre **Cliente de demostración**.
   Pulse **Registrar** y muestre el aviso de confirmación.
8. Busque el código recién creado y pulse **Abrir** en su fila. Muestre identidad
   técnica separada del código visible, estado, roles, colecciones y versión `0`.
9. Pulse **Asignar cliente**. El resultado esperado es:
   - aviso **Rol cliente asignado**;
   - rol **Cliente · Activo**;
   - versión `1`;
   - la fila del resultado también refleja **Cliente**.
10. Explique que la acción exigió `business_partners.roles.manage`; ocultar el botón
    no sería suficiente. El handler vuelve a autorizar y la auditoría no guarda
    nombre, documento, dirección ni canales.

## Responsive y accesibilidad

Repita la pantalla en `375 × 900`, `720 × 900` y `1280 × 900`:

- compacto: bloques, tabla alternativa y acciones se apilan sin scroll horizontal;
- medio: búsqueda/registro y tarjetas usan dos columnas cuando cabe;
- expandido: tabla y formularios aprovechan el ancho manteniendo orden de lectura;
- en los tres: un solo `h1`, labels asociados, foco visible, estados comprensibles
  y acciones accesibles por teclado.

Las capturas de referencia están en
`docs/evidence/screenshots/J11-S6-05/e2e/`.

## Mensajes clave para la audiencia

- ya es una pantalla funcional sobre PostgreSQL, no un mock;
- el plugin es dueño de dominio, aplicación, esquema y handler;
- el shell es dueño de seguridad web, JSF, Material Design 3 y responsive;
- una personalización empresarial futura usa los slots/IDs públicos y no toca
  XHTML ni tablas privadas;
- retirar o desactivar el plugin no elimina sus datos.

## Limitaciones que deben declararse

- este guion conserva evidencia histórica de la composición efímera J11-S6-05;
- validación integral, demo final, retrospectiva y PDF pendientes de `J11-S6-07`;
- no existen todavía corrección histórica de identificaciones ni edición/baja de
  direcciones, canales o contactos;
- no se presenta como versión productiva ni como Sprint cerrado.

## Restauración y repetición

No borre tablas ni volúmenes. Los datos de demo son ficticios y pueden conservarse.
Para repetir el alta use otro código. Si necesita retirar un registro del uso
normal, use **Inactivar participante**; la historia se conserva. Al terminar, deje
el viewport en tamaño normal y cierre sesión.
