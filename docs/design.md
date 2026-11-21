# Resolvo — design.md (Cálido editorial, claro por defecto)

> Leyenda de valores: **[m]** medido en el CSS o los tokens publicados por la referencia · **[aprox.]** estimado a ojo sobre capturas · **[propuesta]** decisión propia para completar el sistema.
> Front: Angular. Los tokens son CSS custom properties con tema por `[data-theme]` (`frontend/src/styles/tokens.css` y `themes.css`); los componentes solo consumen estos tokens.
> Cambio de dirección del 2026-11-21: la primera versión («neutro denso, modo oscuro primero», al estilo Linear) se veía fría y lejana, con letra de 13px sobre casi negro. Esta dirección conserva la estructura (sidebar, bandeja agrupada por SLA, detalle con panel de IA) y cambia la piel.

## 1. Visión
Resolvo es una herramienta de trabajo diaria para gente que pasa horas en la bandeja, y al otro lado hay un cliente con un problema. El diseño busca **cercanía y calma**: papel cálido en vez de gris, titulares con serifa como en una revista, letra que se lee sin esfuerzo y color reservado para **estados, SLA y la acción principal**. Debe transmitir fiabilidad: la IA propone y siempre enseña de dónde sale lo que dice.

**Principios**
1. **Cercano antes que denso.** Texto base de 15px, filas con aire y asuntos en serifa a 17px. La jerarquía va por familia y peso, no por letra diminuta.
2. **El color significa algo, y solo cuando algo va mal o pide acción.** Terracota para la acción y la IA; rojo, ámbar y verde solo para SLA y estado. Lo normal va en neutro: un ticket en plazo, de prioridad baja o media, no pinta nada de color. Cada dato se dice una sola vez por pantalla (el estado no sale como icono y como chip, ni en el titular y en Propiedades) y cada vista tiene un único botón primario.
3. **La IA es verificable.** Toda afirmación lleva su cita a la fuente, y toda propuesta es editable antes de enviarla.
4. **Teclado primero.** Cada acción frecuente tiene atajo, y la paleta de comandos (⌘K) llega a todo.

### Referencias
- [Plain](https://www.plain.com): la bandeja agrupada por urgencia ("Breaching" primero), el chip de tiempo de SLA, los IDs en mono y las filas de dos líneas.
- [Intercom Helpdesk](https://www.intercom.com/helpdesk): el panel Copilot, con un chip de fuente dentro de la frase y "2 sources" en el pie del borrador.
- [Linear](https://linear.app): la estructura de sidebar y vista de detalle, y la escala de motion.
- Tono editorial: revistas y webs con serifa de display sobre papel cálido (Fraunces + sans humanista).

Las referencias sirven de inspiración: no uses sus logos, iconos propios ni fuentes de pago.

## 2. Tokens

### 2.1 Color — tema claro (por defecto si el sistema no pide oscuro) [propuesta]
Contraste medido el 2026-11-21: todo texto y todo chip (color al 12 % sobre cualquier superficie) llega a 4.5:1 como mínimo; `--text-4` queda solo para lo decorativo.
```css
:root, [data-theme="light"] {
  --bg-0: #fbf8f3;   /* fondo de la app y del detalle */
  --bg-1: #fffdf9;   /* paneles y tarjetas */
  --bg-2: #f6f0e6;   /* hover */
  --bg-3: #ffffff;   /* inputs y fila seleccionada */
  --bg-4: #f1eadf;   /* chips y pestaña activa */
  --bg-5: #ece3d5;   /* pulsado */
  --bg-6: #2b2219;   /* tooltips y avisos */
  --bg-sidebar: #f3eee5;
  --border-1: #ebe3d6; --border-2: #dfd3c1; --border-3: #cdbea8;
  --text-1: #231c15; --text-2: #4a4037; --text-3: #6a5e50; --text-4: #a89b8b; /* solo decorativo */
  --accent: #a93f22;       /* texto y enlaces (6.0:1 sobre --bg-1, 5.0:1 sobre --accent-tint) */
  --accent-solid: #c2552d; /* botón primario con texto #fff (4.9:1) */
  --accent-tint: #f8e4d6;
  --red: #ad301e; --orange: #904d05; --yellow: #755a00; --green: #376a34; --blue: #2d628a; --teal: #1f7a72;
  /* Sombras cálidas: tiñen de marrón, no de negro */
  --shadow-low: 0 1px 0 #3b2a140d, 0 1px 3px #3b2a1414;
  --shadow-medium: 0 1px 0 #3b2a140a, 0 8px 22px -10px #6b4a1f40;
  --shadow-high: 0 18px 44px -16px #6b4a1f52;
}
```

### 2.2 Color — tema oscuro cálido [propuesta]
La misma dirección de noche: carbón tostado en vez de negro azulado, y terracota clara como acento.
```css
[data-theme="dark"] {
  --bg-0: #17140f; --bg-1: #1e1a14; --bg-2: #241f18; --bg-3: #26211a; --bg-4: #2b251d; --bg-5: #342c22; --bg-6: #3d3428;
  --bg-sidebar: #120f0b;
  --border-1: #2e2820; --border-2: #40372b; --border-3: #52473a;
  --text-1: #f6efe4; --text-2: #d6cbbb; --text-3: #a39684; --text-4: #75695b;
  --accent: #f0916a; --accent-solid: #e8805a; --on-accent: #1c0f08; --accent-tint: #3a2419;
  --red: #ff8a73; --orange: #f0a45b; --yellow: #f2c35b; --green: #8fd18a; --blue: #84b6e8; --teal: #6cc8bd;
}
```

### 2.3 Tokens semánticos de estado (los únicos que usan los componentes)
```css
:root {
  --status-open:      var(--blue);    /* Abierto */
  --status-pending:   var(--yellow);  /* Pendiente (esperando al cliente) */
  --status-resolved:  var(--green);   /* Resuelto */
  --status-closed:    var(--text-3);  /* Cerrado */
  --sla-ok:           var(--text-3);  /* SLA en plazo: neutro, sin alarma */
  --sla-risk:         var(--orange);  /* SLA en riesgo (por debajo del 25% del tiempo restante) [propuesta] */
  --sla-breached:     var(--red);     /* SLA incumplido */
  --priority-urgent:  var(--red);
  --priority-high:    var(--orange);
  --ai:               var(--accent);  /* todo lo que propone la IA */
  /* Fondos de chip: color al 12% sobre la superficie */
  --tint: 12%;
}
/* Ejemplo de uso: background: color-mix(in srgb, var(--sla-risk) var(--tint), transparent); */
```
Un estado nunca va solo con color: lleva siempre icono y texto (§4.2).

### 2.4 Tipografía
- **Display:** `Fraunces` (variable, con eje óptico) en titulares de página, asunto del ticket, asuntos de la bandeja y la marca. Peso 500–600, tracking −0.01em.
- **UI:** `Instrument Sans`, sans humanista que acompaña a la serifa sin competir. Texto por defecto a 15px.
- **Mono** (IDs de ticket, tiempos de SLA, citas, contadores): `Geist Mono`.
- Evitar Inter, Roboto y Arial: son lo que hacía la UI genérica y lejana.

Pesos: 400 normal, 500 medium y 600 semibold.

| Token | Tamaño | Peso | Uso |
|---|---|---|---|
| `--fs-micro` | 12px | 500–600 | contadores, etiquetas de sección en versalitas, SLA en mono |
| `--fs-mini` | 13px | 400–500 | chips y metadatos |
| `--fs-small` | 14px | 400–500 | botones, etiquetas de formulario, cabeceras de mensaje |
| `--fs-regular` | 15px | 400 | **texto por defecto**, conversación y artículos |
| `--fs-large` | 17px | 500 (serifa) | asunto en la fila de la bandeja |
| `--fs-title3` | 22px | 600 (serifa) | cabeceras de panel |
| `--fs-title2` | 30px | 500–600 (serifa) | título de página y asunto del ticket |
| `--fs-title1` | 40px | 500 (serifa) | titulares de inicio y acceso |

### 2.5 Spacing, radius, shadow, layout
```css
:root {
  --space-0_5: 2px; --space-1: 4px; --space-1_5: 6px; --space-2: 8px; --space-2_5: 10px; --space-3: 12px;
  --space-3_5: 14px; --space-4: 16px; --space-5: 20px; --space-6: 24px; --space-8: 32px; --space-10: 40px; --space-12: 48px;

  --radius-xs: 5px;   /* citas */
  --radius-sm: 8px;   /* logo y controles pequeños */
  --radius-md: 10px;  /* botones, inputs e ítems de navegación */
  --radius-lg: 14px;  /* filas de la bandeja */
  --radius-xl: 18px;  /* tarjetas, burbujas, panel de IA y compositor */
  --radius-pill: 9999px; /* chips y botones segmentados */

  --h-control-sm: 28px; --h-control: 36px; --h-control-lg: 42px;
  --h-row: 44px; --h-row-2: 68px; --h-header: 60px;

  --sidebar-w: 248px; --list-w: 384px; --aside-w: 304px; --measure: 736px;
}
```

### 2.6 Motion [m] Linear (sin cambios)
```css
:root {
  --dur-quick: 100ms;    /* hover y press */
  --dur-regular: 160ms;  /* fondos y colores (Linear usa .16s) */
  --dur-slow: 250ms;     /* paneles y menús */
  --ease-out-quad: cubic-bezier(.25,.46,.45,.94);
  --ease-out-quint: cubic-bezier(.23,1,.32,1); /* curva estándar [propuesta] */
}
@media (prefers-reduced-motion: reduce) { :root { --dur-regular: 0ms; --dur-slow: 0ms; } }
```

## 3. Layout y grid
```
┌──────────┬───────────────────────┬──────────────────────────────┬──────────────┐
│ Sidebar  │ Lista de tickets      │ Conversación                 │ Panel IA     │
│ 232px    │ 360–420px, ajustable  │ flexible (mín. 480px)        │ 360px, plegable│
└──────────┴───────────────────────┴──────────────────────────────┴──────────────┘
```
- La sidebar y los paneles van sobre `--bg-1`, separados por bordes de 1px `--border-1`; no hay huecos entre paneles (estilo "split", no tarjetas flotando).
- La cabecera de cada panel mide `--h-header` (48px) y tiene borde inferior.
- Contenido de lectura (artículos de la KB y la conversación): ancho máximo de 720px.
- **En la app construida** el layout es de tres zonas: sidebar (232px), lista (`--list-w`, 380px) y ticket, que ocupa todo el ancho restante. El panel de IA no es una cuarta columna: vive en la columna lateral del ticket (`--aside-w`, 320px) junto a las propiedades, bajo el título «Asistente ✦». Esa columna lateral se pone al lado de la conversación solo si el panel del ticket mide ≥52rem (container query); si no, baja bajo la conversación.
- **Navegación:**
  - Un ticket abierto siempre tiene **«← Bandeja»** (vuelve a la lista con la misma vista), la posición «3 / 12» y los botones anterior/siguiente, que siguen el orden de la bandeja. Atajos: `J` siguiente, `K` anterior y `Esc` volver (no actúan mientras se escribe en un campo).
  - La vista elegida vive en la URL (`?vista=`): se conserva al abrir un ticket, al volver y al recargar.
  - Al recargar, la sesión se recupera con la cookie de refresco; solo si no hay sesión se vuelve a `/login?volver=<ruta>`.
- **Responsive** (los puntos de corte van en `em`, para seguir la escala de texto):
  - Con ≥1200px se ven la lista y el ticket a la vez.
  - Entre 960 y 1199px, un solo panel: la lista, o el ticket con su «← Bandeja».
  - Con <960px la sidebar pasa a barra superior con los mismos accesos.
  - (Queda pendiente el panel IA como drawer y el rail de 48px de la sidebar: no se han construido.)
- Densidad conmutable entre **compacta** (fila de 36px) y **cómoda** (56px, dos líneas), con `[data-density]`.

## 4. Componentes clave

### 4.0 Inicio y acceso
Es lo primero que ve quien llega, así que cuenta qué es Resolvo antes de pedir nada.
- **Inicio (`/`)**, pública. Cabecera con logo, cambio de tema y «Iniciar sesión» (o «Ir a la bandeja» si ya hay sesión). Héroe centrado con una etiqueta, el titular (`clamp(2rem, 1rem + 4vw, 3.25rem)`, 590) y un subtítulo de dos líneas. Debajo, los **accesos de demostración** (el punto siguiente) y, a continuación, una maqueta de la bandeja y del panel de IA hecha en HTML y CSS, sin imágenes y marcada `aria-hidden`. Luego tres tarjetas de valor (respuestas con citas, SLA a la vista, la IA propone y tú decides), «Cómo funciona» en tres pasos y un pie con el enlace al código.
- **Accesos de demostración:** «Entrar como Cliente / Agente / Admin», cada uno con una línea de lo que puede hacer ese rol. Entran con un clic con la cuenta demo, se deshabilitan mientras entra (un doble clic no entra dos veces) y enseñan el mismo error que el formulario si falla.
- **Login (`/login`)** a dos columnas: a la izquierda `--bg-1` con el logo, una frase y tres puntos del producto; a la derecha «← Volver al inicio», el formulario (email y contraseña, 24rem de ancho), un separador «o usa la demostración» y los accesos demo. Con <960px queda una sola columna y la columna izquierda se reduce al logo. Tras entrar se vuelve a la ruta que se pedía (`?volver=`), solo si es interna.
- **Lista de tickets vacía del panel derecho:** icono de mensaje a 48px, «Elige un ticket de la lista…» y la pista de atajos con `kbd`.
- **El SLA solo se muestra mientras corre** (ticket abierto o pendiente): un ticket resuelto o cerrado no lleva chip de SLA, ni rojo por un plazo que ya no importa.

### 4.1 Sidebar (patrón Linear [aprox.])
- Arriba, el selector de organización con chevron, y a la derecha los botones de búsqueda y de "nuevo ticket".
- Ítems de 28px con icono de 16px, texto `--fs-small` en `--text-2` y contador alineado a la derecha en `--fs-micro` `--text-3`. Las secciones son colapsables ("Vistas", "Equipos", "Favoritos") y su etiqueta va en `--fs-micro`, 510, en `--text-3`.
- Estados:
  - hover: `--bg-2`
  - activo: `--bg-5` con texto `--text-1` y radio `--radius-sm`
  - foco: anillo de 2px en `--accent`
- Las vistas por defecto son Mis tickets, Sin asignar, Todos, SLA en riesgo (con el contador en naranja si es mayor que 0), Pendientes y Resueltos.

### 4.2 Indicador de estado (StatusIcon + StatusLabel)
Icono circular de 14px, al estilo de Linear [aprox.], siempre con tooltip o texto al lado:

| Estado | Icono | Color |
|---|---|---|
| Abierto | círculo con borde | `--status-open` |
| Pendiente | círculo medio relleno | `--status-pending` |
| Resuelto | círculo relleno con ✓ | `--status-resolved` |
| Cerrado | círculo relleno gris | `--status-closed` |

**Prioridad:** tres barras crecientes (patrón Linear); la urgente es un cuadrado con "!" en `--priority-urgent`.

### 4.3 Chip de SLA (patrón Plain)
- Altura de 20px, `--radius-xs`, padding de 0 6px, `--fs-mini` con tnum e icono de reloj de 12px.
- **En plazo:** texto `--text-3` sobre `--bg-4` ("3h 20m").
- **En riesgo:** texto `--sla-risk` sobre su tinte al 12%, con el icono de reloj.
- **Incumplido:** texto `--sla-breached` sobre su tinte, icono de alerta y tiempo en negativo ("−1h 45m").
- Tooltip: "Primera respuesta: vence 15:40 (CET)". Se actualiza cada minuto sin animar.

### 4.4 Bandeja de tickets
- **Barra superior:** título de la vista, tabs con contador (Triage 12 · Pendiente 30 · Todos), un botón "Filtros" que abre un menú de campos (Asignado, Estado, Prioridad, Etiqueta, Cliente, Canal, SLA) y un botón "Vista" con opciones de densidad, agrupación y orden.
- **Filtros aplicados:** chips eliminables bajo la barra ("Estado: Abierto ×").
- **Agrupación por defecto por urgencia** (patrón Plain): primero "SLA incumplido", luego "En riesgo", "Urgente" y "Resto". La cabecera de cada grupo lleva icono del color del grupo, nombre en 590 y contador en `--text-3`, y es colapsable.
- **Fila calmada (construida el 2026-11-21, sustituye a la de chips):** tres líneas, sin píldoras.
  - Línea 1: ID en mono `--text-3` y, a la derecha, el SLA como **texto** en mono con icono (reloj o alerta). En plazo va en `--text-3`; en riesgo en `--sla-risk`; incumplido en `--sla-breached` y peso 600. Solo se pinta mientras el ticket está abierto o pendiente, y lleva texto accesible («SLA incumplido: −5d 17h»).
  - Línea 2: el asunto en serifa (`--fs-large`), hasta dos líneas.
  - Línea 3: el icono de estado con su nombre en `--text-3` y, solo si es Alta o Urgente, la prioridad con icono y su color. Baja y Media no se dicen en la fila.
  - Sin avatar, vista previa ni hora en la fila: son candidatos a la densidad «cómoda» (pendiente).
- **Estados de fila:**
  - hover: `--bg-2`
  - seleccionada: `--accent-tint` con una barra izquierda de 2px en `--accent`
  - activa (abierta en el detalle): `--bg-5`
  - no leída: punto de 6px en `--accent` y asunto en 590
- **Selección múltiple:** al haber selección aparece una barra flotante abajo, con `--shadow-high` y `--radius-lg`, con las acciones Asignar, Estado, Etiquetar y Cerrar y sus atajos.
- **Vacío:** ilustración mínima en línea y un texto del tipo "No hay tickets que coincidan con estos filtros", con un botón "Limpiar filtros".
- **Carga:** skeleton de filas en `--bg-2`, sin spinner.

### 4.5 Vista de ticket (conversación)
- **Cabecera:** ID en mono, asunto en `--fs-large`, selector de estado, asignado, prioridad, navegación "3 / 84" con ↑↓ y menú "⋯".
- **Mensajes:** una columna de tarjetas blancas con la esquina superior izquierda casi recta (apunta al avatar), sombra baja y avatar de 32px con iniciales en `--accent-tint`. Las notas internas van en ámbar con borde discontinuo.
  - Cada mensaje lleva avatar, nombre en 510, hora y cuerpo en `--fs-regular` `--text-1`.
  - Mensaje del cliente: fondo `--bg-1`.
  - Respuesta del agente: tarjeta `--bg-2` con borde `--border-1`.
  - **Nota interna:** fondo con el amarillo al 8% y una etiqueta "Nota interna" en amarillo, para que no se confunda con lo que lee el cliente.
- **Eventos del sistema:** una línea en `--fs-mini` `--text-3`, por ejemplo "Ana asignó a Luis · hace 2 min" (patrón Linear).
- **Compositor:** fijo abajo, con tabs Responder y Nota, editor de texto enriquecido, adjuntos, plantillas (con atajo "/") y el botón primario "Enviar" (⌘↵) con un desplegable para "Enviar y resolver".

### 4.6 Panel de IA con citas (Intercom más el panel de agente de Linear)
- **Cabecera:** "Asistente", un chip con la versión del modelo o de la KB y los botones de plegar y expandir.
- **Traza colapsable:** "Leyó la conversación y consultó 3 artículos ▸" en `--fs-mini` `--text-3`. Al desplegarla se ve la lista de artículos consultados.
- **Bloque "Resumen":** 2 o 3 líneas.
- **Bloque "Respuesta propuesta":** tarjeta `--bg-2`, borde `--border-2`, `--radius-lg`.
  - **Cita en línea:** chip `[1]` o con icono y título corto, por ejemplo "📄 Política de reembolsos". Altura de 18px, `--radius-xs`, fondo `--accent-tint`, texto `--accent` y `--fs-mini`. Va justo después de la frase que respalda.
  - En hover sobre la cita aparece un popover con el título del artículo, el fragmento citado (la frase resaltada con subrayado en `--accent`), la fecha de última actualización y "Abrir en la base de conocimiento ↗".
  - Al pulsar la cita, la frase respaldada del borrador se resalta para que el agente la verifique.
  - **Pie:** avatares de las fuentes solapados con "3 fuentes", y las acciones "Insertar en respuesta" (primario), "Editar" y "Regenerar", más 👍 y 👎.
- **Sin fuentes suficientes:** un aviso en `--orange` del tipo "No encontré artículos que respalden una respuesta. Revisa antes de enviar". En ese caso no se propone texto sin citas.
- **Input:** "Pregunta sobre este ticket…" con envío por ↵.
- **Estados:** pensando (tres puntos o shimmer en el texto "Analizando…"), streaming (el texto aparece por bloques, sin cursor parpadeante), error con "Reintentar" y deshabilitado si la KB no está configurada.
- Todo lo que viene de la IA lleva su marca: un icono ✦ de 12px en `--ai`. No hay degradados "mágicos".
- El panel va en `--bg-1` con borde `--border-1` y sin sombra ni degradado. «Aplicar» y «Aprobar y publicar» son botones secundarios: el único primario del ticket es «Enviar».

### 4.7 Botones
| Variante | Fondo | Texto | Borde | Hover |
|---|---|---|---|---|
| Primario | `--accent-solid` | `--on-accent` | — | `--accent` |
| Secundario | `--bg-4` | `--text-1` | `--border-2` | `--bg-5` |
| Fantasma | transparente | `--text-2` | — | `--bg-2` |
| Peligro | `--red` al 12% | `--red` | — | al 20% |

- Altura `--h-control` (28px), padding de 0 10px, `--radius-sm`, `--fs-small` y peso 510. Con atajo, añaden un `kbd` en mono a la derecha.
- Estados:
  - press: oscurece el fondo y no escala
  - foco: `outline: 2px solid var(--accent); outline-offset: 2px`
  - deshabilitado: opacidad .5
  - cargando: spinner de 12px que sustituye al icono, con el ancho fijo

### 4.8 Base de conocimiento [propuesta]
- Columna izquierda con el árbol de colecciones (como la sidebar).
- Centro con el artículo en `--fs-regular`, ancho máximo de 720px y títulos en `--fs-large` o `--fs-title3`.
- Metadatos: autor, actualización, estado (Borrador, Publicado, Revisar) y "Citado en 42 respuestas este mes", que conecta la KB con la IA.
- Un aviso "Desactualizado" (más de 90 días) en amarillo, porque la IA lo cita.
- Editor en bloques, con barra flotante al seleccionar texto.

### 4.9 Métricas y SLA [propuesta]
- Fila de KPI en tarjetas `--bg-1` con borde, `--radius-md` y padding de 16px:
  - Tickets abiertos
  - Primera respuesta (mediana)
  - Cumplimiento de SLA (%)
  - Tasa de resolución
  - Uso de la IA (% de respuestas con propuesta aceptada)
- Cada KPI lleva la etiqueta en `--fs-mini` `--text-3`, la cifra en `--fs-title2` con tnum y la variación frente al periodo anterior, en verde o rojo según signifique mejora o empeora (no según suba o baje).
- Gráficos: líneas de 1.5px, rejilla `--border-1` solo horizontal, ejes en `--fs-micro` `--text-3`, como mucho 3 series de la paleta de estado y tooltip en `--bg-6` con `--shadow-medium`.
- **Carga del equipo:** tabla con agente, tickets abiertos, en riesgo e incumplidos (chips de SLA) y una barra horizontal de carga.
- **Tabla de "en riesgo ahora"** con enlace a cada ticket.

### 4.10 Ajustes de la organización [propuesta]
Navegación secundaria a la izquierda (General, Miembros, Equipos, SLA, Canales, Base de conocimiento e IA, Facturación) y un formulario a la derecha con secciones separadas por bordes, cada una con su título y descripción en `--text-3`.
- Los inputs miden 28px y van sobre `--bg-3`, con borde `--border-2` y `--radius-sm`. Con foco, el borde pasa a `--accent` y se añade un anillo del 25%; el error usa el borde `--red` con un mensaje en `--fs-mini`.
- Las acciones destructivas van en una sección "Zona de peligro" al final.

### 4.11 Paleta de comandos y atajos
- ⌘K abre un modal centrado de 560px con `--radius-lg`, `--shadow-high`, una búsqueda y resultados agrupados (Acciones, Tickets, Artículos).
- Atajos [propuesta]:
  - `J`/`K`: moverse por la bandeja
  - `↵`: abrir el ticket
  - `R`: responder
  - `N`: nota interna
  - `A`: asignar
  - `S`: cambiar el estado
  - `E`: resolver
  - `⌘↵`: enviar
  - `⌘I`: abrir o cerrar el panel de IA
  - `I`: insertar la propuesta de la IA
  - `X`: seleccionar
  - `?`: ver la lista de atajos
- `kbd`: mono en `--fs-micro`, `--bg-4`, borde `--border-2` y `--radius-xs`.

### 4.12b Moderación de comentarios
Cualquier persona con acceso puede escribir, así que hay que poder retirar lo que sobre sin perder el rastro.
- **Quién:** un agente o un admin **oculta** con un motivo (Spam, Ofensivo u Otro); solo un admin **restaura**. Un cliente no puede hacer ninguna de las dos cosas. Nunca se borra: queda quién, cuándo y por qué.
- **Ocultar:** botón fantasma «Ocultar» (icono de ojo tachado) en la cabecera de cada comentario. Abre en línea un selector de motivo, «Ocultar comentario» (secundario) y «Cancelar». No hay modal.
- **Lo oculto, para el equipo:** una sola línea en `--text-3` con icono: «Oculto por Ana · Spam», con «Ver texto» y, para el admin, «Restaurar».
- **Lo oculto, para el cliente:** «Este comentario se ha retirado.», sin el texto y sin saber quién ni por qué (lo decide el dominio, no la interfaz).
- **Frenos automáticos:** 10 comentarios por usuario y minuto (429) y, para un cliente, como mucho 3 enlaces por comentario (422 `demasiados-enlaces`). Un agente o un admin pueden pegar los que quieran.
- **Demo pública:** con `RESOLVO_DEMO_PURGAR_COMENTARIOS=true`, al arrancar y cada 24 h se borran los comentarios de visitantes de más de 24 h; los de la semilla se conservan.

### 4.12 Otros
- **Avatares:** 16, 20, 24 o 32px, circulares, con inicial sobre `--bg-4` y texto `--text-2` (construido así el 2026-11-21: ya no son terracota). El color derivado del nombre sigue pendiente.
- **Nota interna:** el aviso es icono (candado) y texto en ámbar, sin píldora.
- **Etiquetas:** punto de color de 8px y texto `--text-2` sobre `--bg-4` con `--radius-xs`, al estilo Linear (sin fondo de color pleno).
- **Toasts:** abajo a la izquierda, sobre `--bg-6`, con "Deshacer" en las acciones destructivas (5s).
- **Tooltips:** `--bg-6`, `--fs-mini`, retardo de 400ms y atajo incluido.

## 5. Motion
- Hover y color: `var(--dur-regular) var(--ease-out-quad)`.
- Menús y popovers: opacidad 0→1 y `translateY(-4px→0)` en 160ms.
- Panel de IA y drawers: entran deslizando 250ms con `--ease-out-quint`.
- Filas: no se animan al reordenar si hay más de 50; un ticket nuevo entra con un fundido de 160ms y el fondo `--accent-tint` se desvanece en 1.5s.
- El destacado de una cita se desvanece en 150ms **[m]** (el "highlight fade-out" de Linear).
- Implementado (2026-11-21): una sola animación, `aparecer` (opacidad 0→1 y subida de 6px, `--dur-slow`), en la conversación al abrir un ticket y en los paneles de IA cuando llegan. La llegada al login es el único momento orquestado: titular y tres puntos en cascada de 80ms. Todo bajo `prefers-reduced-motion: no-preference`.
- Los botones se hunden 1px al pulsar. No hay entradas con desplazamiento en cada sección ni hover animado en cada tarjeta.
- Nada rebota, y no hay parallax ni animaciones decorativas.

## 6. Imágenes e iconos
- **Iconos:** [Lucide](https://lucide.dev) en línea de 1.5px, a 16px en la interfaz y 14px en chips y estados, en `currentColor`. Los iconos de estado y prioridad son SVG propios (§4.2).
- No hay fotografía ni ilustración 3D en el producto. Los estados vacíos llevan iconos en línea a 48px en `--text-4`.
- Logos de cliente o empresa: 16px y redondos, con la inicial como alternativa.

## 7. Do / Don't
**Do**
- Neutros para casi todo, con el color reservado al estado, el SLA, la prioridad y la acción principal.
- IDs, tiempos y cifras en mono o tnum.
- Toda propuesta de la IA con citas, y toda cita abre su fuente.
- Atajo visible en tooltips y menús.
- Probar cada pantalla en oscuro **y** en claro con los mismos componentes.

**Don't**
- Usar el acento índigo para avisos, porque el SLA tiene sus propios colores.
- Indicar un estado solo con color.
- Usar `--text-4` en texto que el usuario tenga que leer.
- Sombras en paneles fijos (solo en lo que flota).
- Usar degradados, glow o "brillos de IA".
- Texto de la IA sin marca ✦, o una respuesta sin fuentes presentada como segura.

## 8. Accesibilidad (WCAG AA)
Contrastes calculados en oscuro sobre `--bg-1` #0f1011:
- `--text-1` 17.9:1 ✅
- `--text-2` 13.0:1 ✅
- `--text-3` 5.9:1 ✅
- `--text-4` 3.3:1 ⚠️ (solo decorativo o deshabilitado)
- `--accent` 5.0:1 ✅
- `--red` 5.5 ✅
- `--orange` 7.2 ✅
- `--yellow` 11.0 ✅
- `--green` 6.0 ✅
- `--blue` 7.5 ✅
- `--teal` 7.9 ✅
- El texto blanco del botón primario sobre `--accent-solid` da 4.7:1 ✅.

En claro sobre #fff:
- `--text-1` 16.3 ✅
- `--text-3` 5.9 ✅
- `--text-4` 2.8 ⚠️ (solo decorativo)
- `--accent` 4.7 ✅
- `--red` 5.5 ✅
- `--orange` 4.7 ✅
- `--yellow` 5.1 ✅
- `--green` 5.1 ✅
- `--blue` 5.7 ✅

Revisa además estos puntos:
- **Texto de chips:** cuando el chip tenga el tinte al 12% (es un poco menos que sobre el fondo puro).
- **Foco:** siempre visible y con el mismo anillo en toda la app.
- **Tamaño de controles:** mínimo de 24×24px (WCAG 2.2, criterio 2.5.8).
- **Bandeja:** se navega con teclado como `role="grid"` o lista con `aria-activedescendant`.
- **SLA:** el chip anuncia el texto completo ("SLA en riesgo, vence en 20 minutos").
- **Respuesta de la IA:** una región `aria-live="polite"`; cada cita es un enlace con nombre accesible ("Fuente 1: Política de reembolsos").
- **Motion:** respeta `prefers-reduced-motion`.

## 9. Notas para Angular
- Los tokens van en `styles/tokens.css` (primitivos), `styles/semantic.css` (los de §2.3) y `styles/themes.css` (`[data-theme]`). Los componentes usan **solo los semánticos**.
- Tema: un servicio `ThemeService` que pone `data-theme` en `<html>` y respeta `prefers-color-scheme`. La densidad va en `data-density`.
- Componentes propuestos:
  - `rs-button`
  - `rs-icon-button`
  - `rs-status`
  - `rs-priority`
  - `rs-sla-chip`
  - `rs-chip`
  - `rs-avatar`
  - `rs-kbd`
  - `rs-ticket-row`
  - `rs-ticket-group`
  - `rs-ai-panel`
  - `rs-citation` (con popover del CDK Overlay)
  - `rs-command-palette`
  - `rs-kpi-card`
  - `rs-empty-state`
  - `rs-toast`
- Menús, popovers y tooltips con `@angular/cdk/overlay` y `a11y` (FocusTrap, ListKeyManager para la bandeja y la paleta de comandos).
- Fuentes autoalojadas en `frontend/public/fonts/` (woff2 variables, solo subconjunto latino, licencia SIL OFL con su texto en la misma carpeta) y declaradas en `src/styles/fuentes.css` con `font-display: swap`. Fraunces e Instrument Sans se precargan en `index.html`; no hay peticiones a Google.

## 10. Valores pendientes de validar
- Las alturas reales de filas, botones e inputs y los paddings no se pudieron medir (las webs muestran maquetas de marketing). Los valores de §2.5 son una propuesta; valídalos con un prototipo.
- Las pantallas de base de conocimiento, métricas y ajustes son una propuesta sin referencia directa en oscuro.
