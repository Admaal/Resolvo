export interface CitaSugerencia {
  orden: number;
  fuente: 'ARTICULO' | 'TICKET';
  fuenteId: string;
  titulo: string;
}

export interface Sugerencia {
  id: string;
  ticketId: string;
  borrador: string;
  estado: 'PENDIENTE' | 'APROBADA' | 'DESCARTADA';
  modelo: string;
  creadaEn: string;
  citas: CitaSugerencia[];
}

export interface Articulo {
  id: string;
  titulo: string;
  cuerpo: string;
}
