# ADR-0001: Java 21 + Spring Boot

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

Proyecto de portfolio orientado al mercado español, con un dominio de helpdesk que debe sostener reglas de negocio (estados, SLA, aislamiento por organización) y más adelante integración con LLM. La restricción de tiempo es de 8–10 h/semana, así que el ecosistema debe traer baterías incluidas: seguridad, persistencia, validación, métricas y tests.

## Opciones

1. **Java 21 + Spring Boot 3.5.x** — a favor: Java aparece en el 32 % y Spring en el 22 % de las ofertas españolas; Testcontainers, Actuator, Security y validación vienen resueltos; LTS con soporte largo; Spring AI encaja en el mismo contenedor. En contra: arranque de JVM más lento (importante en Render Free), más ceremonia que un framework minimalista.
2. **Quarkus** — a favor: arranque rápido y consumo bajo, ideal para cold start. En contra: menos presencia en las ofertas que se quieren enseñar y menos material de referencia para un proyecto en solitario.
3. **Kotlin + Spring Boot** — a favor: concisión y null-safety. En contra: el mercado objetivo pide Java con más frecuencia; añade una curva sin mejorar el relato del portfolio.

## Decisión

Java 21 LTS con Spring Boot 3.5.16. Es el stack con más salida en el mercado al que apunta el proyecto y el que menos infraestructura propia exige. El coste de cold start se mitiga con CDS/AOT (ver ADR-0009), no cambiando de framework. Gradle 9.3.1 con wrapper en `backend/`.

## Consecuencias

- El paquete base es `com.admaal.resolvo`.
- La versión queda fijada en 3.5.16 al crear el esqueleto (fase 0). No se sube de minor dentro del plan.
- El arranque en Render Free se optimiza con CDS/AOT; se mide y se anota el tiempo real.
