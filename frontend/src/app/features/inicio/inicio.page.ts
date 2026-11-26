import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccesosDemo } from '../../core/accesos-demo';
import { AuthService } from '../../core/auth.service';
import { alternarTema } from '../../core/tema';
import { EstadoIcono } from '../../shared/estado-icono';
import { Icono } from '../../shared/icono';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, AccesosDemo, Icono, EstadoIcono],
  template: `
    <div class="inicio">
      <header class="inicio-cabecera">
        <a class="marca" routerLink="/" aria-label="Resolvo, inicio">
          <span class="marca-logo" aria-hidden="true"><rs-icono nombre="check-circle" [tam]="14" /></span>
          Resolvo
        </a>
        <div class="inicio-acciones">
          <button class="btn fantasma tema" type="button" (click)="tema()">
            <rs-icono [nombre]="oscuro() ? 'sun' : 'moon'" />
            <span class="solo-ancho">Tema {{ oscuro() ? 'claro' : 'oscuro' }}</span>
          </button>
          @if (conSesion()) {
            <a class="btn secundario" routerLink="/tickets">Ir a la bandeja</a>
          } @else {
            <a class="btn secundario" routerLink="/login">Iniciar sesión</a>
          }
        </div>
      </header>

      <main>
        <section class="heroe">
          <p class="etiqueta"><rs-icono nombre="sparkles" [tam]="14" /> Helpdesk con IA verificable</p>
          <h1>Soporte más rápido, con respuestas que enseñan su fuente</h1>
          <p class="lead">
            Resolvo propone la respuesta, te dice de qué artículos sale y tú la apruebas antes de enviarla.
            Con el plazo de SLA siempre a la vista.
          </p>
          @if (conSesion()) {
            <div class="cta">
              <a class="btn primario grande" routerLink="/tickets">Ir a la bandeja</a>
            </div>
          } @else {
            <div class="cta">
              <app-accesos-demo />
              <p class="ayuda">Demostración con datos de ejemplo: sin registro y sin contraseñas que recordar.</p>
            </div>
          }

          <div class="maqueta" aria-hidden="true">
            <div class="maqueta-lista">
              <p class="maqueta-titulo">Todos los tickets</p>
              <div class="maqueta-grupo"><rs-icono nombre="alert" [tam]="12" /> SLA incumplido <span>1</span></div>
              <div class="maqueta-fila">
                <span class="fila-texto">
                  <span class="fila-cabecera">
                    <span class="codigo">RS-24</span>
                    <span class="sla sla-incumplido"><rs-icono nombre="alert" [tam]="12" />−1h 45m</span>
                  </span>
                  <span class="fila-asunto">No puedo iniciar sesión</span>
                  <span class="fila-meta">
                    <span class="fila-estado"><rs-estado estado="ABIERTO" /> Abierto</span>
                    <span class="fila-prioridad prioridad-urgente"><rs-icono nombre="zap" [tam]="12" /> Urgente</span>
                  </span>
                </span>
              </div>
              <div class="maqueta-grupo"><rs-icono nombre="inbox" [tam]="12" /> Resto <span>2</span></div>
              <div class="maqueta-fila activa">
                <span class="fila-texto">
                  <span class="fila-cabecera">
                    <span class="codigo">RS-21</span>
                    <span class="sla sla-ok"><rs-icono nombre="clock" [tam]="12" />3h 20m</span>
                  </span>
                  <span class="fila-asunto">Factura duplicada</span>
                  <span class="fila-meta">
                    <span class="fila-estado"><rs-estado estado="PENDIENTE" /> Pendiente</span>
                    <span class="fila-prioridad prioridad-alta"><rs-icono nombre="zap" [tam]="12" /> Alta</span>
                  </span>
                </span>
              </div>
              <div class="maqueta-fila">
                <span class="fila-texto">
                  <span class="fila-cabecera"><span class="codigo">RS-18</span></span>
                  <span class="fila-asunto">Cambiar datos de la cuenta</span>
                  <span class="fila-meta">
                    <span class="fila-estado"><rs-estado estado="RESUELTO" /> Resuelto</span>
                  </span>
                </span>
              </div>
            </div>
            <div class="maqueta-ia">
              <p class="maqueta-titulo"><rs-icono nombre="sparkles" [tam]="12" /> Asistente</p>
              <p class="maqueta-borrador">
                Hola, puedes restablecer tu contraseña desde «¿Olvidaste tu contraseña?» en la pantalla de inicio.
                El enlace caduca en 30 minutos.
              </p>
              <p><span class="cita">Restablecer la contraseña</span> <span class="cita">RS-12</span></p>
              <div class="maqueta-botones"><span class="btn primario">Aprobar y publicar</span><span class="btn secundario">Descartar</span></div>
            </div>
          </div>
        </section>

        <section class="valor" aria-labelledby="valor-titulo">
          <h2 id="valor-titulo" class="visually-hidden">Qué hace Resolvo</h2>
          <article>
            <rs-icono nombre="file-text" [tam]="20" />
            <h3>Respuestas con citas</h3>
            <p>Cada borrador de la IA enseña los artículos y los tickets resueltos de los que sale. Si no hay fuentes, no inventa.</p>
          </article>
          <article>
            <rs-icono nombre="clock" [tam]="20" />
            <h3>El SLA, siempre a la vista</h3>
            <p>La bandeja se agrupa por urgencia: primero lo incumplido, luego lo que está en riesgo. El tiempo restante va en cada fila.</p>
          </article>
          <article>
            <rs-icono nombre="shield-check" [tam]="20" />
            <h3>La IA propone, tú decides</h3>
            <p>Editas el borrador, lo apruebas o lo descartas. Nada llega al cliente sin que una persona lo apruebe.</p>
          </article>
        </section>

        <section class="pasos" aria-labelledby="pasos-titulo">
          <h2 id="pasos-titulo">Cómo funciona</h2>
          <ol>
            <li><b>1</b><span>Un cliente abre un ticket y se calcula su plazo de SLA.</span></li>
            <li><b>2</b><span>El agente pide una sugerencia: la IA busca en la base de conocimiento y redacta con citas.</span></li>
            <li><b>3</b><span>El agente revisa, edita y aprueba: la respuesta se publica en la conversación.</span></li>
          </ol>
        </section>
      </main>

      <footer class="inicio-pie">
        <p>Resolvo es un proyecto de portfolio. <a href="https://github.com/Admaal/Resolvo" rel="noopener">Código en GitHub</a>.</p>
      </footer>
    </div>
  `,
})
export class InicioPage {
  private readonly auth = inject(AuthService);
  protected readonly conSesion = computed(() => this.auth.rol() !== null);
  protected readonly oscuro = signal(document.documentElement.dataset['theme'] !== 'light');

  protected tema(): void {
    this.oscuro.set(alternarTema() === 'dark');
  }
}
