# ADR-0011: Evals de IA fuera del gate obligatorio

- Fecha: 2026-11-16
- Estado: aceptada

## Contexto

La fase 4 añade la clasificación de tickets y pide evals con umbral de calidad, coste y latencia. Un eval con valor llama al modelo real: gasta cuota del free tier, depende de un proveedor externo que da 404, 503 o cuota agotada (ya pasó, ADR-0006) y necesita una clave. El `Gate obligatorio` (ADR-0010) debe seguir sin red ni secretos y no puede quedar bloqueado por Google.

## Opciones

1. **Job aparte, no bloqueante** — a favor: el gate sigue determinista y el informe muestra calidad real, coste y latencia. En contra: un PR puede fusionarse con un eval en rojo si nadie mira el job.
2. **Bloqueante en los PR que tocan IA** — a favor: no se degrada en silencio. En contra: un 503 o una cuota agotada de Gemini impide el merge sin que el código tenga la culpa.
3. **Respuestas grabadas** — a favor: determinista y gratis. En contra: no mide la calidad ni la latencia del modelo vigente, que es lo que cambia sin avisar.

## Decisión

Opción 1. Los evals son tests JUnit con la etiqueta `eval`, excluida de `gradlew test`, que se lanzan con `gradlew evals` y en [evals.yml](../../.github/workflows/evals.yml) (a mano y en los PR del mismo repositorio que tocan prompts, `infrastructure/ai` o los evals; nunca `pull_request_target`, y un PR de un fork se salta). El job falla si un umbral no se cumple, pero no entra en el ruleset de `main`.

Lo que no necesita red sí está en el gate: aritmética de métricas, informe, lectura de umbrales y validez de los datasets tienen tests normales en `evals/`. Un test comprueba que el informe nunca contiene la clave del proveedor.

Los datasets y los umbrales viven en `backend/src/test/resources/evals/` (`clasificacion.jsonl`, `rag.jsonl`, `umbrales.json`), con datos ficticios. Medidas y cifras iniciales:

- Calidad: respuestas válidas ≥ 95 %, acierto de categoría ≥ 90 %, prioridad exacta ≥ 70 %, recall@5 del artículo esperado ≥ 80 %, consultas sin respuesta que devuelven sin fuentes ≥ 80 %, borrador que cita la fuente esperada ≥ 90 % (el plan pedía una «precisión de citas» ≥ 90 %; se mide como «el borrador cita el artículo esperado», que es la versión que se puede etiquetar a mano).
- Coste: tokens medios por llamada ≤ 3000. Sin cálculo en euros: no hay tabla de precios versionada y no se inventa; el free tier cuesta 0 €.
- Latencia: p95 por llamada ≤ 15 s. El plan decía 8 s, una estimación; tres corridas reales dieron 10,2, 8,6 y 12,7 s (2026-11-18) y se relajó a 15 s con esa evidencia, a petición del propietario (2026-11-18). Es una decisión sobre el listón, no una mejora: el p95 sigue siendo alto y una llamada mínima ya tarda unos 6 s desde Render.

`UmbralesTest` fija el suelo de cada una de estas cifras. Bajar un umbral para que el eval pase cuenta como debilitar un test (AGENTS.md): se cambia con un cambio de este ADR, no en silencio.

## Consecuencias

- Las cifras de calidad se mantuvieron tras la primera corrida real (2026-11-18): hizo falta ajustar el prompt de clasificación y fijar la distancia del RAG, no rebajar umbrales. Solo la latencia se relajó (de 8 a 15 s, ver arriba), con las tres corridas como evidencia; subirla otra vez exige cambiar este ADR y `UmbralesTest`.
- El eval imprime el umbral de distancia que separa las consultas con y sin respuesta; con él se fijó `resolvo.rag.distancia-maxima` en 0,29 tras la primera corrida real (2026-11-18). Es un margen estrecho sobre 16 consultas con respuesta y 6 sin ella, solo de artículos; hay que recalibrarlo con más casos y con los tickets resueltos como fuentes.
- Cada llamada del eval espera `resolvo.evals.pausa-ms` (4,5 s) para no pasar la cuota por minuto: unas 90 llamadas tardan varios minutos.
- `LlamadorGemini.observar` expone tokens y duración de cada llamada; la fase 5 puede reutilizarlo para las métricas de producción.

## Desviaciones respecto al plan de la fase 4

- Los evals y sus datasets viven en `src/test` (etiqueta `eval`, tarea `evals`) y no en un source set `evals` aparte: una sola compilación y las pruebas de su aritmética corren en el gate normal.
- `Clasificacion` no guarda tokens ni se persisten métricas: es de la fase 5. Los tokens y la duración de cada llamada se observan con `LlamadorGemini.observar`.
- Se añadió `GET /api/tickets/{id}/clasificaciones/actual` (no estaba en el plan) para que la SPA recupere la propuesta pendiente al recargar, igual que las sugerencias.
- El doble clic sobre el mismo ticket llama una sola vez al modelo gracias a un cerrojo por ticket **dentro del proceso**; entre instancias solo el índice único de V6 evita la duplicada y entonces sí se gastan dos llamadas. Render Free corre una instancia.
