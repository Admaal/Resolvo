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

/** Tiempo que falta (o que ha pasado) respecto al vencimiento: «3h 20m», «1d 2h» o «−1h 45m». */
export function tiempoSla(venceEl: string, ahora: number = Date.now()): string {
  const diferencia = Math.round((new Date(venceEl).getTime() - ahora) / 60_000);
  const minutos = Math.abs(diferencia);
  const dias = Math.floor(minutos / 1440);
  const horas = Math.floor((minutos % 1440) / 60);
  const resto = minutos % 60;
  const partes: string[] = [];
  if (dias > 0) {
    partes.push(`${dias}d`);
    if (horas > 0) partes.push(`${horas}h`);
  } else {
    if (horas > 0) partes.push(`${horas}h`);
    if (resto > 0 || horas === 0) partes.push(`${resto}m`);
  }
  return `${diferencia < 0 ? '\u2212' : ''}${partes.join(' ')}`;
}

/** El SLA solo corre mientras el ticket está abierto o pendiente. */
export function slaCorre(estado: string): boolean {
  return estado === 'ABIERTO' || estado === 'PENDIENTE';
}
