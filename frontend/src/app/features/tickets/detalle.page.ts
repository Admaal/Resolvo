import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Categoria, TicketDetalle, UsuarioResumen } from './ticket.model';
import { ClasificacionPanel } from './clasificacion-panel';
import { SugerenciaPanel } from './sugerencia-panel';
import { TicketThread } from './ticket-thread';
import { etiquetaEstado, etiquetaPrioridad, nivelSla, textoSla } from './etiquetas';
import { TicketsService } from './tickets.service';

@Component({
  selector: 'app-detalle',
  imports: [FormsModule, ClasificacionPanel, SugerenciaPanel, TicketThread],
  template: `
    @switch (carga()) {
      @case ('cargando') {
        <p class="estado-carga" role="status">Cargando ticket…</p>
      }
      @case ('error') {
        <p class="error" role="alert">No se pudo cargar el ticket. Puede que no exista o que no tengas acceso.</p>
      }
      @default {
        @if (ticket(); as actual) {
          <p class="codigo">{{ actual.codigo }}</p>
          <h1 class="asunto">{{ actual.asunto }}</h1>
          <div class="chips">
            <span class="chip" [class]="claseEstado(actual.estado)">{{ etiquetaEstado(actual.estado) }}</span>
            <span class="chip" [class]="clasePrioridad(actual.prioridad)">{{ etiquetaPrioridad(actual.prioridad) }}</span>
            <span class="chip" [class]="claseSla(actual)">{{ textoSla(nivel(actual)) }}</span>
          </div>
          <p class="cuerpo">{{ actual.cuerpo }}</p>
          @if (puedeGestionar()) {
            <div class="acciones transiciones" role="group" aria-label="Cambiar estado">
              @for (estado of estados; track estado) {
                <button class="btn secundario" type="button" [disabled]="estado === actual.estado" (click)="transicionar(estado)">{{ etiquetaEstado(estado) }}</button>
              }
            </div>
            @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
            <div class="asignar">
              <label>Asignar a
                <select name="agente" [(ngModel)]="agente">
                  @for (usuario of agentes(); track usuario.id) {
                    <option [value]="usuario.id">{{ usuario.email }}</option>
                  }
                </select>
              </label>
              <button class="btn secundario" type="button" (click)="asignar()">Asignar</button>
            </div>
          }
          <app-clasificacion-panel [ticketId]="actual.id" [categorias]="categorias()" (aplicada)="refrescar()" />
          <app-sugerencia-panel [ticketId]="actual.id" (aprobada)="refrescar()" />
          <app-ticket-thread
            [comentarios]="actual.comentarios"
            [puedeNota]="puedeGestionar()"
            [error]="errorComentario()"
            (enviado)="comentar($event)" />
        }
      }
    }
  `,
})
export class DetallePage {
  private readonly tickets = inject(TicketsService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  readonly ticket = signal<TicketDetalle | null>(null);
  readonly agentes = signal<UsuarioResumen[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly error = signal('');
  readonly errorComentario = signal('');
  readonly carga = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly etiquetaEstado = etiquetaEstado;
  protected readonly etiquetaPrioridad = etiquetaPrioridad;
  protected readonly textoSla = textoSla;
  readonly estados = ['ABIERTO', 'PENDIENTE', 'RESUELTO', 'CERRADO'];
  agente = '';

  constructor() {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    void this.cargar(id);
    if (this.puedeGestionar()) {
      void this.tickets.categorias().then((categorias) => this.categorias.set(categorias));
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

  clasePrioridad(prioridad: string): string {
    return `chip prioridad-${prioridad.toLowerCase()}`;
  }

  nivel(ticket: TicketDetalle) {
    return nivelSla(ticket);
  }

  claseSla(ticket: TicketDetalle): string {
    return `chip sla-${nivelSla(ticket)}`;
  }

  async transicionar(estado: string): Promise<void> {
    const actual = this.ticket();
    if (!actual) return;
    this.error.set('');
    try {
      this.ticket.set(await this.tickets.transicionar(actual.id, estado));
    } catch (fallo) {
      this.error.set(mensajeDeError(fallo));
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
    this.errorComentario.set('');
    try {
      await this.tickets.comentar(actual.id, evento.cuerpo, evento.interno);
      this.ticket.set(await this.tickets.detalle(actual.id));
    } catch (fallo) {
      this.errorComentario.set(mensajeDeError(fallo));
    }
  }

  async refrescar(): Promise<void> {
    const actual = this.ticket();
    if (actual) this.ticket.set(await this.tickets.detalle(actual.id));
  }

  private async cargar(id: string): Promise<void> {
    try {
      this.ticket.set(await this.tickets.detalle(id));
      this.carga.set('listo');
    } catch {
      this.carga.set('error');
    }
  }
}
