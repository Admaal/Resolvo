import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { Articulo, CitaSugerencia, Sugerencia } from './sugerencia.model';
import { SugerenciasService } from './sugerencias.service';

@Component({
  selector: 'app-sugerencia-panel',
  imports: [MatButton, RouterLink],
  template: `
    @if (visible()) {
      <section class="sugerencia">
        @if (sugerencia(); as actual) {
          <p class="marca-ia chip">Borrador generado por IA: revísalo antes de publicar</p>
          <label>Respuesta sugerida
            <textarea
              class="borrador"
              name="borradorIa"
              [value]="texto()"
              (input)="texto.set($any($event.target).value)"></textarea>
          </label>
          @if (actual.citas.length > 0) {
            <p class="ayuda">Fuentes</p>
            <ol class="citas">
              @for (cita of actual.citas; track cita.orden) {
                <li>
                  @if (cita.fuente === 'TICKET') {
                    <a class="cita cita-ticket" [routerLink]="['/tickets', cita.fuenteId]">{{ cita.titulo }}</a>
                  } @else {
                    <button class="cita cita-articulo secundario" type="button" (click)="verArticulo(cita)">
                      {{ cita.titulo }}
                    </button>
                  }
                </li>
              }
            </ol>
          }
          @if (articulo(); as leido) {
            <div class="articulo-citado">
              <strong>{{ leido.titulo }}</strong>
              <p class="cuerpo">{{ leido.cuerpo }}</p>
            </div>
          }
          <p>
            <button
              matButton
              class="aprobar primario"
              type="button"
              [disabled]="enviando() || texto().trim() === ''"
              (click)="aprobar()">
              Aprobar y publicar
            </button>
            <button class="descartar secundario" type="button" [disabled]="enviando()" (click)="descartar()">
              Descartar
            </button>
          </p>
        } @else {
          <button class="sugerir secundario" type="button" [disabled]="cargando()" (click)="sugerir()">
            {{ cargando() ? 'Generando…' : 'Sugerir respuesta' }}
          </button>
        }
        @if (aviso()) { <p class="aviso error">{{ aviso() }}</p> }
      </section>
    }
  `,
})
export class SugerenciaPanel implements OnInit {
  private readonly api = inject(SugerenciasService);
  private readonly auth = inject(AuthService);

  readonly ticketId = input.required<string>();
  readonly aprobada = output<void>();

  readonly sugerencia = signal<Sugerencia | null>(null);
  readonly articulo = signal<Articulo | null>(null);
  readonly texto = signal('');
  readonly aviso = signal('');
  readonly cargando = signal(false);
  readonly enviando = signal(false);
  readonly visible = computed(() => {
    const rol = this.auth.rol();
    return rol !== null && rol !== 'CLIENTE';
  });

  async ngOnInit(): Promise<void> {
    if (!this.visible()) return;
    try {
      this.poner(await this.api.actual(this.ticketId()));
    } catch (fallo) {
      this.aviso.set(textoDeAviso(fallo));
    }
  }

  async sugerir(): Promise<void> {
    if (this.cargando()) return;
    this.cargando.set(true);
    this.aviso.set('');
    try {
      this.poner(await this.api.sugerir(this.ticketId()));
    } catch (fallo) {
      this.aviso.set(textoDeAviso(fallo));
    } finally {
      this.cargando.set(false);
    }
  }

  async aprobar(): Promise<void> {
    const actual = this.sugerencia();
    if (!actual || this.enviando() || this.texto().trim() === '') return;
    this.enviando.set(true);
    this.aviso.set('');
    try {
      await this.api.aprobar(actual.id, this.texto());
      this.poner(null);
      this.aprobada.emit();
    } catch (fallo) {
      this.aviso.set(textoDeAviso(fallo));
      if (fallo instanceof HttpErrorResponse && fallo.status === 409) {
        this.poner(await this.api.actual(this.ticketId()).catch(() => null));
      }
    } finally {
      this.enviando.set(false);
    }
  }

  async descartar(): Promise<void> {
    const actual = this.sugerencia();
    if (!actual || this.enviando()) return;
    this.enviando.set(true);
    this.aviso.set('');
    try {
      await this.api.descartar(actual.id);
      this.poner(null);
    } catch (fallo) {
      this.aviso.set(textoDeAviso(fallo));
    } finally {
      this.enviando.set(false);
    }
  }

  async verArticulo(cita: CitaSugerencia): Promise<void> {
    try {
      this.articulo.set(await this.api.articulo(cita.fuenteId));
    } catch (fallo) {
      this.aviso.set(textoDeAviso(fallo));
    }
  }

  private poner(sugerencia: Sugerencia | null): void {
    this.sugerencia.set(sugerencia);
    this.texto.set(sugerencia?.borrador ?? '');
    this.articulo.set(null);
  }
}

function textoDeAviso(fallo: unknown): string {
  const codigo = fallo instanceof HttpErrorResponse ? (fallo.error as { error?: string } | null)?.error : undefined;
  switch (codigo) {
    case 'sin-fuentes':
      return 'No hay fuentes en la base de conocimiento para este ticket.';
    case 'ia-no-configurada':
      return 'La IA no está disponible en este entorno.';
    case 'ia-cuota-agotada':
      return 'Cuota de IA agotada, inténtalo más tarde.';
    case 'demasiadas-peticiones':
      return 'Demasiadas peticiones, espera un minuto e inténtalo de nuevo.';
    case 'ia-presupuesto-agotado':
      return 'Se alcanzó el límite de IA de hoy; se renueva a medianoche (UTC).';
    case 'ia-no-disponible':
      return 'La IA no responde, inténtalo más tarde.';
    case 'conflicto':
      return 'Esta sugerencia ya fue resuelta por otra persona.';
    default:
      return 'No se pudo completar la acción.';
  }
}
