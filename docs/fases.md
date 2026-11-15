# Fases

Historial. La fase vigente está en `AGENTS.md`, no aquí.

| Fecha | Fase | Motivo de cierre |
| --- | --- | --- |
| 2026-10-31 | 0 valla | Esqueleto Spring Boot 3.5.16, ArchUnit con las dos reglas comprobadas en rojo, PIT al 80 % verificado con una clase temporal, hooks y CI llamando a `scripts/gates.ps1`, presupuestos medidos. |
| 2026-11-04 | 1 sesión, tickets y bandeja local | API local con sesión, SLA y aislamiento por organización, y SPA Angular para entrar, abrir, listar y comentar. Sin IA y sin despliegue. |
| 2026-11-15 | 2 MVP desplegado | SPA en Cloudflare Pages (`resolvo-6lc.pages.dev`) con proxy a la API en Render Free, Postgres en Neon Free, semilla de demo `V3` idempotente, aviso de servidor dormido y ping cada 5 min. Login de demo comprobado a mano en la URL pública. |
