import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { Icono, NombreIcono } from '../shared/icono';
import { TicketsStore, VISTAS, Vista } from '../features/tickets/tickets.store';
import { AuthService } from './auth.service';
import { alternarTema } from './tema';

interface Item {
  vista: Vista;
  nombre: string;
  icono: NombreIcono;
  soloEquipo?: boolean;
  alerta?: boolean;
}

const ITEMS: Item[] = [
  { vista: 'mios', nombre: 'Mis tickets', icono: 'user' },
  { vista: 'sin', nombre: 'Sin asignar', icono: 'user-x', soloEquipo: true },
  { vista: 'todos', nombre: 'Todos', icono: 'layers' },
  { vista: 'riesgo', nombre: 'SLA en riesgo', icono: 'alert', soloEquipo: true, alerta: true },
  { vista: 'resueltos', nombre: 'Resueltos', icono: 'check-circle' },
];

function capitalizar(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1).toLowerCase();
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, Icono],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="sidebar-cabecera">
          <a class="marca" routerLink="/tickets" aria-label="Resolvo, ir a la bandeja">
            <span class="marca-logo" aria-hidden="true"><rs-icono nombre="check-circle" [tam]="14" /></span>
            Resolvo
          </a>
        </div>
        <a class="btn primario nuevo" routerLink="/tickets/nuevo" [attr.aria-current]="activa() === 'nuevo' ? 'page' : null">
          <rs-icono nombre="plus" />
          <span class="nombre">Nuevo ticket</span>
        </a>
        <nav class="nav" aria-label="Vistas">
          @for (item of items(); track item.vista) {
            <a
              [routerLink]="['/tickets']"
              [queryParams]="item.vista === 'todos' ? null : { vista: item.vista }"
              [attr.aria-current]="activa() === item.vista ? 'page' : null">
              <rs-icono [nombre]="item.icono" />
              <span class="nombre">{{ item.nombre }}</span>
              @if (contadores()[item.vista] > 0) {
                <span class="contador" [class.alerta]="item.alerta">{{ contadores()[item.vista] }}</span>
              }
            </a>
          }
        </nav>
        <div class="sidebar-pie">
          <p class="rol"><rs-icono nombre="user-check" />{{ rol() }}</p>
          <button class="btn fantasma tema" type="button" (click)="tema()">
            <rs-icono [nombre]="oscuro() ? 'sun' : 'moon'" />
            <span class="nombre">Tema {{ oscuro() ? 'claro' : 'oscuro' }}</span>
          </button>
          <button class="btn fantasma" type="button" (click)="salir()">
            <rs-icono nombre="log-out" />
            <span class="nombre">Salir</span>
          </button>
        </div>
      </aside>
      <main class="contenido">
        <router-outlet />
      </main>
    </div>
  `,
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly store = inject(TicketsStore);
  readonly oscuro = signal(document.documentElement.dataset['theme'] !== 'light');
  protected readonly contadores = this.store.contadores;
  protected readonly rol = computed(() => capitalizar(this.auth.rol() ?? ''));
  protected readonly items = computed(() => ITEMS.filter((item) => !item.soloEquipo || this.auth.rol() !== 'CLIENTE'));

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );

  /** La vista (o «nuevo») que corresponde a la URL actual, también al ver un ticket. */
  protected readonly activa = computed<Vista | 'nuevo' | null>(() => {
    const arbol = this.router.parseUrl(this.url());
    const segmentos = arbol.root.children['primary']?.segments.map((segmento) => segmento.path) ?? [];
    if (segmentos[0] !== 'tickets') return null;
    if (segmentos[1] === 'nuevo') return 'nuevo';
    const vista = arbol.queryParams['vista'];
    return VISTAS.includes(vista) ? vista : 'todos';
  });

  tema(): void {
    this.oscuro.set(alternarTema() === 'dark');
  }

  async salir(): Promise<void> {
    await this.auth.salir();
    await this.router.navigateByUrl('/');
  }
}
