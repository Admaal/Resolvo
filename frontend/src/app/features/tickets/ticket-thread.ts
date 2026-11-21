import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Comentario } from './ticket.model';

@Component({
  selector: 'app-ticket-thread',
  imports: [FormsModule, DatePipe],
  template: `
    <ol class="hilo" aria-label="Conversación">
      @if (cuerpo()) {
        <li class="mensaje mensaje-inicial">
          <span class="avatar" aria-hidden="true">{{ inicial(autorEtiqueta()) }}</span>
          <div class="mensaje-contenido">
            <p class="mensaje-cabecera">
              <span class="autor">{{ autorEtiqueta() }}</span>
              @if (abiertoEn()) {
                <time class="ayuda" [attr.datetime]="abiertoEn()">{{ abiertoEn() | date: 'dd/MM/yyyy HH:mm' }}</time>
              }
            </p>
            <p class="cuerpo">{{ cuerpo() }}</p>
          </div>
        </li>
      }
      @for (comentario of comentarios(); track comentario.id) {
        <li class="mensaje comentario" [class.interno]="comentario.interno">
          <span class="avatar" aria-hidden="true">{{ inicial(autor(comentario)) }}</span>
          <div class="mensaje-contenido">
            <p class="mensaje-cabecera">
              <span class="autor">{{ autor(comentario) }}</span>
              @if (comentario.interno) { <span class="chip nota-interna">Nota interna</span> }
              <time class="ayuda" [attr.datetime]="comentario.creadoEn">{{ comentario.creadoEn | date: 'dd/MM/yyyy HH:mm' }}</time>
            </p>
            <p class="cuerpo">{{ comentario.cuerpo }}</p>
          </div>
        </li>
      }
    </ol>
    <div class="compositor" [class.nota]="modo() === 'nota'">
      @if (puedeNota()) {
        <div class="pestanas" role="tablist" aria-label="Tipo de mensaje">
          <button type="button" role="tab" [attr.aria-selected]="modo() === 'responder'" (click)="modo.set('responder')">Responder</button>
          <button type="button" role="tab" [attr.aria-selected]="modo() === 'nota'" (click)="modo.set('nota')">Nota interna</button>
        </div>
      }
      <label class="visually-hidden" for="borrador-hilo">{{ modo() === 'nota' ? 'Nota interna' : 'Respuesta' }}</label>
      <textarea
        id="borrador-hilo"
        name="borrador"
        rows="3"
        [placeholder]="modo() === 'nota' ? 'Nota visible solo para el equipo…' : 'Escribe una respuesta…'"
        [(ngModel)]="borrador"
        (keydown)="alTeclear($event)"></textarea>
      <div class="acciones">
        <button class="btn primario" type="button" [disabled]="vacio()" (click)="enviar()">
          {{ modo() === 'nota' ? 'Guardar nota' : 'Enviar' }}
          <kbd>Ctrl ↵</kbd>
        </button>
      </div>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    </div>
  `,
})
export class TicketThread {
  readonly comentarios = input<Comentario[]>([]);
  readonly puedeNota = input(false);
  readonly error = input('');
  /** Texto con el que se abrió el ticket: se enseña como primer mensaje. */
  readonly cuerpo = input('');
  readonly autorEtiqueta = input('Cliente');
  readonly abiertoEn = input('');
  /** Nombre por id de usuario, para las personas del equipo. */
  readonly autores = input<Record<string, string>>({});
  readonly yoId = input<string | null>(null);
  /** Cómo se llama a quien no es uno mismo ni del equipo conocido. */
  readonly etiquetaOtros = input('Cliente');
  readonly enviado = output<{ cuerpo: string; interno: boolean }>();
  readonly modo = signal<'responder' | 'nota'>('responder');
  borrador = '';

  private readonly nombres = computed(() => this.autores());

  autor(comentario: Comentario): string {
    if (comentario.autorId === this.yoId()) return 'Tú';
    return this.nombres()[comentario.autorId] ?? this.etiquetaOtros();
  }

  inicial(nombre: string): string {
    return nombre.trim().charAt(0).toUpperCase() || '?';
  }

  vacio(): boolean {
    return this.borrador.trim() === '';
  }

  alTeclear(evento: KeyboardEvent): void {
    if (evento.key === 'Enter' && (evento.ctrlKey || evento.metaKey)) {
      evento.preventDefault();
      this.enviar();
    }
  }

  enviar(interno: boolean = this.modo() === 'nota'): void {
    if (this.vacio()) return;
    this.enviado.emit({ cuerpo: this.borrador.trim(), interno });
    this.borrador = '';
  }
}
