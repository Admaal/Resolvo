import { DatePipe } from '@angular/common';
import { Component, HostListener, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { mensajeDeError } from '../../core/errores';
import { Icono } from '../../shared/icono';
import { ordenDeBandeja } from './agrupacion';
import { Categoria, TicketDetalle, UsuarioResumen } from './ticket.model';
import { ClasificacionPanel } from './clasificacion-panel';
import { SugerenciaPanel } from './sugerencia-panel';
import { TicketThread } from './ticket-thread';
import { etiquetaEstado, etiquetaPrioridad, nivelSla, slaCorre, textoSla, tiempoSla } from './etiquetas';
import { TicketsService } from './tickets.service';
import { TicketsStore, filtrarPorVista } from './tickets.store';

@Component({
  selector: 'app-detalle',
  imports: [FormsModule, DatePipe, RouterLink, Icono, ClasificacionPanel, SugerenciaPanel, TicketThread],
  template: `
    <header class="panel-cabecera">
      <a class="btn fantasma volver" [routerLink]="['/tickets']" [queryParams]="paramsVista()">
        <rs-icono nombre="arrow-left" />
        Bandeja
      </a>
      @if (ticket(); as actual) { <span class="codigo">{{ actual.codigo }}</span> }
      <span class="espacio"></span>
      @if (posicion() >= 0) {
        <span class="posicion" aria-label="Posición en la bandeja">{{ posicion() + 1 }} / {{ orden().length }}</span>
        <span class="navegar">
          @if (anterior(); as previo) {
            <a class="btn fantasma icono anterior" [routerLink]="['/tickets', previo.codigo]" [queryParams]="paramsVista()" aria-label="Ticket anterior (K)" title="Anterior (K)">
              <rs-icono nombre="chevron-up" />
            </a>
          }
          @if (siguiente(); as proximo) {
            <a class="btn fantasma icono siguiente" [routerLink]="['/tickets', proximo.codigo]" [queryParams]="paramsVista()" aria-label="Ticket siguiente (J)" title="Siguiente (J)">
              <rs-icono nombre="chevron-down" />
            </a>
          }
        </span>
      }
    </header>
    <div class="panel-cuerpo">
      @switch (carga()) {
        @case ('cargando') {
          <p class="estado-carga" role="status">Cargando ticket…</p>
        }
        @case ('error') {
          <p class="error" role="alert">No se pudo cargar el ticket. Puede que no exista o que no tengas acceso.</p>
        }
        @default {
          @if (ticket(); as actual) {
            <div class="detalle-ticket">
              <div class="conversacion">
                <h1 class="asunto">{{ actual.asunto }}</h1>
                <app-ticket-thread
                  [cuerpo]="actual.cuerpo"
                  [abiertoEn]="actual.abiertoEn"
                  [autorEtiqueta]="nombreDe(actual.autorId, 'Cliente')"
                  [comentarios]="actual.comentarios"
                  [autores]="nombres()"
                  [yoId]="yo()"
                  [etiquetaOtros]="puedeGestionar() ? 'Cliente' : 'Soporte'"
                  [puedeNota]="puedeGestionar()"
                  [puedeModerar]="puedeGestionar()"
                  [puedeRestaurar]="esAdmin()"
                  [error]="errorComentario()"
                  (enviado)="comentar($event)"
                  (ocultado)="ocultar($event)"
                  (restaurado)="restaurar($event)" />
              </div>
              <aside class="lateral" aria-label="Propiedades y asistente">
                <section class="propiedades">
                  <h2 class="lateral-titulo">Propiedades</h2>
                  <dl>
                    <dt>Estado</dt>
                    <dd>
                      @if (puedeGestionar()) {
                        <div class="transiciones segmentado" role="group" aria-label="Cambiar estado">
                          @for (estado of estados; track estado) {
                            <button type="button" [disabled]="estado === actual.estado" [attr.aria-pressed]="estado === actual.estado" (click)="transicionar(estado)">{{ etiquetaEstado(estado) }}</button>
                          }
                        </div>
                      } @else {
                        {{ etiquetaEstado(actual.estado) }}
                      }
                    </dd>
                    <dt>Prioridad</dt>
                    <dd>{{ etiquetaPrioridad(actual.prioridad) }}</dd>
                    <dt>Vence</dt>
                    <dd>
                      <time [attr.datetime]="actual.venceEl">{{ actual.venceEl | date: 'dd/MM/yyyy HH:mm' }}</time>
                      @if (slaCorre(actual.estado)) {
                        <span [class]="claseSla(actual)">
                          <rs-icono [nombre]="nivel(actual) === 'incumplido' ? 'alert' : 'clock'" [tam]="12" />
                          <span class="visually-hidden">{{ textoSla(nivel(actual)) }}: </span>{{ tiempo(actual) }}
                        </span>
                      }
                    </dd>
                    <dt>Abierto</dt>
                    <dd><time [attr.datetime]="actual.abiertoEn">{{ actual.abiertoEn | date: 'dd/MM/yyyy HH:mm' }}</time></dd>
                    @if (categoriaDe(actual); as categoria) {
                      <dt>Categoría</dt>
                      <dd>{{ categoria }}</dd>
                    }
                    @if (puedeGestionar()) {
                      <dt>Asignado</dt>
                      <dd>{{ nombreDe(actual.asignadoId, 'Sin asignar') }}</dd>
                    }
                  </dl>
                  @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
                  @if (puedeGestionar()) {
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
                </section>
                @if (puedeGestionar()) {
                  <section class="asistente">
                    <h2 class="lateral-titulo"><rs-icono nombre="sparkles" [tam]="14" /> Asistente</h2>
                    <app-clasificacion-panel [ticketId]="actual.id" [categorias]="categorias()" (aplicada)="refrescar()" />
                    <app-sugerencia-panel [ticketId]="actual.id" (aprobada)="refrescar()" />
                  </section>
                }
              </aside>
            </div>
          }
        }
      }
    </div>
  `,
})
export class DetallePage {
  private readonly tickets = inject(TicketsService);
  private readonly store = inject(TicketsStore);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly ticket = signal<TicketDetalle | null>(null);
  readonly agentes = signal<UsuarioResumen[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly error = signal('');
  readonly errorComentario = signal('');
  readonly carga = signal<'cargando' | 'listo' | 'error'>('cargando');
  protected readonly etiquetaEstado = etiquetaEstado;
  protected readonly etiquetaPrioridad = etiquetaPrioridad;
  protected readonly textoSla = textoSla;
  protected readonly slaCorre = slaCorre;
  readonly estados = ['ABIERTO', 'PENDIENTE', 'RESUELTO', 'CERRADO'];
  agente = '';
  private readonly hilo = viewChild(TicketThread);
  private enviandoComentario = false;
  private referencia = '';

  protected readonly yo = computed(() => this.auth.usuarioId());
  private readonly vista = toSignal(this.route.queryParamMap.pipe(map((params) => params.get('vista') ?? 'todos')), {
    initialValue: 'todos',
  });
  protected readonly paramsVista = computed(() => (this.vista() === 'todos' ? null : { vista: this.vista() }));
  protected readonly nombres = computed(() => Object.fromEntries(this.agentes().map((usuario) => [usuario.id, usuario.email])));

  /** Los tickets de la vista en el orden de la bandeja: de ahí salen anterior, siguiente y la posición. */
  protected readonly orden = computed(() => ordenDeBandeja(filtrarPorVista(this.store.tickets(), this.vista(), this.yo())));
  protected readonly posicion = computed(() => this.orden().findIndex((ticket) => ticket.id === this.ticket()?.id));
  protected readonly anterior = computed(() => (this.posicion() > 0 ? this.orden()[this.posicion() - 1] : null));
  protected readonly siguiente = computed(() => {
    const indice = this.posicion();
    return indice >= 0 && indice < this.orden().length - 1 ? this.orden()[indice + 1] : null;
  });

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const referencia = params.get('id') ?? '';
      // Tras normalizar un UUID al código la ruta cambia, pero el ticket ya está en pantalla.
      if (this.ticket()?.codigo === referencia) return;
      this.referencia = referencia;
      this.ticket.set(null);
      this.error.set('');
      this.errorComentario.set('');
      this.carga.set('cargando');
      void this.cargar(referencia);
    });
    if (this.puedeGestionar()) {
      void this.tickets.categorias().then((categorias) => this.categorias.set(categorias));
      void this.tickets.usuarios().then((usuarios) => {
        const agentes = usuarios.filter((usuario) => usuario.rol === 'AGENTE' || usuario.rol === 'ADMIN');
        this.agentes.set(agentes);
        this.agente = agentes[0]?.id ?? '';
      });
    }
  }

  @HostListener('document:keydown', ['$event'])
  protected alTeclear(evento: KeyboardEvent): void {
    if (evento.ctrlKey || evento.metaKey || evento.altKey) return;
    const origen = evento.target;
    if (origen instanceof HTMLElement && origen.closest('input, textarea, select, [contenteditable="true"]')) return;
    if (evento.key === 'Escape') {
      void this.router.navigateByUrl(this.router.createUrlTree(['/tickets'], { queryParams: this.paramsVista() }));
    } else if (evento.key === 'j' && this.siguiente()) {
      this.ir(this.siguiente()!.codigo);
    } else if (evento.key === 'k' && this.anterior()) {
      this.ir(this.anterior()!.codigo);
    }
  }

  puedeGestionar(): boolean {
    return this.auth.rol() !== 'CLIENTE';
  }

  esAdmin(): boolean {
    return this.auth.rol() === 'ADMIN';
  }

  nombreDe(id: string | null, porDefecto: string): string {
    if (id && id === this.yo()) return 'Tú';
    return (id && this.nombres()[id]) || porDefecto;
  }

  categoriaDe(ticket: TicketDetalle): string | undefined {
    return this.categorias().find((categoria) => categoria.id === ticket.categoriaId)?.nombre;
  }

  nivel(ticket: TicketDetalle) {
    return nivelSla(ticket);
  }

  tiempo(ticket: TicketDetalle): string {
    return tiempoSla(ticket.venceEl);
  }

  claseSla(ticket: TicketDetalle): string {
    return `sla sla-${nivelSla(ticket)}`;
  }

  async transicionar(estado: string): Promise<void> {
    const actual = this.ticket();
    if (!actual) return;
    this.error.set('');
    try {
      this.ticket.set(await this.tickets.transicionar(actual.id, estado));
      void this.store.cargar();
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
    void this.store.cargar();
  }

  /**
   * El borrador solo se vacía cuando el comentario se ha guardado: si la API lo rechaza (enlaces de más,
   * demasiado rápido, sesión caducada) el texto sigue en el cuadro. Como no se vacía al instante, un
   * segundo envío mientras el primero sigue en vuelo se ignora para no duplicar el comentario.
   */
  async comentar(evento: { cuerpo: string; interno: boolean }): Promise<void> {
    if (this.enviandoComentario) return;
    this.enviandoComentario = true;
    try {
      const guardado = await this.actuarSobreElHilo((id) => this.tickets.comentar(id, evento.cuerpo, evento.interno));
      if (guardado) this.hilo()?.limpiar();
    } finally {
      this.enviandoComentario = false;
    }
  }

  async ocultar(evento: { id: string; motivo: string }): Promise<void> {
    await this.actuarSobreElHilo((id) => this.tickets.ocultar(id, evento.id, evento.motivo));
  }

  async restaurar(comentarioId: string): Promise<void> {
    await this.actuarSobreElHilo((id) => this.tickets.restaurar(id, comentarioId));
  }

  /**
   * Hace algo en el hilo del ticket y lo recarga; si falla, lo dice en lenguaje claro bajo el hilo.
   * Devuelve si la acción llegó a guardarse, aunque la recarga posterior falle.
   */
  private async actuarSobreElHilo(accion: (ticketId: string) => Promise<unknown>): Promise<boolean> {
    const actual = this.ticket();
    if (!actual) return false;
    this.errorComentario.set('');
    let guardado = false;
    try {
      await accion(actual.id);
      guardado = true;
      this.ticket.set(await this.tickets.detalle(actual.id));
    } catch (fallo) {
      this.errorComentario.set(mensajeDeError(fallo));
    }
    return guardado;
  }

  async refrescar(): Promise<void> {
    const actual = this.ticket();
    if (!actual) return;
    this.ticket.set(await this.tickets.detalle(actual.id));
    void this.store.cargar();
  }

  private ir(codigo: string): void {
    void this.router.navigateByUrl(this.router.createUrlTree(['/tickets', codigo], { queryParams: this.paramsVista() }));
  }

  /** La URL lleva el código (`RS-3`); un UUID o un código en minúsculas se acepta y se normaliza. */
  private async cargar(referencia: string): Promise<void> {
    try {
      const cargado = await this.tickets.detalle(referencia);
      if (referencia !== this.referencia) return;
      this.ticket.set(cargado);
      this.carga.set('listo');
      if (cargado.codigo !== referencia) {
        void this.router.navigate(['/tickets', cargado.codigo], { replaceUrl: true, queryParamsHandling: 'preserve' });
      }
    } catch {
      if (referencia === this.referencia) this.carga.set('error');
    }
  }
}
