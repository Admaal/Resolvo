import { Component, input } from '@angular/core';

/** Icono circular del estado del ticket (design §4.2). Va siempre junto a un texto o con título. */
@Component({
  selector: 'rs-estado',
  template: `
    <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true" focusable="false" [class]="estado().toLowerCase()">
      @switch (estado()) {
        @case ('PENDIENTE') {
          <circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" stroke-width="1.5" />
          <path d="M8 4a4 4 0 0 1 0 8z" fill="currentColor" />
        }
        @case ('RESUELTO') {
          <circle cx="8" cy="8" r="7" fill="currentColor" />
          <path d="m5 8.2 2 2 4-4.2" fill="none" stroke="var(--bg-1)" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        }
        @case ('CERRADO') {
          <circle cx="8" cy="8" r="7" fill="currentColor" />
        }
        @default {
          <circle cx="8" cy="8" r="6" fill="none" stroke="currentColor" stroke-width="1.5" />
        }
      }
    </svg>
  `,
  styles: `
    :host { display: inline-flex; flex: none; }
    .abierto { color: var(--status-open); }
    .pendiente { color: var(--status-pending); }
    .resuelto { color: var(--status-resolved); }
    .cerrado { color: var(--status-closed); }
  `,
})
export class EstadoIcono {
  readonly estado = input.required<string>();
}
