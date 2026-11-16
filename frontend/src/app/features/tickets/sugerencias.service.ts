import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Articulo, Sugerencia } from './sugerencia.model';

@Injectable({ providedIn: 'root' })
export class SugerenciasService {
  private readonly http = inject(HttpClient);

  sugerir(ticketId: string): Promise<Sugerencia> {
    return firstValueFrom(this.http.post<Sugerencia>(`/api/tickets/${ticketId}/sugerencias`, {}));
  }

  /** La sugerencia pendiente del ticket, o null si no hay ninguna. */
  async actual(ticketId: string): Promise<Sugerencia | null> {
    try {
      return await firstValueFrom(this.http.get<Sugerencia>(`/api/tickets/${ticketId}/sugerencias/actual`));
    } catch (fallo) {
      if (fallo instanceof HttpErrorResponse && fallo.status === 404) return null;
      throw fallo;
    }
  }

  aprobar(id: string, texto: string): Promise<unknown> {
    return firstValueFrom(this.http.post(`/api/sugerencias/${id}/aprobar`, { texto }));
  }

  descartar(id: string): Promise<Sugerencia> {
    return firstValueFrom(this.http.post<Sugerencia>(`/api/sugerencias/${id}/descartar`, {}));
  }

  articulo(id: string): Promise<Articulo> {
    return firstValueFrom(this.http.get<Articulo>(`/api/articulos/${id}`));
  }
}
