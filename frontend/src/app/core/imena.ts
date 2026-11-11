// "Ana & Ivo"; kod brzog upita zna se samo jedno ime, pa tada bez znaka &
export function par(mlada: string | null | undefined, mladozenja: string | null | undefined): string {
  return [mlada, mladozenja].map((ime) => (ime ?? '').trim()).filter(Boolean).join(' & ');
}

// "Ana Horvat" -> "AH", za krug s inicijalima
export function inicijali(ime: string | null | undefined): string {
  return (ime ?? '').trim().split(/\s+/).filter(Boolean).slice(0, 2).map((dio) => dio[0].toUpperCase()).join('');
}

// Koliko je dana do datuma, riječima: "danas", "sutra", "za 9 dana", "prije 3 dana"
export function zaKoliko(dana: number): string {
  if (dana === 0) {
    return 'danas';
  }
  if (dana === 1) {
    return 'sutra';
  }
  if (dana === -1) {
    return 'jučer';
  }
  // 21, 31... dan; ostalo dana (11 je iznimka)
  const rijec = Math.abs(dana) % 10 === 1 && Math.abs(dana) % 100 !== 11 ? 'dan' : 'dana';
  return dana > 0 ? `za ${dana} ${rijec}` : `prije ${-dana} ${rijec}`;
}
