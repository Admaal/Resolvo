# Fases

Historial. La fase vigente está en `AGENTS.md`, no aquí.

| Fecha | Fase | Motivo de cierre |
| --- | --- | --- |
| 2026-10-31 | 0 valla | Esqueleto Spring Boot 3.5.16, ArchUnit con las dos reglas comprobadas en rojo, PIT al 80 % verificado con una clase temporal, hooks y CI llamando a `scripts/gates.ps1`, presupuestos medidos. |
| 2026-11-04 | 1 sesión, tickets y bandeja local | API local con sesión, SLA y aislamiento por organización, y SPA Angular para entrar, abrir, listar y comentar. Sin IA y sin despliegue. |
| 2026-11-15 | 2 MVP desplegado | SPA en Cloudflare Pages (`resolvo-6lc.pages.dev`) con proxy a la API en Render Free, Postgres en Neon Free, semilla de demo `V3` idempotente, aviso de servidor dormido y ping cada 5 min. Login de demo comprobado a mano en la URL pública. |
| 2026-11-16 | 3 RAG con pgvector y sugerencias | Sugerencias con citas y aprobación humana funcionando en la demo pública. Dominio y casos de uso (PR #2), Spring AI 1.1.8 con perfil `gemini`, pgvector con búsqueda exacta por organización y panel en la SPA. En Neon, V4 y V5 aplicadas; probado en producción: sugerencia `APROBADA` con `gemini-flash-lite-latest`, 1 cita y 1 comentario público. El primer modelo fijo (`gemini-2.5-flash`) estaba retirado: la app sondea una lista de modelos al arrancar y usa el que responde (PR #4 y #5). Excepción documentada en AGENTS.md: RED de los PR 1 a 3 reconstruido. |
