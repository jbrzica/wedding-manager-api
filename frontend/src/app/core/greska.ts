import { HttpErrorResponse } from '@angular/common/http';

// Backend vraća { status, poruka, greske: { polje: poruka } }
export function porukaGreske(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Server nije dostupan. Je li backend pokrenut?';
    }
    const tijelo = err.error;
    if (tijelo?.greske) {
      return Object.values(tijelo.greske).join(', ');
    }
    if (tijelo?.poruka) {
      return tijelo.poruka;
    }
  }
  return 'Nešto je pošlo po zlu.';
}
