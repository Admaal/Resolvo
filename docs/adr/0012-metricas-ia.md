# ADR-0012: Métricas de la IA en Postgres, con tarifas de lista

- Fecha: 2026-11-19
- Estado: aceptada

## Contexto

La fase 5 pide tokens, coste y latencia de cada llamada a la IA. `LlamadorGemini` ya sabía los tokens y la duración de las llamadas que salían bien (ADR-0011), pero nada se guardaba y las llamadas fallidas, las que más tardan, no se veían.

## Opciones

1. **Una fila por llamada en Postgres y un endpoint de resumen para ADMIN** — a favor: sobrevive a que Render se duerma, se filtra por organización con una columna y el p95 incluye los fallos. En contra: una escritura más por llamada (unas pocas al día en la demo).
2. **Micrometer y `/actuator/prometheus`** — a favor: es lo estándar. En contra: los contadores viven en memoria y se pierden en cada arranque en frío de Render Free, y no hay un Prometheus donde recogerlos.
3. **Solo logs** — a favor: cero código. En contra: no se pueden agregar ni enseñar.

## Decisión

Opción 1. La tabla `llamada_ia` (V7) guarda organización, operación (`sugerir`, `clasificar`), modelo, tokens de entrada y salida, milisegundos y si salió bien. Se escribe desde un observador de `LlamadorGemini.observarLlamadas`, que avisa **una vez por petición**, con el modelo que la sirvió o, si ninguno pudo, el último probado, y con la duración que esperó la persona. El sondeo de arranque no cuenta. `GET /api/metricas/ia?desde=&hasta=` (solo ADMIN, solo su organización, 7 días por defecto) agrega en el dominio (`ResumenLlamadas`): por operación y modelo, llamadas, fallos, tokens, p50 y p95 por rango más cercano y coste.

El coste es **a precio de lista, en USD**, con las tarifas de `resolvo.ia.tarifas` (`application.yml`), cada una con su fuente y la fecha en que se miró (la página oficial de precios de Gemini, 2026-11-19). No hay tipo de cambio a euros que no pudiera citar. Un modelo sin tarifa, como los alias `*-latest` o `gemini-3.5-flash`, deja su coste **desconocido (nulo)**, y el total también: no se inventa un cero. La factura real del nivel gratuito es 0.

## Consecuencias

- El modelo que más se usa en la demo es el alias `gemini-flash-lite-latest`, que no tiene tarifa publicada: su coste sale desconocido hasta que se fije una tarifa o se configure un modelo con nombre.
- `gemini-3.8-flash` y `gemini-3.6-flash` suben al doble el 2027-01-01; la tarifa del yaml es la vigente hasta el 2026-12-31 y hay que revisarla.
- El resumen se calcula en memoria sobre un máximo de 20 000 filas por consulta: vale para la demo; si crece, hay que agregar en SQL.
- No se miden los embeddings: no pasan por `LlamadorGemini`.
- Las llamadas sin contexto (los evals) se anotan como `sin-contexto` y sin organización: ningún endpoint las muestra.
