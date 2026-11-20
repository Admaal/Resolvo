# Resolvo — design.md (Neutro denso, modo oscuro primero)

> Leyenda de valores: **[m]** medido en el CSS o los tokens publicados por la referencia · **[aprox.]** estimado a ojo sobre capturas · **[propuesta]** decisión propia para completar el sistema.
> Front: Angular. Los tokens son CSS custom properties con tema por `[data-theme]`; los componentes van en una librería propia (o sobre Angular CDK) que solo consume estos tokens.

## 1. Visión
Resolvo es una herramienta de trabajo diaria para gente que pasa horas en la bandeja. El diseño busca lo mismo que Linear: neutros casi negros, bordes de 1px, tipografía pequeña y nítida, y color reservado solo para **estados, SLA y la acción principal**. Debe transmitir fiabilidad: la IA propone y siempre enseña de dónde sale lo que dice.

**Principios**
1. **Densidad legible.** Mucha información por pantalla, con jerarquía clara por peso y gris, no por tamaño.
2. **El color significa algo.** Si algo tiene color, es un estado, una prioridad o un aviso de SLA.
3. **La IA es verificable.** Toda afirmación lleva su cita a la fuente, y toda propuesta es editable antes de enviarla.
4. **Teclado primero.** Cada acción frecuente tiene atajo, y la paleta de comandos (⌘K) llega a todo.

### Referencias
- [Linear](https://linear.app): la base visual (tokens de color, tipografía, radios, sombras y motion medidos en su CSS), la sidebar, la vista de detalle y el panel del agente.
- [Plain](https://www.plain.com): la bandeja agrupada por urgencia ("Breaching" primero), el chip de tiempo de SLA, los IDs en mono y las filas de dos líneas.
- [Intercom Helpdesk](https://www.intercom.com/helpdesk): el panel Copilot, con un chip de fuente dentro de la frase, "2 sources" en el pie del borrador y una traza colapsable.
- Secundarias: [Help Scout Inbox](https://www.helpscout.com/inbox/) (columna de tiempo esperando y contadores por vista) y [Missive](https://missiveapp.com) (barra roja lateral en las urgentes).
- Capturas en `/workspace/design/resolvo/refs/` (las de detalle llevan el prefijo `study-`).

Las referencias sirven de inspiración: no uses sus logos, iconos propios ni fuentes de pago.

> Ninguna web pública mostraba una base de conocimiento ni un panel de métricas en oscuro, así que esas pantallas (§4.8 y §4.9) son **[propuesta]** coherente con el sistema.

## 2. Tokens

### 2.1 Color — tema oscuro (por defecto)
```css
:root, [data-theme="dark"] {
  /* Superficies [m] Linear */
  --bg-0: #08090a;         /* fondo de la app */
  --bg-1: #0f1011;         /* paneles: sidebar, lista, detalle */
  --bg-2: #141516;         /* tarjetas y filas en hover */
  --bg-3: #191a1b;         /* inputs y menús */
  --bg-4: #1c1c1f;         /* chips y botón secundario */
  --bg-5: #232326;         /* seleccionado y pulsado */
  --bg-6: #28282c;         /* tooltips */

  /* Bordes [m] */
  --border-1: #23252a;     /* separadores por defecto */
  --border-2: #34343a;     /* inputs y hover */
  --border-3: #3e3e44;     /* fuerte */
  --border-alpha: #ffffff14;

  /* Texto [m] */
  --text-1: #f7f8f8;       /* principal */
  --text-2: #d0d6e0;       /* secundario */
  --text-3: #8a8f98;       /* terciario: metadatos y placeholders */
  --text-4: #62666d;       /* solo decorativo o deshabilitado (3.3:1, no para texto que haya que leer) */

  /* Acento [m] indigo de Linear */
  --accent: #7170ff;
  --accent-hover: #828fff;
  --accent-tint: #18182f;
  --accent-solid: #5e6ad2; /* fondo del botón primario, texto #fff (4.7:1) */
  --on-accent: #ffffff;

  /* Paleta de estado base [m] */
  --red: #eb5757; --orange: #fc7840; --yellow: #f0bf00;
  --green: #27a644; --blue: #4ea7fc; --teal: #00b8cc;
}
```

### 2.2 Color — tema claro [propuesta]
Plain e Intercom solo enseñaban su versión clara de marketing, así que estos valores los he ajustado yo para que cumplan AA sobre blanco. El gris `#636370` y la tinta `#1f1f28` sí están medidos en Plain.
```css
[data-theme="light"] {
  --bg-0: #f7f7f8; --bg-1: #ffffff; --bg-2: #f9f9fc; --bg-3: #f3f3f6;
  --bg-4: #ececf1; --bg-5: #e4e4ea; --bg-6: #1f1f28;
  --border-1: #e7e7ef; --border-2: #d6d6df; --border-3: #c4c4cf; --border-alpha: #0000000f;
  --text-1: #1f1f28; --text-2: #3c3c48; --text-3: #636370; --text-4: #9898a9;
  --accent: #5e6ad2; --accent-hover: #4f5bc4; --accent-tint: #eef0fd; --accent-solid: #5e6ad2;
  --red: #c4302b; --orange: #b25e09; --yellow: #8a6a00; --green: #1e7f36; --blue: #2563c9; --teal: #0e7c8a;
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
- **UI:** `Inter` **[m]**, la misma familia que usa Linear (Inter Variable). Disponible en Google Fonts. Actívale `font-feature-settings: "cv01", "ss03"` **[m]**, y además `"tnum"` en las cifras.
- **Mono** (IDs de ticket, código, atajos): Linear usa Berkeley Mono **[m]**, que es de pago. Equivalente libre: **`JetBrains Mono`** o `Geist Mono` (este último lo usa Plain [m]).
- Sin serifa de display: el producto no es una landing.

Pesos **[m]** de Linear: 400 normal, 510 medium y 590 semibold (con Inter Variable funcionan; si no, usa 400, 500 y 600).

| Token | Tamaño / interlineado | Peso | Uso |
|---|---|---|---|
| `--fs-micro` | 11px / 16px | 510 | contadores, etiquetas de sección, timestamps |
| `--fs-mini` | 12px / 16px | 400–510 | chips, metadatos y columnas de tabla |
| `--fs-small` | 13px / 20px | 400 | **texto por defecto de la UI**, filas de la bandeja |
| `--fs-regular` | 15px / 24px | 400 | cuerpo de la conversación y artículos de la KB |
| `--fs-large` | 18px / 26px | 590 | títulos de sección |
| `--fs-title3` | 20px / 28px | 590 | título de la página |
| `--fs-title2` | 24px / 32px | 590 | cifras de KPI |
| `--fs-title1` | 36px / 40px | 590 | cifra destacada del panel |

La escala está **[m]** (en el CSS de Linear); los interlineados son **[propuesta]**.

### 2.5 Spacing, radius, shadow, layout
```css
:root {
  --space-0_5: 2px; --space-1: 4px; --space-1_5: 6px; --space-2: 8px; --space-3: 12px;
  --space-4: 16px; --space-5: 20px; --space-6: 24px; --space-8: 32px; --space-12: 48px;

  /* Radios [m] Linear */
  --radius-xs: 4px;   /* chips y badges */
  --radius-sm: 6px;   /* botones, inputs e ítems de navegación */
  --radius-md: 8px;   /* menús y tarjetas pequeñas */
  --radius-lg: 12px;  /* paneles flotantes, tarjeta del borrador IA y modales */
  --radius-xl: 16px;
  --radius-pill: 9999px;

  /* Sombras [m] Linear (en oscuro, solo para lo que flota) */
  --shadow-low:    0 2px 4px #0000001a;
  --shadow-medium: 0 4px 24px #00000033;
  --shadow-high:   0 7px 32px #00000059;

  /* Alturas [propuesta] (no se pudieron medir en la app real) */
  --h-control-sm: 24px; --h-control: 28px; --h-control-lg: 32px;
  --h-row: 36px;        /* fila de bandeja compacta (1 línea) */
  --h-row-2: 56px;      /* fila cómoda (2 líneas, patrón Plain) */
  --h-header: 48px;

  --sidebar-w: 232px;   /* [m] Linear */
  --ai-panel-w: 360px;  /* [propuesta] */
}
```

### 2.6 Motion [m] Linear
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
- **Fila cómoda (2 líneas):**
  - Línea 1: checkbox (visible en hover o si hay selección), icono de estado, cliente o empresa en 510, ID en mono `--text-3` ("RS-24629") y asunto en `--text-1`.
  - Línea 2: vista previa en `--text-3`, cortada en una línea, más los chips de etiqueta.
  - A la derecha: chip de SLA, avatar del asignado (20px) y hora relativa en `--fs-micro`.
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
- **Mensajes:** una sola columna sin burbujas de color (más sobrio que Intercom).
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

### 4.12 Otros
- **Avatares:** 16, 20, 24 o 32px, circulares, con inicial sobre un color derivado del nombre (paleta neutra y desaturada).
- **Etiquetas:** punto de color de 8px y texto `--text-2` sobre `--bg-4` con `--radius-xs`, al estilo Linear (sin fondo de color pleno).
- **Toasts:** abajo a la izquierda, sobre `--bg-6`, con "Deshacer" en las acciones destructivas (5s).
- **Tooltips:** `--bg-6`, `--fs-mini`, retardo de 400ms y atajo incluido.

## 5. Motion
- Hover y color: `var(--dur-regular) var(--ease-out-quad)`.
- Menús y popovers: opacidad 0→1 y `translateY(-4px→0)` en 160ms.
- Panel de IA y drawers: entran deslizando 250ms con `--ease-out-quint`.
- Filas: no se animan al reordenar si hay más de 50; un ticket nuevo entra con un fundido de 160ms y el fondo `--accent-tint` se desvanece en 1.5s.
- El destacado de una cita se desvanece en 150ms **[m]** (el "highlight fade-out" de Linear).
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
- Fuentes: Inter y JetBrains Mono autoalojadas (woff2, `font-display: swap`).

## 10. Valores pendientes de validar
- Las alturas reales de filas, botones e inputs y los paddings no se pudieron medir (las webs muestran maquetas de marketing). Los valores de §2.5 son una propuesta; valídalos con un prototipo.
- Las pantallas de base de conocimiento, métricas y ajustes son una propuesta sin referencia directa en oscuro.
