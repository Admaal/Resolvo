export interface TicketResumen {
  id: string;
  codigo: string;
  asunto: string;
  estado: string;
  prioridad: string;
  asignadoId: string | null;
  autorId: string;
  venceEl: string;
  abiertoEn?: string;
}

export interface Comentario {
  id: string;
  autorId: string;
  cuerpo: string;
  interno: boolean;
  creadoEn: string;
}

export interface TicketDetalle extends TicketResumen {
  cuerpo: string;
  categoriaId: string;
  abiertoEn: string;
  comentarios: Comentario[];
}

export interface Categoria {
  id: string;
  nombre: string;
}

export interface UsuarioResumen {
  id: string;
  email: string;
  rol: string;
}

export interface PaginaTickets {
  elementos: TicketResumen[];
  pagina: number;
  tamano: number;
  total: number;
}
