import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButton } from '@angular/material/button';
import { Categoria } from './ticket.model';
import { TicketsService } from './tickets.service';

@Component({
  selector: 'app-alta',
  imports: [FormsModule, MatButton],
  template: `
    <form class="login" (ngSubmit)="crear()">
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
          <option>BAJA</option>
          <option>MEDIA</option>
          <option>ALTA</option>
          <option>URGENTE</option>
        </select>
      </label>
      <label>Asunto <input name="asunto" [(ngModel)]="asunto" maxlength="200" /></label>
      <label>Cuerpo <textarea name="cuerpo" [(ngModel)]="cuerpo"></textarea></label>
      @if (error()) { <p class="error">{{ error() }}</p> }
      <button matButton class="primario" type="submit">Abrir</button>
    </form>
  `,
})
export class AltaPage {
  private readonly tickets = inject(TicketsService);
  private readonly router = inject(Router);
  readonly categorias = signal<Categoria[]>([]);
  readonly error = signal('');
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
    this.error.set('');
    try {
      const ticket = await this.tickets.abrir({
        categoriaId: this.categoriaId,
        prioridad: this.prioridad,
        asunto: this.asunto,
        cuerpo: this.cuerpo,
      });
      await this.router.navigate(['/tickets', ticket.id]);
    } catch {
      this.error.set('invalido');
    }
  }
}
