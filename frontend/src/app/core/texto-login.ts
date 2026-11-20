/** Traduce el código de error del inicio de sesión a un mensaje para la persona. */
export function textoDeLogin(fallo: unknown): string {
  switch (fallo instanceof Error ? fallo.message : '') {
    case 'demasiados-intentos':
      return 'Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo.';
    case 'no-autorizado':
      return 'Email o contraseña incorrectos.';
    default:
      return 'No se pudo entrar, inténtalo de nuevo.';
  }
}
