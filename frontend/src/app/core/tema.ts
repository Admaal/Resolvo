export type Tema = 'dark' | 'light';

export const CLAVE_TEMA = 'resolvo.tema';

/** Elección guardada si es válida; si no, la preferencia del sistema. */
export function temaInicial(guardado: string | null, prefiereOscuro: boolean): Tema {
  if (guardado === 'dark' || guardado === 'light') return guardado;
  return prefiereOscuro ? 'dark' : 'light';
}

/** Cambia entre oscuro y claro, lo aplica al documento y lo guarda; devuelve el tema nuevo. */
export function alternarTema(): Tema {
  const siguiente: Tema = document.documentElement.dataset['theme'] === 'light' ? 'dark' : 'light';
  document.documentElement.dataset['theme'] = siguiente;
  try {
    localStorage.setItem(CLAVE_TEMA, siguiente);
  } catch {
    // sin almacenamiento: el cambio vale solo para esta sesión
  }
  return siguiente;
}
