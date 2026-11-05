import { IzvorUpita, StatusVjencanja, UlogaSuradnika, VrstaUplate } from './modeli';

// Tekst koji korisnik vidi umjesto naziva enuma

export const STATUSI: Record<StatusVjencanja, string> = {
  UPIT: 'Upit',
  POTVRDENO: 'Potvrđeno',
  ODRADENO: 'Odrađeno',
  ISPORUCENO: 'Isporučeno',
};

export const VRSTE_UPLATE: Record<VrstaUplate, string> = {
  KAPARA: 'Kapara',
  RATA: 'Rata',
  OSTATAK: 'Ostatak',
};

export const ULOGE_SURADNIKA: Record<UlogaSuradnika, string> = {
  FOTOGRAF: 'Fotograf',
  VIDEOGRAF: 'Videograf',
  DRON: 'Dron',
  ASISTENT: 'Asistent',
  OSTALO: 'Ostalo',
};

export const IZVORI_UPITA: Record<IzvorUpita, string> = {
  INSTAGRAM: 'Instagram',
  FACEBOOK: 'Facebook',
  GOOGLE: 'Google',
  WEB_STRANICA: 'Web stranica',
  PREPORUKA: 'Preporuka',
  OSTALO: 'Ostalo',
};

export function kljucevi<T extends string>(oznake: Record<T, string>): T[] {
  return Object.keys(oznake) as T[];
}
