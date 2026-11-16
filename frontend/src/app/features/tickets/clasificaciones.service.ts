import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Clasificacion, Prioridad } from './clasificacion.model';
import { TicketDetalle } from './ticket.model';

@Injectable({ providedIn: 'root' })
export class ClasificacionesService {
  private readonly http = inject(HttpClient);

  sugerir(ticketId: string): Promise<Clasificacion> {
    return firstValueFrom(this.http.post<Clasificacion>(`/api/tickets/${ticketId}/clasificaciones`, {}));
  }

  /** La clasificacion pendiente del ticket, o null si no hay ninguna. */
  async actual(ticketId: string): Promise<Clasificacion | null> {
    try {
      return await firstValueFrom(
        this.http.get<Clasificacion>(`/api/tickets/${ticketId}/clasificaciones/actual`),
      );
    } catch (fallo) {
      if (fallo instanceof HttpErrorResponse && fallo.status === 404) return null;
      throw fallo;
    }
  }

  aplicar(id: string, categoriaId: string, prioridad: Prioridad): Promise<TicketDetalle> {
    return firstValueFrom(
      this.http.post<TicketDetalle>(`/api/clasificaciones/${id}/aplicacion`, { categoriaId, prioridad }),
    );
  }

  descartar(id: string): Promise<Clasificacion> {
    return firstValueFrom(this.http.post<Clasificacion>(`/api/clasificaciones/${id}/descarte`, {}));
  }
}
