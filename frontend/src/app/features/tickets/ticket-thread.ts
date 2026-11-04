import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { Comentario } from './ticket.model';

@Component({
  selector: 'app-ticket-thread',
  imports: [FormsModule, MatButton],
  template: `
    <ol>
      @for (comentario of comentarios(); track comentario.id) {
        <li class="comentario">
          @if (comentario.interno) { <span class="chip">Nota interna</span> }
          <p class="cuerpo">{{ comentario.cuerpo }}</p>
        </li>
      }
    </ol>
    <label>Respuesta <textarea name="borrador" [(ngModel)]="borrador"></textarea></label>
    <button matButton class="primario" type="button" (click)="enviar(false)">Responder</button>
    @if (puedeNota()) {
      <button matButton class="secundario" type="button" (click)="enviar(true)">Nota</button>
    }
    @if (error()) { <p class="error">{{ error() }}</p> }
  `,
})
export class TicketThread {
  readonly comentarios = input<Comentario[]>([]);
  readonly puedeNota = input(false);
  readonly error = input('');
  readonly enviado = output<{ cuerpo: string; interno: boolean }>();
  borrador = '';

  enviar(interno: boolean): void {
    this.enviado.emit({ cuerpo: this.borrador, interno });
    this.borrador = '';
  }
}
