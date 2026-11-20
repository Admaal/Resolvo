export type Tema = 'dark' | 'light';

export const CLAVE_TEMA = 'resolvo.tema';

/** Elección guardada si es válida; si no, la preferencia del sistema. */
export function temaInicial(guardado: string | null, prefiereOscuro: boolean): Tema {
  if (guardado === 'dark' || guardado === 'light') return guardado;
  return prefiereOscuro ? 'dark' : 'light';
}
