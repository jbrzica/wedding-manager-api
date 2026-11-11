import { HttpResponse } from '@angular/common/http';

// Ime datoteke iz zaglavlja Content-Disposition (filename* s UTF-8 ima prednost, zbog č, ć, š, ž i đ)
export function imeDatoteke(odgovor: HttpResponse<Blob>, zadano: string): string {
  const zaglavlje = odgovor.headers.get('Content-Disposition') ?? '';
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(zaglavlje);
  if (utf8) {
    return decodeURIComponent(utf8[1]);
  }
  const obicno = /filename="?([^";]+)"?/i.exec(zaglavlje);
  return obicno ? obicno[1] : zadano;
}

/**
 * Na mobitelu otvori izbornik za dijeljenje (Spremi u Datoteke, Ispis, Mail, WhatsApp),
 * jer iPhone u aplikaciji s početnog zaslona ne zna preuzimati datoteke. Na računalu preuzme.
 * Vraća false kad preglednik dijeljenje odbije jer nije odmah nakon dodira; tada treba novi dodir.
 */
export async function spremiIliPodijeli(blob: Blob, ime: string): Promise<boolean> {
  const datoteka = new File([blob], ime, { type: blob.type });
  const mobitel = window.matchMedia?.('(pointer: coarse)').matches;
  if (mobitel && navigator.canShare?.({ files: [datoteka] })) {
    try {
      await navigator.share({ files: [datoteka] });
      return true;
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') {
        return true; // korisnik je sam zatvorio izbornik
      }
      if (e instanceof DOMException && e.name === 'NotAllowedError') {
        return false;
      }
    }
  }
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = ime;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
  return true;
}
