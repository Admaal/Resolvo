import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { Clasificacion, PRIORIDADES, Prioridad } from './clasificacion.model';
import { ClasificacionesService } from './clasificaciones.service';
import { Categoria } from './ticket.model';

@Component({
  selector: 'app-clasificacion-panel',
  template: `
    @if (visible()) {
      <section class="clasificacion">
        @if (clasificacion(); as actual) {
          <p class="marca-ia chip">Clasificación sugerida por IA: revísala antes de aplicar</p>
          <p class="motivo">{{ actual.motivo }}</p>
          <label>Categoría
            <select class="categoria" name="categoriaIa" [value]="categoriaId()" (change)="categoriaId.set($any($event.target).value)">
              @for (categoria of categorias(); track categoria.id) {
                <option [value]="categoria.id" [selected]="categoria.id === categoriaId()">{{ categoria.nombre }}</option>
              }
            </select>
          </label>
          <label>Prioridad
            <select class="prioridad" name="prioridadIa" [value]="prioridad()" (change)="prioridad.set($any($event.target).value)">
              @for (opcion of prioridades; track opcion) {
                <option [value]="opcion" [selected]="opcion === prioridad()">{{ opcion }}</option>
              }
            </select>
          </label>
          <p>
            <button class="aplicar-clasificacion primario" type="button" [disabled]="enviando()" (click)="aplicar()">
              Aplicar
            </button>
            <button class="descartar-clasificacion secundario" type="button" [disabled]="enviando()" (click)="descartar()">
              Descartar
            </button>
          </p>
        } @else {
          <button class="sugerir-clasificacion secundario" type="button" [disabled]="cargando()" (click)="sugerir()">
            {{ cargando() ? 'Clasificando…' : 'Sugerir clasificación' }}
          </button>
        }
        @if (aviso()) { <p class="aviso error">{{ aviso() }}</p> }
      </section>
    }
  `,
})
export class ClasificacionPanel implements OnInit {
  private readonly api = inject(ClasificacionesService);
  private readonly auth = inject(AuthService);

  readonly ticketId = input.required<string>();
  readonly categorias = input.required<Categoria[]>();
  readonly aplicada = output<void>();

  readonly prioridades = PRIORIDADES;
  readonly clasificacion = signal<Clasificacion | null>(null);
  readonly categoriaId = signal('');
  readonly prioridad = signal<Prioridad>('MEDIA');
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

  async aplicar(): Promise<void> {
    const actual = this.clasificacion();
    if (!actual || this.enviando()) return;
    this.enviando.set(true);
    this.aviso.set('');
    try {
      await this.api.aplicar(actual.id, this.categoriaId(), this.prioridad());
      this.poner(null);
      this.aplicada.emit();
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
    const actual = this.clasificacion();
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

  private poner(clasificacion: Clasificacion | null): void {
    this.clasificacion.set(clasificacion);
    if (clasificacion) {
      this.categoriaId.set(clasificacion.categoriaId);
      this.prioridad.set(clasificacion.prioridad);
    }
  }
}

function textoDeAviso(fallo: unknown): string {
  const codigo = fallo instanceof HttpErrorResponse ? (fallo.error as { error?: string } | null)?.error : undefined;
  switch (codigo) {
    case 'ia-no-configurada':
      return 'La IA no está disponible en este entorno.';
    case 'ia-cuota-agotada':
      return 'Cuota de IA agotada, inténtalo más tarde.';
    case 'ia-no-disponible':
      return 'La IA no responde, inténtalo más tarde.';
    case 'clasificacion-invalida':
      return 'La IA propuso una clasificación no válida, inténtalo de nuevo.';
    case 'conflicto':
      return 'Esta clasificación ya fue resuelta o el ticket ya no admite cambios.';
    default:
      return 'No se pudo completar la acción.';
  }
}
