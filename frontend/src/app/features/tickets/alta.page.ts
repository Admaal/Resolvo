import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { mensajeDeError } from '../../core/errores';
import { Icono } from '../../shared/icono';
import { Categoria } from './ticket.model';
import { etiquetaPrioridad } from './etiquetas';
import { TicketsService } from './tickets.service';
import { TicketsStore } from './tickets.store';

@Component({
  selector: 'app-alta',
  imports: [FormsModule, RouterLink, Icono],
  template: `
    <header class="panel-cabecera">
      <a class="btn fantasma volver" routerLink="/tickets">
        <rs-icono nombre="arrow-left" />
        Cancelar
      </a>
      <span class="espacio"></span>
    </header>
    <div class="panel-cuerpo">
      <form class="formulario ancho" (ngSubmit)="crear()">
        <h1>Nuevo ticket</h1>
        <p class="ayuda">Cuéntanos qué pasa. Cuanto más detalle, más rápido podremos resolverlo.</p>
        <div class="fila-campos">
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
        </div>
        <label>Asunto <input name="asunto" [(ngModel)]="asunto" maxlength="200" required /></label>
        <label>Cuerpo <textarea name="cuerpo" [(ngModel)]="cuerpo" rows="8" required></textarea></label>
        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
        <div class="acciones">
          <button class="btn primario" type="submit" [disabled]="enviando()">{{ enviando() ? 'Abriendo…' : 'Abrir ticket' }}</button>
        </div>
      </form>
    </div>
  `,
})
export class AltaPage {
  private readonly tickets = inject(TicketsService);
  private readonly store = inject(TicketsStore);
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
      void this.store.cargar();
      await this.router.navigate(['/tickets', ticket.codigo]);
    } catch (fallo) {
      this.error.set(mensajeDeError(fallo));
    } finally {
      this.enviando.set(false);
    }
  }
}
