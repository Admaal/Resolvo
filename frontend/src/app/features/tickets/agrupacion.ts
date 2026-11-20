import { nivelSla } from './etiquetas';
import { TicketResumen } from './ticket.model';

export type ClaveGrupo = 'incumplido' | 'riesgo' | 'urgente' | 'resto';

export interface Grupo {
  clave: ClaveGrupo;
  titulo: string;
  tickets: TicketResumen[];
}

const TITULOS: Record<ClaveGrupo, string> = {
  incumplido: 'SLA incumplido',
  riesgo: 'En riesgo',
  urgente: 'Urgente',
  resto: 'Resto',
};

const ORDEN: ClaveGrupo[] = ['incumplido', 'riesgo', 'urgente', 'resto'];

function claveDe(ticket: TicketResumen, ahora: number): ClaveGrupo {
  if (ticket.estado !== 'ABIERTO' && ticket.estado !== 'PENDIENTE') return 'resto';
  const nivel = nivelSla(ticket, ahora);
  if (nivel === 'incumplido') return 'incumplido';
  if (nivel === 'riesgo') return 'riesgo';
  return ticket.prioridad === 'URGENTE' ? 'urgente' : 'resto';
}

/** Bandeja agrupada por urgencia (design §4.4): primero lo que ya incumplió el SLA. */
export function agruparPorUrgencia(tickets: TicketResumen[], ahora: number = Date.now()): Grupo[] {
  return ORDEN.map((clave) => ({
    clave,
    titulo: TITULOS[clave],
    tickets: tickets.filter((ticket) => claveDe(ticket, ahora) === clave),
  })).filter((grupo) => grupo.tickets.length > 0);
}

/** Los tickets en el orden en que se ven en la bandeja: lo que usan anterior y siguiente. */
export function ordenDeBandeja(tickets: TicketResumen[], ahora: number = Date.now()): TicketResumen[] {
  return agruparPorUrgencia(tickets, ahora).flatMap((grupo) => grupo.tickets);
}
