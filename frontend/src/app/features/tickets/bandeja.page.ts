import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { TicketResumen } from './ticket.model';
import { TicketsService } from './tickets.service';

@Component({
  selector: 'app-bandeja',
  imports: [RouterLink],
  template: `
    <h1>Bandeja</h1>
    @for (ticket of visibles(); track ticket.id) {
      <a class="fila" [routerLink]="['/tickets', ticket.id]">
        <span class="codigo">{{ ticket.codigo }}</span>
        <span>{{ ticket.asunto }}</span>
        <span class="chip" [class]="claseEstado(ticket.estado)">{{ etiqueta(ticket.estado) }}</span>
        <span class="chip" [class]="claseSla(ticket)">{{ textoSla(ticket) }}</span>
      </a>
    } @empty {
      <p class="ayuda">No hay tickets en esta vista.</p>
    }
  `,
})
export class BandejaPage {
  private readonly tickets = inject(TicketsService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly todos = signal<TicketResumen[]>([]);
  private readonly vista = signal('todos');

  constructor() {
    this.route.queryParamMap.subscribe((params) => this.vista.set(params.get('vista') ?? 'todos'));
    void this.tickets.listar({}).then((pagina) => this.todos.set(pagina.elementos));
  }

  visibles(): TicketResumen[] {
    const yo = this.auth.usuarioId();
    return this.todos().filter((ticket) => {
      if (this.vista() === 'sin') return ticket.asignadoId == null;
      if (this.vista() === 'mios') return ticket.autorId === yo || ticket.asignadoId === yo;
      return true;
    });
  }

  claseEstado(estado: string): string {
    return `chip ${estado.toLowerCase()}`;
  }

  etiqueta(estado: string): string {
    return estado.charAt(0) + estado.slice(1).toLowerCase();
  }

  claseSla(ticket: TicketResumen): string {
    return `chip sla-${this.sla(ticket)}`;
  }

  textoSla(ticket: TicketResumen): string {
    const nivel = this.sla(ticket);
    if (nivel === 'incumplido') return 'SLA incumplido';
    if (nivel === 'riesgo') return 'SLA en riesgo';
    return 'SLA en plazo';
  }

  private sla(ticket: TicketResumen): 'ok' | 'riesgo' | 'incumplido' {
    const vence = new Date(ticket.venceEl).getTime();
    const ahora = Date.now();
    if (ahora >= vence) return 'incumplido';
    const abierto = ticket.abiertoEn ? new Date(ticket.abiertoEn).getTime() : ahora;
    const total = vence - abierto;
    if (total > 0 && (vence - ahora) / total < 0.25) return 'riesgo';
    return 'ok';
  }
}
