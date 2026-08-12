<article>
  <div class="page-footer">LogixOne · Manual del Panel de demostración · edición 2026-08-11 · página <span class="page-number"></span></div>
  <header class="cover">
    <div class="eyebrow">Manual de usuario por módulo</div>
    <h1>Panel de demostración</h1>
    <p class="subtitle">Recorrido de la pantalla de referencia que demuestra composición, autorización y actualización segura de una vista.</p>
    <div class="meta"><strong>Versión documentada:</strong> plugin fixture <code>reference_plugin</code>, baseline Sprint 8.<br><strong>Audiencia:</strong> usuarios de demostración, soporte e implementación.<br><strong>Alcance:</strong> capacidad técnica demostrativa; no procesa operaciones comerciales.</div>
  </header>

  <section class="toc">
    <h2>Antes de comenzar</h2>
    <p>Este módulo existe como plugin de referencia para probar que un plugin físicamente presente y activo puede aportar menú, permiso, pantalla y migración sin acoplarse al kernel.</p>
    <table><thead><tr><th>Requisito</th><th>Descripción</th></tr></thead><tbody><tr><td>Empresa activa</td><td>La sesión debe tener un contexto empresarial seleccionado.</td></tr><tr><td>Plugin activo</td><td><code>reference_plugin</code> debe estar habilitado para esa empresa.</td></tr><tr><td>Permiso</td><td><code>reference.dashboard.view</code>.</td></tr></tbody></table>
    <h3>Glosario</h3>
    <dl class="term-grid"><dt>Plugin de referencia</dt><dd>Ejemplo controlado usado para verificar contratos técnicos del producto.</dd><dt>Resumen</dt><dd>Texto solicitado por el usuario para actualizar el contenido visible; no se guarda como transacción de negocio.</dd><dt>Saludo</dt><dd>Mensaje calculado por la pantalla usando el contexto autorizado.</dd><dt>Actualizar</dt><dd>Acción Faces que vuelve a calcular la presentación.</dd><dt>Fixture de migración</dt><dd>Fila técnica que demuestra que la migración del plugin se aplicó; no es un dato editable desde el panel.</dd></dl>
  </section>

  <section class="screen" data-screen="reference-dashboard">
    <div class="screen-title"><h2>1. Panel de demostración</h2><span class="route">/faces/reference</span></div>
    <p><strong>Objetivo:</strong> comprobar visualmente que el plugin está compuesto, activo y autorizado, y que una acción de pantalla actualiza su contenido.</p>

    <h3>Bosquejo orientativo de la pantalla</h3>
    <div class="wireframe">┌ Panel de demostración ────────────────────────────────────────┐
│ Saludo: Bienvenido a la capacidad de referencia                 │
│                                                                 │
│ Resumen [ escriba un texto para la demostración              ]  │
│                                                   [ Actualizar ] │
│ Resultado actualizado: …                                        │
└─────────────────────────────────────────────────────────────────┘</div>

    <h3>Términos, datos y controles</h3>
    <table><thead><tr><th>Dato o control</th><th>Significado y formato</th><th>Efecto</th></tr></thead><tbody>
      <tr><td>Saludo</td><td>Texto informativo de solo lectura.</td><td>Confirma que la vista y su modelo fueron resueltos.</td></tr>
      <tr><td>Resumen</td><td>Texto libre de demostración. No introduzca secretos ni datos personales.</td><td>Entrada temporal de la vista; no crea una entidad de negocio.</td></tr>
      <tr><td>Actualizar</td><td>Botón de acción.</td><td>Reprocesa el resumen y actualiza la región de resultado.</td></tr>
      <tr><td>Resultado</td><td>Mensaje visible calculado tras la actualización.</td><td>Permite verificar interacción y retroalimentación de la UI.</td></tr>
    </tbody></table>

    <h3>Recorrido recomendado</h3>
    <ol><li>Abra <strong>Panel de demostración</strong> desde el menú.</li><li>Compruebe que aparece el saludo.</li><li>Escriba un resumen breve y no sensible.</li><li>Pulse <strong>Actualizar</strong>.</li><li>Verifique que el resultado cambió sin salir de la pantalla.</li></ol>
    <p class="success"><strong>Resultado esperado:</strong> la pantalla permanece disponible y presenta la respuesta actualizada.</p>
    <p class="warning"><strong>Si no aparece:</strong> compruebe empresa activa, activación del plugin y permiso. Si el menú existe pero la ruta rechaza el acceso, registre empresa, usuario ficticio/rol, ruta, hora y correlación para soporte.</p>

    <h3>Diagrama de datos y tablas afectadas</h3>
    <div class="db-diagram">
      <div class="db-row"><div class="db-name">Kernel / contratos</div><div>Sesión, empresa activa, activación del plugin, contribución de menú y autorización.</div><div class="crud">EXT</div></div>
      <div class="db-row"><div class="db-name">migration_fixture</div><div><code>fixture_key</code> (PK) y <code>created_at</code>. Prueba que la migración del plugin se ejecutó.</div><div class="crud">—</div></div>
      <p class="relation"><strong>Flujo real:</strong> navegador → pantalla Faces → modelo del plugin → contratos públicos del kernel. El texto del resumen permanece en la vista; la pantalla no consulta ni modifica <code>migration_fixture</code>. No se encontraron FK, vistas, funciones ni triggers adicionales en el esquema del plugin.</p>
    </div>
  </section>

  <section><h2>Límites y soporte</h2><ul><li>No use este panel como registro de notas: el texto no es persistencia funcional.</li><li>No demuestra reglas de ventas, inventario o facturación.</li><li>Funciona en ancho compacto, medio y expandido; el control y botón deben reordenarse sin desplazamiento horizontal normal.</li><li>Canal de soporte: mesa interna del proyecto. No envíe tokens, contraseñas ni datos reales.</li></ul></section>
</article>
