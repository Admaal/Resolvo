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

/** Quién retiró un comentario y por qué. Para un cliente llega vacío: solo sabe que se retiró. */
export interface Oculto {
  motivo: string | null;
  porId: string | null;
  en: string | null;
}

export interface Comentario {
  id: string;
  autorId: string;
  /** Nulo solo para un cliente ante un comentario retirado. */
  cuerpo: string | null;
  interno: boolean;
  creadoEn: string;
  retirado?: boolean;
  oculto?: Oculto | null;
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
