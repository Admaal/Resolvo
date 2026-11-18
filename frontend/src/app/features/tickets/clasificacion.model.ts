export type Prioridad = 'BAJA' | 'MEDIA' | 'ALTA' | 'URGENTE';

export const PRIORIDADES: readonly Prioridad[] = ['BAJA', 'MEDIA', 'ALTA', 'URGENTE'];

export interface Clasificacion {
  id: string;
  ticketId: string;
  categoriaId: string;
  prioridad: Prioridad;
  motivo: string;
  estado: 'PENDIENTE' | 'APLICADA' | 'DESCARTADA';
  modelo: string;
  creadaEn: string;
}
