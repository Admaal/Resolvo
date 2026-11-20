import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Comentario } from './ticket.model';

@Component({
  selector: 'app-ticket-thread',
  imports: [FormsModule, DatePipe],
  template: `
    <ol class="hilo" aria-label="Conversación">
      @for (comentario of comentarios(); track comentario.id) {
        <li class="comentario" [class.interno]="comentario.interno">
          <p class="ayuda">
            @if (comentario.interno) { <span class="chip nota-interna">Nota interna</span> }
            <time [attr.datetime]="comentario.creadoEn">{{ comentario.creadoEn | date: 'dd/MM/yyyy HH:mm' }}</time>
          </p>
          <p class="cuerpo">{{ comentario.cuerpo }}</p>
        </li>
      }
    </ol>
    <label>Respuesta <textarea name="borrador" [(ngModel)]="borrador"></textarea></label>
    <div class="acciones">
      <button class="btn primario" type="button" [disabled]="vacio()" (click)="enviar(false)">Responder</button>
      @if (puedeNota()) {
        <button class="btn secundario" type="button" [disabled]="vacio()" (click)="enviar(true)">Nota</button>
      }
    </div>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
  `,
})
export class TicketThread {
  readonly comentarios = input<Comentario[]>([]);
  readonly puedeNota = input(false);
  readonly error = input('');
  readonly enviado = output<{ cuerpo: string; interno: boolean }>();
  borrador = '';

  vacio(): boolean {
    return this.borrador.trim() === '';
  }

  enviar(interno: boolean): void {
    if (this.vacio()) return;
    this.enviado.emit({ cuerpo: this.borrador.trim(), interno });
    this.borrador = '';
  }
}
