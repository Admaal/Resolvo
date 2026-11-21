@AGENTS.md

Los hooks de Claude Code están en `.claude/settings.json` y llaman a los mismos scripts de `.cursor/hooks/` con `-Cliente claude`.
`.cursor/proteger-main` sigue siendo el interruptor de la protección de `main`.

## Diseño frontend

Dirección: **cálido editorial**, claro por defecto y con tema oscuro cálido. Manda [docs/design.md](docs/design.md); los tokens viven en `frontend/src/styles/tokens.css` y `themes.css`.

- Antes de tocar la UI, carga la skill `frontend-design` (`.claude/skills/`). La dirección ya está elegida: la skill sirve para ejecutarla bien, no para cambiarla.
- Colores, tamaños, radios, sombras y fuentes salen de los tokens. Nada de hex ni `px` sueltos en componentes.
- Toda pantalla nueva o retocada se revisa con capturas en el navegador: escritorio y móvil (390 px), tema claro y oscuro.

**Prohibido**
- Inter, Roboto y Arial.
- Degradados morados o azul-violeta, y degradados como adorno.
- Tarjetas idénticas con la misma sombra para todo: la jerarquía se marca con tamaño, peso y superficie.
- Layouts de plantilla (héroe con cifra grande y degradado, tres tarjetas iguales en fila).
- Texto de lectura por debajo de 14 px, o gris que no llegue a 4.5:1 sobre su fondo.
- Entradas con fundido y desplazamiento en cada sección. El movimiento responde a una acción de la persona o es un único momento orquestado, y respeta `prefers-reduced-motion`.
- Estados solo con color: llevan siempre icono y texto.
