import { DatePipe } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icono } from '../../shared/icono';
import { Comentario } from './ticket.model';

@Component({
  selector: 'app-ticket-thread',
  imports: [FormsModule, DatePipe, Icono],
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
        @if (comentario.retirado) {
          <li class="comentario retirado">
            <p class="retirado-linea">
              <rs-icono nombre="eye-off" [tam]="14" />
              @if (puedeModerar()) {
                <span>Oculto por {{ quienOculto(comentario) }} · {{ textoMotivo(comentario) }}</span>
                <button type="button" class="btn fantasma ver-texto" [attr.aria-expanded]="textoAbierto(comentario.id)" (click)="alternarTexto(comentario.id)">
                  {{ textoAbierto(comentario.id) ? 'Ocultar texto' : 'Ver texto' }}
                </button>
                @if (puedeRestaurar()) {
                  <button type="button" class="btn fantasma restaurar" (click)="restaurado.emit(comentario.id)">
                    <rs-icono nombre="rotate-ccw" [tam]="14" /> Restaurar
                  </button>
                }
              } @else {
                <span>Este comentario se ha retirado.</span>
              }
            </p>
            @if (puedeModerar() && textoAbierto(comentario.id)) {
              <p class="cuerpo">{{ comentario.cuerpo }}</p>
            }
          </li>
        } @else {
          <li class="mensaje comentario" [class.interno]="comentario.interno">
            <span class="avatar" aria-hidden="true">{{ inicial(autor(comentario)) }}</span>
            <div class="mensaje-contenido">
              <p class="mensaje-cabecera">
                <span class="autor">{{ autor(comentario) }}</span>
                @if (comentario.interno) { <span class="nota-interna"><rs-icono nombre="lock" [tam]="12" /> Nota interna</span> }
                <time class="ayuda" [attr.datetime]="comentario.creadoEn">{{ comentario.creadoEn | date: 'dd/MM/yyyy HH:mm' }}</time>
                @if (puedeModerar()) {
                  <button type="button" class="btn fantasma ocultar" (click)="moderar(comentario.id)">
                    <rs-icono nombre="eye-off" [tam]="14" /> Ocultar
                  </button>
                }
              </p>
              <p class="cuerpo">{{ comentario.cuerpo }}</p>
              @if (moderando() === comentario.id) {
                <div class="moderar">
                  <label>Motivo
                    <select name="motivo" (change)="motivo.set($any($event.target).value)">
                      @for (opcion of motivos; track opcion.valor) {
                        <option [value]="opcion.valor" [selected]="opcion.valor === motivo()">{{ opcion.texto }}</option>
                      }
                    </select>
                  </label>
                  <button type="button" class="btn secundario confirmar-ocultar" (click)="confirmarOcultar(comentario.id)">Ocultar comentario</button>
                  <button type="button" class="btn fantasma cancelar-ocultar" (click)="moderando.set(null)">Cancelar</button>
                </div>
              }
            </div>
          </li>
        }
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
  /** Agente y admin pueden ocultar; solo el admin restaura. El texto de lo retirado solo llega al equipo. */
  readonly puedeModerar = input(false);
  readonly puedeRestaurar = input(false);
  readonly ocultado = output<{ id: string; motivo: string }>();
  readonly restaurado = output<string>();
  protected readonly motivos = [
    { valor: 'SPAM', texto: 'Spam' },
    { valor: 'OFENSIVO', texto: 'Ofensivo' },
    { valor: 'OTRO', texto: 'Otro' },
  ];
  readonly moderando = signal<string | null>(null);
  readonly motivo = signal('SPAM');
  private readonly textosAbiertos = signal<string[]>([]);
  readonly modo = signal<'responder' | 'nota'>('responder');
  borrador = '';
  private ultimoEnviado: string | null = null;

  private readonly nombres = computed(() => this.autores());

  autor(comentario: Comentario): string {
    if (comentario.autorId === this.yoId()) return 'Tú';
    return this.nombres()[comentario.autorId] ?? this.etiquetaOtros();
  }

  inicial(nombre: string): string {
    return nombre.trim().charAt(0).toUpperCase() || '?';
  }

  moderar(id: string): void {
    this.motivo.set('SPAM');
    this.moderando.set(id);
  }

  confirmarOcultar(id: string): void {
    this.ocultado.emit({ id, motivo: this.motivo() });
    this.moderando.set(null);
  }

  textoAbierto(id: string): boolean {
    return this.textosAbiertos().includes(id);
  }

  alternarTexto(id: string): void {
    this.textosAbiertos.update((abiertos) => (abiertos.includes(id) ? abiertos.filter((otro) => otro !== id) : [...abiertos, id]));
  }

  quienOculto(comentario: Comentario): string {
    const id = comentario.oculto?.porId;
    if (!id) return 'el equipo';
    return id === this.yoId() ? 'ti' : (this.nombres()[id] ?? 'el equipo');
  }

  textoMotivo(comentario: Comentario): string {
    const motivo = comentario.oculto?.motivo;
    return this.motivos.find((opcion) => opcion.valor === motivo)?.texto ?? 'Sin motivo';
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

  /**
   * Emite el borrador, pero no lo vacía: quien lo guarda llama a {@link limpiar} cuando la API lo ha
   * aceptado. Si falla, el texto sigue en el cuadro para poder corregirlo y reenviarlo.
   */
  enviar(interno: boolean = this.modo() === 'nota'): void {
    if (this.vacio()) return;
    this.ultimoEnviado = this.borrador.trim();
    this.enviado.emit({ cuerpo: this.ultimoEnviado, interno });
  }

  /** Vacía el borrador enviado; si la persona ya ha escrito otra cosa mientras tanto, la respeta. */
  limpiar(): void {
    if (this.borrador.trim() === this.ultimoEnviado) this.borrador = '';
  }
}
