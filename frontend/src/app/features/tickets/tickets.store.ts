import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { nivelSla } from './etiquetas';
import { TicketResumen } from './ticket.model';
import { TicketsService } from './tickets.service';

export type Vista = 'mios' | 'sin' | 'todos' | 'riesgo' | 'resueltos';
export const VISTAS: Vista[] = ['mios', 'sin', 'todos', 'riesgo', 'resueltos'];

const abierto = (ticket: TicketResumen) => ticket.estado === 'ABIERTO' || ticket.estado === 'PENDIENTE';

export function filtrarPorVista(tickets: TicketResumen[], vista: string, yo: string | null, ahora: number = Date.now()): TicketResumen[] {
  switch (vista) {
    case 'mios':
      return tickets.filter((t) => t.autorId === yo || t.asignadoId === yo);
    case 'sin':
      return tickets.filter((t) => t.asignadoId == null);
    case 'riesgo':
      return tickets.filter((t) => abierto(t) && nivelSla(t, ahora) !== 'ok');
    case 'resueltos':
      return tickets.filter((t) => !abierto(t));
    default:
      return tickets;
  }
}

export function contarPorVista(tickets: TicketResumen[], yo: string | null, ahora: number = Date.now()): Record<Vista, number> {
  const cuenta = (vista: Vista) => filtrarPorVista(tickets, vista, yo, ahora).length;
  return { mios: cuenta('mios'), sin: cuenta('sin'), todos: cuenta('todos'), riesgo: cuenta('riesgo'), resueltos: cuenta('resueltos') };
}

/** Los tickets de la organización, compartidos por la barra lateral, la bandeja y el detalle. */
@Injectable({ providedIn: 'root' })
export class TicketsStore {
  private readonly api = inject(TicketsService);
  private readonly auth = inject(AuthService, { optional: true });
  readonly tickets = signal<TicketResumen[]>([]);
  readonly carga = signal<'cargando' | 'listo' | 'error'>('cargando');
  readonly contadores = computed(() => contarPorVista(this.tickets(), this.auth?.usuarioId() ?? null));

  async cargar(): Promise<void> {
    try {
      // Los contadores y las vistas se calculan aquí, así que hacen falta todas las páginas.
      const todos: TicketResumen[] = [];
      for (let numero = 0; ; numero++) {
        const pagina = await this.api.listar({}, numero);
        todos.push(...pagina.elementos);
        if (pagina.elementos.length === 0 || todos.length >= pagina.total) break;
      }
      this.tickets.set(todos);
      this.carga.set('listo');
    } catch {
      this.carga.set('error');
    }
  }
}
