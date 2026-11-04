import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { TicketDetalle, UsuarioResumen } from './ticket.model';
import { TicketThread } from './ticket-thread';
import { TicketsService } from './tickets.service';

@Component({
  selector: 'app-detalle',
  imports: [FormsModule, TicketThread],
  template: `
    @if (ticket(); as actual) {
      <p class="codigo">{{ actual.codigo }}</p>
      <h1>{{ actual.asunto }}</h1>
      <p>
        <span class="chip" [class]="claseEstado(actual.estado)">{{ actual.estado }}</span>
        <span class="chip" [class]="claseSla(actual)">{{ textoSla(actual) }}</span>
      </p>
      <p class="cuerpo">{{ actual.cuerpo }}</p>
      @if (puedeGestionar()) {
        <p>
          @for (estado of estados; track estado) {
            <button class="secundario" type="button" (click)="transicionar(estado)">{{ estado }}</button>
          }
        </p>
        @if (error()) { <p class="error">{{ error() }}</p> }
        <label>Asignar
          <select name="agente" [(ngModel)]="agente">
            @for (usuario of agentes(); track usuario.id) {
              <option [value]="usuario.id">{{ usuario.email }}</option>
            }
          </select>
        </label>
        <button class="secundario" type="button" (click)="asignar()">Asignar</button>
      }
      <app-ticket-thread
        [comentarios]="actual.comentarios"
        [puedeNota]="puedeGestionar()"
        [error]="error()"
        (enviado)="comentar($event)" />
    }
  `,
})
export class DetallePage {
  private readonly tickets = inject(TicketsService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  readonly ticket = signal<TicketDetalle | null>(null);
  readonly agentes = signal<UsuarioResumen[]>([]);
  readonly error = signal('');
  readonly estados = ['ABIERTO', 'PENDIENTE', 'RESUELTO', 'CERRADO'];
  agente = '';

  constructor() {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    void this.cargar(id);
    if (this.puedeGestionar()) {
      void this.tickets.usuarios().then((usuarios) => {
        const agentes = usuarios.filter((usuario) => usuario.rol === 'AGENTE' || usuario.rol === 'ADMIN');
        this.agentes.set(agentes);
        this.agente = agentes[0]?.id ?? '';
      });
    }
  }

  puedeGestionar(): boolean {
    return this.auth.rol() !== 'CLIENTE';
  }

  claseEstado(estado: string): string {
    return `chip ${estado.toLowerCase()}`;
  }

  claseSla(ticket: TicketDetalle): string {
    return `chip sla-${nivelSla(ticket)}`;
  }

  textoSla(ticket: TicketDetalle): string {
    const nivel = nivelSla(ticket);
    if (nivel === 'incumplido') return 'SLA incumplido';
    if (nivel === 'riesgo') return 'SLA en riesgo';
    return 'SLA en plazo';
  }

  async transicionar(estado: string): Promise<void> {
    const actual = this.ticket();
    if (!actual) return;
    this.error.set('');
    try {
      this.ticket.set(await this.tickets.transicionar(actual.id, estado));
    } catch (fallo) {
      this.error.set(mensaje(fallo));
      this.ticket.set(await this.tickets.detalle(actual.id));
    }
  }

  async asignar(): Promise<void> {
    const actual = this.ticket();
    if (!actual || !this.agente) return;
    await this.tickets.asignar(actual.id, this.agente);
    this.ticket.set(await this.tickets.detalle(actual.id));
  }

  async comentar(evento: { cuerpo: string; interno: boolean }): Promise<void> {
    const actual = this.ticket();
    if (!actual) return;
    this.error.set('');
    try {
      await this.tickets.comentar(actual.id, evento.cuerpo, evento.interno);
      this.ticket.set(await this.tickets.detalle(actual.id));
    } catch (fallo) {
      this.error.set(mensaje(fallo));
    }
  }

  private async cargar(id: string): Promise<void> {
    this.ticket.set(await this.tickets.detalle(id));
  }
}

function nivelSla(ticket: TicketDetalle): 'ok' | 'riesgo' | 'incumplido' {
  const vence = new Date(ticket.venceEl).getTime();
  const ahora = Date.now();
  if (ahora >= vence) return 'incumplido';
  const abierto = new Date(ticket.abiertoEn).getTime();
  const total = vence - abierto;
  if (total > 0 && (vence - ahora) / total < 0.25) return 'riesgo';
  return 'ok';
}

function mensaje(fallo: unknown): string {
  if (fallo instanceof HttpErrorResponse) {
    return fallo.error?.error ?? 'conflicto';
  }
  return 'conflicto';
}
