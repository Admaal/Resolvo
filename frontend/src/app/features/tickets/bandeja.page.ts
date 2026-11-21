import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { EstadoIcono } from '../../shared/estado-icono';
import { Icono, NombreIcono } from '../../shared/icono';
import { ClaveGrupo, agruparPorUrgencia } from './agrupacion';
import { etiquetaEstado, etiquetaPrioridad, nivelSla, slaCorre, textoSla, tiempoSla } from './etiquetas';
import { TicketResumen } from './ticket.model';
import { TicketsStore, filtrarPorVista } from './tickets.store';

const TITULOS: Record<string, string> = {
  mios: 'Mis tickets',
  sin: 'Sin asignar',
  todos: 'Todos los tickets',
  riesgo: 'SLA en riesgo',
  resueltos: 'Resueltos',
};

const ICONOS_GRUPO: Record<ClaveGrupo, NombreIcono> = {
  incumplido: 'alert',
  riesgo: 'clock',
  urgente: 'zap',
  resto: 'inbox',
};

@Component({
  selector: 'app-bandeja',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, Icono, EstadoIcono],
  template: `
    <div class="bandeja" [class.con-detalle]="detalleAbierto()">
      <section class="panel lista" aria-label="Lista de tickets">
        <header class="panel-cabecera">
          <h1>{{ titulo() }}</h1>
          <span class="contador-vista" [attr.aria-label]="visibles().length + ' tickets'">{{ visibles().length }}</span>
        </header>
        <div class="busqueda">
          <rs-icono nombre="search" />
          <input
            type="search"
            name="busqueda"
            aria-label="Buscar por asunto o código"
            placeholder="Buscar por asunto o código"
            [value]="busqueda()"
            (input)="busqueda.set($any($event.target).value)" />
        </div>
        <div class="panel-cuerpo">
          @switch (store.carga()) {
            @case ('cargando') {
              <div class="estado-carga" role="status">
                <span class="visually-hidden">Cargando tickets…</span>
                @for (esqueleto of esqueletos; track esqueleto) {
                  <div class="esqueleto" aria-hidden="true"></div>
                }
              </div>
            }
            @case ('error') {
              <p class="error" role="alert">No se pudieron cargar los tickets. Recarga la página e inténtalo de nuevo.</p>
            }
            @default {
              @for (grupo of grupos(); track grupo.clave) {
                <section class="grupo" [class]="'grupo ' + grupo.clave">
                  <h2 class="grupo-titulo">
                    <rs-icono [nombre]="iconoGrupo(grupo.clave)" [tam]="14" />
                    <span>{{ grupo.titulo }}</span>{{ ' ' }}<span class="n">{{ grupo.tickets.length }}</span>
                  </h2>
                  <ul class="lista-tickets">
                    @for (ticket of grupo.tickets; track ticket.id) {
                      <li>
                        <a
                          class="fila"
                          [routerLink]="['/tickets', ticket.id]"
                          queryParamsHandling="preserve"
                          routerLinkActive="activa"
                          ariaCurrentWhenActive="page">
                          <span class="fila-texto">
                            <span class="fila-cabecera">
                              <span class="codigo">{{ ticket.codigo }}</span>
                              @if (slaCorre(ticket.estado)) {
                                <span [class]="claseSla(ticket)">
                                  <rs-icono [nombre]="iconoSla(ticket)" [tam]="12" />
                                  <span class="visually-hidden">{{ textoSla(nivel(ticket)) }}: </span>{{ tiempo(ticket) }}
                                </span>
                              }
                            </span>
                            <span class="fila-asunto">{{ ticket.asunto }}</span>
                            <span class="fila-meta">
                              <span class="fila-estado">
                                <rs-estado [estado]="ticket.estado" />
                                {{ etiquetaEstado(ticket.estado) }}
                              </span>
                              @if (destacaPrioridad(ticket)) {
                                <span [class]="clasePrioridad(ticket)">
                                  <rs-icono nombre="zap" [tam]="12" />
                                  {{ etiquetaPrioridad(ticket.prioridad) }}
                                </span>
                              }
                            </span>
                          </span>
                        </a>
                      </li>
                    }
                  </ul>
                </section>
              } @empty {
                <div class="estado-vacio">
                  <rs-icono nombre="inbox" [tam]="32" />
                  <p>{{ busqueda().trim() ? 'Ningún ticket coincide con la búsqueda.' : 'No hay tickets en esta vista.' }}</p>
                </div>
              }
            }
          }
        </div>
      </section>
      <section class="panel detalle" aria-label="Detalle">
        <router-outlet (activate)="alActivar()" (deactivate)="detalleAbierto.set(false)" />
      </section>
    </div>
  `,
})
export class BandejaPage {
  protected readonly store = inject(TicketsStore);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vista = toSignal(this.route.queryParamMap.pipe(map((params) => params.get('vista') ?? 'todos')), {
    initialValue: 'todos',
  });
  protected readonly busqueda = signal('');
  protected readonly detalleAbierto = signal(false);
  protected readonly esqueletos = [1, 2, 3, 4, 5, 6];

  protected readonly etiquetaEstado = etiquetaEstado;
  protected readonly etiquetaPrioridad = etiquetaPrioridad;
  protected readonly textoSla = textoSla;
  protected readonly slaCorre = slaCorre;

  readonly titulo = computed(() => TITULOS[this.vista()] ?? TITULOS['todos']);

  readonly visibles = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    return filtrarPorVista(this.store.tickets(), this.vista(), this.auth.usuarioId()).filter(
      (ticket) => !texto || ticket.asunto.toLowerCase().includes(texto) || ticket.codigo.toLowerCase().includes(texto),
    );
  });

  protected readonly grupos = computed(() => agruparPorUrgencia(this.visibles()));

  constructor() {
    void this.store.cargar();
  }

  /** Con un ticket (o el alta) abierto, en pantallas estrechas la lista cede el sitio al detalle. */
  protected alActivar(): void {
    this.detalleAbierto.set(this.router.url.split('?')[0] !== '/tickets');
  }

  protected iconoGrupo(clave: ClaveGrupo): NombreIcono {
    return ICONOS_GRUPO[clave];
  }

  nivel(ticket: TicketResumen) {
    return nivelSla(ticket);
  }

  tiempo(ticket: TicketResumen): string {
    return tiempoSla(ticket.venceEl);
  }

  clasePrioridad(ticket: TicketResumen): string {
    return `fila-prioridad prioridad-${ticket.prioridad.toLowerCase()}`;
  }

  claseSla(ticket: TicketResumen): string {
    return `sla sla-${nivelSla(ticket)}`;
  }

  iconoSla(ticket: TicketResumen): NombreIcono {
    return nivelSla(ticket) === 'incumplido' ? 'alert' : 'clock';
  }

  /** El color y el icono de prioridad solo aparecen cuando importa: Baja y Media no se dicen. */
  destacaPrioridad(ticket: TicketResumen): boolean {
    return ticket.prioridad === 'ALTA' || ticket.prioridad === 'URGENTE';
  }
}
