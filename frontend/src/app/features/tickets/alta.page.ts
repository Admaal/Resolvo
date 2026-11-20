import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { mensajeDeError } from '../../core/errores';
import { Categoria } from './ticket.model';
import { etiquetaPrioridad } from './etiquetas';
import { TicketsService } from './tickets.service';

@Component({
  selector: 'app-alta',
  imports: [FormsModule],
  template: `
    <form class="formulario" (ngSubmit)="crear()">
      <h1>Nuevo ticket</h1>
      <label>Categoría
        <select name="categoria" [(ngModel)]="categoriaId">
          @for (categoria of categorias(); track categoria.id) {
            <option [value]="categoria.id">{{ categoria.nombre }}</option>
          }
        </select>
      </label>
      <label>Prioridad
        <select name="prioridad" [(ngModel)]="prioridad">
          @for (opcion of prioridades; track opcion) {
            <option [value]="opcion">{{ etiquetaPrioridad(opcion) }}</option>
          }
        </select>
      </label>
      <label>Asunto <input name="asunto" [(ngModel)]="asunto" maxlength="200" required /></label>
      <label>Cuerpo <textarea name="cuerpo" [(ngModel)]="cuerpo" required></textarea></label>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      <button class="btn primario" type="submit" [disabled]="enviando()">{{ enviando() ? 'Abriendo…' : 'Abrir' }}</button>
    </form>
  `,
})
export class AltaPage {
  private readonly tickets = inject(TicketsService);
  private readonly router = inject(Router);
  readonly categorias = signal<Categoria[]>([]);
  readonly error = signal('');
  readonly enviando = signal(false);
  readonly prioridades = ['BAJA', 'MEDIA', 'ALTA', 'URGENTE'];
  protected readonly etiquetaPrioridad = etiquetaPrioridad;
  categoriaId = '';
  prioridad = 'MEDIA';
  asunto = '';
  cuerpo = '';

  constructor() {
    void this.tickets.categorias().then((lista) => {
      this.categorias.set(lista);
      this.categoriaId = lista[0]?.id ?? '';
    });
  }

  async crear(): Promise<void> {
    if (this.enviando()) return;
    this.error.set('');
    this.enviando.set(true);
    try {
      const ticket = await this.tickets.abrir({
        categoriaId: this.categoriaId,
        prioridad: this.prioridad,
        asunto: this.asunto,
        cuerpo: this.cuerpo,
      });
      await this.router.navigate(['/tickets', ticket.id]);
    } catch (fallo) {
      this.error.set(mensajeDeError(fallo));
    } finally {
      this.enviando.set(false);
    }
  }
}
