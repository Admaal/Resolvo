# ADR-0013: Seguridad de la IA y límites de uso

- Fecha: 2026-11-19
- Estado: aceptada

## Contexto

La IA recibe texto de terceros (el cliente que abre el ticket, los comentarios de tickets antiguos y artículos) y gasta una cuota compartida de Gemini. La demo es pública, con cuentas y contraseña a la vista, y corre en una sola instancia de Render Free. Las fases 3 a 5 solo quitaban las etiquetas `<ticket>` del texto del cliente: las fuentes del RAG entraban sin sanear, ningún dato personal se enmascaraba, no había límite de peticiones ni de gasto, y el bloqueo de login por cuenta permitía a cualquiera dejar fuera a todos.

## Opciones

1. **Defensa en capas dentro de la API, en memoria** — a favor: sin dependencias nuevas, cada capa se prueba con tests unitarios y encaja con una sola instancia. En contra: los contadores se pierden al reiniciar y no valen con varias instancias.
2. **Bucket4j, Redis o un WAF delante** — a favor: límites distribuidos y más finos. En contra: infraestructura de pago o más piezas para una demo que cuesta 0 €.
3. **No limitar y confiar en la cuota del proveedor** — a favor: nada que mantener. En contra: un usuario agota la cuota de todos y el proveedor decide el fallo.

## Decisión

La opción 1.

- **Prompt injection.** Texto del cliente y fuentes del RAG van como datos entre delimitadores (`<ticket>`, `<fuente id="…">`) y `PlantillaDePrompt.sinDelimitadores` los quita, repetidamente, del texto de terceros (también con espacios, mayúsculas o anidados). El prompt `redactar-respuesta-v2` dice que lo de dentro no son instrucciones. La salida se valida igual que antes: solo se citan fragmentos recuperados por el servidor, la categoría tiene que estar en el catálogo y el borrador se recorta a 4000 caracteres. La defensa real es que una persona aprueba todo (ADR-0006): la inyección solo puede producir un borrador que alguien revisa.
- **PII.** `EnmascaradorPii` (dominio, puro) sustituye emails, teléfonos españoles, DNI/NIE con letra de control válida, IBAN de dígitos y tarjetas que pasan Luhn por `[EMAIL]`, `[TELEFONO]`, `[DNI]`, `[IBAN]` y `[TARJETA]`. Se aplica a todo lo que sale hacia Gemini (prompts de redactar y clasificar, y embeddings). Lo guardado en la base de datos no cambia.
- **Rate limit.** `LimiteDeFrecuencia` (dominio) es una ventana deslizante por clave. Las rutas de IA admiten 10 peticiones por usuario y minuto (`LimiteDeIa`, un interceptor sobre sugerencias, clasificaciones y reindexado). Login cuenta cada intento (5 por (email, cliente) cada 15 minutos, `LimiteDeLogin`) *antes* de comprobar la contraseña, para que una ráfaga en paralelo no pase del máximo, y un login correcto reinicia la cuenta. El email de la clave se recorta a 254 caracteres y las claves caducadas se purgan solas al pasar de 1000. Respuesta `429` con `Retry-After` y `demasiadas-peticiones` o `demasiados-intentos`.
- **Quién es el cliente.** Según la documentación de Cloudflare, en una subpetición de un Worker a otra zona de Cloudflare (Pages a Render), `CF-Connecting-IP` llega como la IP del Worker, la misma para todos: con ella, cinco intentos de cualquiera bloquearían esa cuenta demo 15 minutos para todos. Por eso la Pages Function manda la IP real en `X-Cliente-Ip` junto con una clave compartida en `X-Resolvo-Proxy` (`PROXY_CLAVE` en Pages, `RESOLVO_PROXY_CLAVE` en Render), descarta lo que el navegador mande en esas dos cabeceras y la API solo acepta `X-Cliente-Ip` si la clave coincide (comparación en tiempo constante). Sin clave configurada nunca se acepta. Si no, la IP sale de `CF-Connecting-IP`, que Cloudflare impone y no se puede forjar (falsificarla da 403, comprobado en producción tanto vía Pages como directo a Render), y por último de la dirección remota. Quien llame directo a Render no tiene la clave: su clave de límite es la de su IP real.
- **Presupuesto diario.** `PresupuestoDiario` (dominio) fija llamadas y tokens por organización y día UTC (200 y 300 000 por defecto, `resolvo.ia.presupuesto-diario.*`). `PresupuestoConReservas` suma lo registrado en `llamada_ia` más las reservas abiertas en memoria y reserva bajo un cerrojo, así que dos peticiones simultáneas no pasan por la última llamada libre. Las llamadas son un tope estricto; los tokens, blando (se conocen al terminar). La reserva se hace después del atajo de «ya hay una pendiente», que no gasta modelo. Al superarlo, `429 ia-presupuesto-agotado`.
- **Bloqueo por cuenta.** Se retira: `IniciarSesion` ya no registra fallos y `CuentaBloqueada` desaparece. Las columnas `fallos_seguidos` y `bloqueado_hasta` se quedan sin uso, sin migración.

## Consecuencias

- Los contadores viven en el proceso: un reinicio de Render los pone a cero y dos instancias sumarían el doble de cupo. Si hubiera más de una, habría que mover los límites a la base de datos.
- Un cliente que se bloquea con sus propios fallos espera hasta 15 minutos; otro cliente con la misma cuenta sigue entrando.
- Los fragmentos ya indexados conservan sus datos personales en `fragmento.texto`: solo se enmascara al enviar. Si se quisiera no almacenarlos, habría que enmascarar al indexar y reindexar.
- El enmascarado es heurístico: no detecta nombres propios, direcciones ni IBAN con letras, y un número largo que no es una tarjeta puede quedar sin tapar o tapado de más.
- Los evals miden la resistencia con dos casos de clasificación con instrucciones inyectadas (`c33` y `c34`); la fuente envenenada del RAG solo se cubre con tests unitarios, porque los evals usan los artículos de la semilla.
- Las respuestas de IA tienen un `429` nuevo que la SPA traduce en los paneles de sugerencia y clasificación y en el login.
- El presupuesto solo cuenta lo que deja fila en `llamada_ia` (chat de sugerir y clasificar): los embeddings, el reindexado y el indexado por eventos no gastan presupuesto. La consulta de consumo corre dentro de un cerrojo global: si Neon tarda en despertar, esperan todas las peticiones de IA de todas las organizaciones.
- Con las cuentas demo compartidas, un solo visitante a 10 peticiones por minuto agota las 200 llamadas diarias de Acme en 20 minutos y deja la IA sin servicio hasta medianoche UTC.
- El enmascarado no tapa DNI con guion o espacio (`12345678-Z`), teléfonos con puntos o prefijo `0034`, ni IBAN en minúsculas, y sí tapa como teléfono cualquier número de 9 cifras que empiece por 6 a 9 (por ejemplo, un número de pedido).
