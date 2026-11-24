import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Categoria, PaginaTickets, TicketDetalle, UsuarioResumen } from './ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketsService {
  private readonly http = inject(HttpClient);

  listar(filtro: { estado?: string; prioridad?: string; asignado?: string }): Promise<PaginaTickets> {
    let params = new HttpParams().set('tamano', '50').set('pagina', '0');
    if (filtro.estado) params = params.set('estado', filtro.estado);
    if (filtro.prioridad) params = params.set('prioridad', filtro.prioridad);
    if (filtro.asignado) params = params.set('asignado', filtro.asignado);
    return firstValueFrom(this.http.get<PaginaTickets>('/api/tickets', { params }));
  }

  detalle(referencia: string): Promise<TicketDetalle> {
    return firstValueFrom(this.http.get<TicketDetalle>(`/api/tickets/${encodeURIComponent(referencia)}`));
  }

  abrir(cuerpo: { categoriaId: string; prioridad: string; asunto: string; cuerpo: string }): Promise<TicketDetalle> {
    return firstValueFrom(this.http.post<TicketDetalle>('/api/tickets', cuerpo));
  }

  transicionar(id: string, estado: string): Promise<TicketDetalle> {
    return firstValueFrom(this.http.post<TicketDetalle>(`/api/tickets/${id}/transicion`, { estado }));
  }

  asignar(id: string, asignadoId: string): Promise<TicketDetalle> {
    return firstValueFrom(this.http.post<TicketDetalle>(`/api/tickets/${id}/asignacion`, { asignadoId }));
  }

  comentar(id: string, cuerpo: string, interno: boolean): Promise<unknown> {
    return firstValueFrom(this.http.post(`/api/tickets/${id}/comentarios`, { cuerpo, interno }));
  }

  ocultar(id: string, comentarioId: string, motivo: string): Promise<unknown> {
    return firstValueFrom(this.http.post(`/api/tickets/${id}/comentarios/${comentarioId}/ocultacion`, { motivo }));
  }

  restaurar(id: string, comentarioId: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/tickets/${id}/comentarios/${comentarioId}/ocultacion`));
  }

  categorias(): Promise<Categoria[]> {
    return firstValueFrom(this.http.get<Categoria[]>('/api/categorias'));
  }

  usuarios(): Promise<UsuarioResumen[]> {
    return firstValueFrom(this.http.get<UsuarioResumen[]>('/api/usuarios'));
  }
}
