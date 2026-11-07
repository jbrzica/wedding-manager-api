// Oblici podataka koje vraća backend (isti nazivi kao Java DTO-ovi)

export type StatusVjencanja = 'UPIT' | 'POTVRDENO' | 'ODRADENO' | 'ISPORUCENO' | 'OTKAZANO';
export type VrstaUplate = 'KAPARA' | 'RATA' | 'OSTATAK';
export type UlogaSuradnika = 'FOTOGRAF' | 'VIDEOGRAF' | 'DRON' | 'ASISTENT' | 'OSTALO';
export type IzvorUpita = 'INSTAGRAM' | 'FACEBOOK' | 'GOOGLE' | 'WEB_STRANICA' | 'PREPORUKA' | 'OSTALO';
export type UlogaKorisnika = 'VLASNIK' | 'CLAN';
export type Razdoblje = 'NADOLAZECA' | 'PROSLA' | 'SVA';

export interface SuradnikPregled {
  ime: string;
  tvrtka: string;
  kalendar: string;
  vjencanja: {
    datum: string;
    lokacija: string | null;
    potvrdeno: boolean;
    mojaUloga: UlogaSuradnika | null;
    mlada: { ime: string; telefon: string | null } | null;
    mladozenja: { ime: string; telefon: string | null } | null;
    raspored: StavkaRasporeda[];
    ekipa: { ime: string; uloga: UlogaSuradnika | null; telefon: string | null; ja: boolean }[];
  }[];
}

export interface FilterVjencanja {
  razdoblje: Razdoblje;
  statusi: StatusVjencanja[];
  godina: number | null;
  nenaplaceno: boolean;
  kasniIsporuka: boolean;
}

export interface Korisnik {
  id: number;
  ime: string;
  email: string;
  uloga: UlogaKorisnika;
  tvrtkaId: number;
  nazivTvrtke: string;
}

export interface TokenOdgovor {
  token: string;
  korisnik: Korisnik;
}

export interface Poruka {
  poruka: string;
}

export interface Osoba {
  ime: string;
  prezime: string;
  telefon?: string | null;
  email?: string | null;
  adresa?: string | null;
  oib?: string | null;
}

export interface Paket {
  id: number;
  naziv: string;
  opis?: string | null;
  cijena: number;
}

export interface Dodatak {
  id: number;
  naziv: string;
  cijena: number;
}

export interface Suradnik {
  id: number;
  ime: string;
  uloga: UlogaSuradnika;
  telefon?: string | null;
  email?: string | null;
  napomena?: string | null;
}

export interface StavkaRasporeda {
  vrijeme?: string | null;
  naziv: string;
  adresa?: string | null;
  napomena?: string | null;
}

export interface ClanEkipe {
  suradnikId: number;
  ime: string;
  uloga: UlogaSuradnika;
  telefon?: string | null;
  honorar?: number | null;
}

export interface Uplata {
  id: number;
  iznos: number;
  datum: string;
  vrsta: VrstaUplate;
  opis?: string | null;
}

export interface Zadatak {
  id: number;
  naziv: string;
  gotovo: boolean;
}

export interface Financije {
  dogovorenaCijena: number;
  uplaceno: number;
  preostalo: number;
  kaparaPlacena: boolean;
  honorariEkipe: number;
  ostaliTroskovi: number;
  zarada: number;
}

export interface VjencanjeSazetak {
  id: number;
  datum: string;
  lokacija?: string | null;
  mlada: string;
  mladozenja: string;
  status: StatusVjencanja;
  dogovorenaCijena: number;
  uplaceno: number;
  preostalo: number;
  kaparaPlacena: boolean;
}

export interface VjencanjeDetalji {
  id: number;
  datum: string;
  lokacija?: string | null;
  mlada: Osoba;
  mladozenja: Osoba;
  paket?: Paket | null;
  dodaci: Dodatak[];
  status: StatusVjencanja;
  rokIsporuke?: string | null;
  izvorUpita?: IzvorUpita | null;
  napomene?: string | null;
  brojGostiju?: number | null;
  linkGalerije?: string | null;
  raspored: StavkaRasporeda[];
  ekipa: ClanEkipe[];
  uplate: Uplata[];
  zadaci: Zadatak[];
  financije: Financije;
}

export interface VjencanjeZahtjev {
  datum: string;
  lokacija?: string | null;
  mlada: Osoba;
  mladozenja: Osoba;
  paketId?: number | null;
  dodatakIds: number[];
  dogovorenaCijena?: number | null;
  rokIsporuke?: string | null;
  izvorUpita?: IzvorUpita | null;
  napomene?: string | null;
  brojGostiju?: number | null;
  ostaliTroskovi?: number | null;
  linkGalerije?: string | null;
  raspored: StavkaRasporeda[];
  ekipa: { suradnikId: number; uloga?: UlogaSuradnika | null; honorar?: number | null }[];
}

export interface Pregled {
  sljedeceVjencanje?: VjencanjeSazetak | null;
  danaDoSljedeceg?: number | null;
  brojOveGodine: number;
  ukupnoNenaplaceno: number;
}

export interface SazetakGodine {
  broj: number;
  prihod: number;
  zarada: number;
  prosjecnaCijena: number;
}

export interface Statistika {
  godina: number;
  ovaGodina: SazetakGodine;
  proslaGodina: SazetakGodine;
  poMjesecima: { mjesec: number; broj: number; dogovoreno: number; uplaceno: number }[];
  izvori: { izvor: IzvorUpita | null; upita: number; posao: number }[];
  paketi: { naziv: string; broj: number }[];
  dodaci: { naziv: string; broj: number }[];
  subote: { datum: string; dogovoreno: number; upita: number }[];
  honorari: { ime: string; brojVjencanja: number; ukupno: number }[];
}

export interface ClanTvrtke {
  id: number;
  ime: string;
  email: string;
  uloga: UlogaKorisnika;
}

export interface Tvrtka {
  id: number;
  naziv: string;
  clanovi: ClanTvrtke[];
}
