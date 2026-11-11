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

// Kad se traži datoteka (responseType 'blob'), i poruka o grešci stigne kao Blob pa je treba prvo pročitati
export async function porukaGreskeDatoteke(err: unknown): Promise<string> {
  if (err instanceof HttpErrorResponse && err.error instanceof Blob) {
    try {
      const tijelo = JSON.parse(await err.error.text());
      return porukaGreske(new HttpErrorResponse({ error: tijelo, status: err.status }));
    } catch {
      // nije JSON, pada na opću poruku
    }
  }
  return porukaGreske(err);
}
