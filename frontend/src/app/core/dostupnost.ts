import { Dostupnost } from './modeli';

export type StanjeDatuma = 'slobodno' | 'upit' | 'zauzeto';

export const OPIS_STANJA: Record<StanjeDatuma, { naslov: string; ikona: string }> = {
  slobodno: { naslov: 'Slobodno', ikona: 'event_available' },
  upit: { naslov: 'Samo upit', ikona: 'event_note' },
  zauzeto: { naslov: 'Zauzeto', ikona: 'event_busy' },
};

// Upit još nije posao: datum za koji se netko samo raspitivao nije zauzet, pa se drugi par ne smije odbiti
export function stanjeDatuma(d: Dostupnost): StanjeDatuma {
  if (d.vjencanja.length === 0) {
    return 'slobodno';
  }
  return d.vjencanja.every((v) => v.status === 'UPIT') ? 'upit' : 'zauzeto';
}
