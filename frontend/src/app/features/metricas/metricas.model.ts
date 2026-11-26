/** Lo que devuelve GET /api/metricas/ia: el total del rango y una fila por operación y modelo. */
export interface FilaMetricas {
  operacion: string;
  modelo: string;
  llamadas: number;
  fallos: number;
  tokensEntrada: number;
  tokensSalida: number;
  tokensMedios: number;
  p50Ms: number;
  p95Ms: number;
  costeUsd: number | null;
}

export interface MetricasIa {
  desde: string;
  hasta: string;
  llamadas: number;
  fallos: number;
  tokensEntrada: number;
  tokensSalida: number;
  costeUsd: number | null;
  nota: string;
  filas: FilaMetricas[];
}
