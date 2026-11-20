export type NivelSla = 'ok' | 'riesgo' | 'incumplido';

function capitalizar(valor: string): string {
  return valor.charAt(0).toUpperCase() + valor.slice(1).toLowerCase();
}

export function etiquetaEstado(estado: string): string {
  return capitalizar(estado);
}

export function etiquetaPrioridad(prioridad: string): string {
  return capitalizar(prioridad);
}

/** Por debajo del 25 % del plazo restante el ticket está en riesgo (design §2.3). */
export function nivelSla(
  ticket: { venceEl: string; abiertoEn?: string },
  ahora: number = Date.now(),
): NivelSla {
  const vence = new Date(ticket.venceEl).getTime();
  if (ahora >= vence) return 'incumplido';
  const abierto = ticket.abiertoEn ? new Date(ticket.abiertoEn).getTime() : ahora;
  const total = vence - abierto;
  if (total > 0 && (vence - ahora) / total < 0.25) return 'riesgo';
  return 'ok';
}

export function textoSla(nivel: NivelSla): string {
  if (nivel === 'incumplido') return 'SLA incumplido';
  if (nivel === 'riesgo') return 'SLA en riesgo';
  return 'SLA en plazo';
}
