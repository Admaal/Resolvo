import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { etiquetaEstado, etiquetaPrioridad, nivelSla, textoSla } from './etiquetas';
import { TicketResumen } from './ticket.model';
import { TicketsService } from './tickets.service';

const TITULOS: Record<string, string> = {
  mios: 'Mis tickets',
  sin: 'Sin asignar',
  todos: 'Todos los tickets',
};

@Component({
  selector: 'app-bandeja',
  imports: [RouterLink],
  template: `
    <h1>{{ titulo() }}</h1>
    @switch (carga()) {
      @case ('cargando') {
        <p class="estado-carga" role="status">Cargando tickets…</p>
      }
      @case ('error') {
        <p class="error" role="alert">No se pudieron cargar los tickets. Recarga la página e inténtalo de nuevo.</p>
      }
      @default {
        @if (visibles().length > 0) {
          <ul class="lista-tickets">
            @for (ticket of visibles(); track ticket.id) {
              <li>
                <a class="fila" [routerLink]="['/tickets', ticket.id]">
                  <span class="codigo">{{ ticket.codigo }}</span>
                  <span>{{ ticket.asunto }}</span>
                  <span class="chip" [class]="clasePrioridad(ticket)">{{ etiquetaPrioridad(ticket.prioridad) }}</span>
                  <span class="chip" [class]="claseEstado(ticket.estado)">{{ etiquetaEstado(ticket.estado) }}</span>
                  <span class="chip" [class]="claseSla(ticket)">{{ textoSla(nivel(ticket)) }}</span>
                </a>
              </li>
            }
          </ul>
        } @else {
          <p class="estado-vacio">No hay tickets en esta vista.</p>
        }
      }
    }
  `,
})
export class BandejaPage {
  private readonly tickets = inject(TicketsService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly todos = signal<TicketResumen[]>([]);
  private readonly vista = signal('todos');
  readonly carga = signal<'cargando' | 'listo' | 'error'>('cargando');

  protected readonly etiquetaEstado = etiquetaEstado;
  protected readonly etiquetaPrioridad = etiquetaPrioridad;
  protected readonly textoSla = textoSla;

  constructor() {
    this.route.queryParamMap.subscribe((params) => this.vista.set(params.get('vista') ?? 'todos'));
    this.tickets
      .listar({})
      .then((pagina) => {
        this.todos.set(pagina.elementos);
        this.carga.set('listo');
      })
      .catch(() => this.carga.set('error'));
  }

  titulo(): string {
    return TITULOS[this.vista()] ?? TITULOS['todos'];
  }

  visibles(): TicketResumen[] {
    const yo = this.auth.usuarioId();
    return this.todos().filter((ticket) => {
      if (this.vista() === 'sin') return ticket.asignadoId == null;
      if (this.vista() === 'mios') return ticket.autorId === yo || ticket.asignadoId === yo;
      return true;
    });
  }

  nivel(ticket: TicketResumen) {
    return nivelSla(ticket);
  }

  claseEstado(estado: string): string {
    return `chip ${estado.toLowerCase()}`;
  }

  clasePrioridad(ticket: TicketResumen): string {
    return `chip prioridad-${ticket.prioridad.toLowerCase()}`;
  }

  claseSla(ticket: TicketResumen): string {
    return `chip sla-${nivelSla(ticket)}`;
  }
}
