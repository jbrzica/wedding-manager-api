import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Termini za kratka fotografiranja (npr. Božićno), odvojeni od vjenčanja. Vremena dolaze kao "10:30:00".

export interface PaketTermina {
  id?: number | null;
  naziv: string;
  opis?: string | null;
  trajanjeMinuta: number;
  cijena: number;
}

export interface DanTermina {
  datum: string;
  od: string;
  doVremena: string;
}

export interface DogadjajRequest {
  naziv: string;
  opis?: string | null;
  lokacija?: string | null;
  korakMinuta: number;
  otvoren: boolean;
  kratkiLink?: string | null;
  kontaktTelefon?: string | null;
  paketi: PaketTermina[];
  dani: DanTermina[];
}

export interface DogadjajSazetak {
  id: number;
  naziv: string;
  prviDan: string | null;
  zadnjiDan: string | null;
  otvoren: boolean;
  ukupnoTermina: number;
  zauzeto: number;
  prihod: number;
}

export interface RezervacijaTermina {
  id: number;
  datum: string;
  vrijeme: string;
  paketId: number | null;
  nazivPaketa: string;
  trajanjeMinuta: number;
  cijena: number;
  ime: string;
  telefon: string;
  email: string | null;
  napomena: string | null;
  rucno: boolean;
  kreirano: string;
}

export interface DanRaspored {
  datum: string;
  od: string;
  doVremena: string;
  mjesta: { vrijeme: string; rezervacija: RezervacijaTermina | null }[];
}

export interface DogadjajDetalji {
  id: number;
  naziv: string;
  opis: string | null;
  lokacija: string | null;
  korakMinuta: number;
  otvoren: boolean;
  javniKod: string;
  kratkiLink: string | null;
  kontaktTelefon: string | null;
  slikaVerzija: number;
  paketi: PaketTermina[];
  raspored: DanRaspored[];
  ukupnoTermina: number;
  zauzeto: number;
  prihod: number;
  poPaketima: { naziv: string; broj: number }[];
}

export interface RucnaRezervacija {
  paketId: number;
  datum: string;
  vrijeme: string;
  ime: string;
  telefon?: string | null;
  email?: string | null;
  napomena?: string | null;
}

export interface JavniDogadjaj {
  naziv: string;
  opis: string | null;
  lokacija: string | null;
  tvrtka: string;
  telefonTvrtke: string | null;
  otvoren: boolean;
  korakMinuta: number;
  otkazSati: number;
  slikaVerzija: number;
  paketi: { id: number; naziv: string; opis: string | null; trajanjeMinuta: number; cijena: number }[];
  dani: { datum: string; ukupno: number; slobodno: string[]; zauzeto: string[] }[];
}

export interface JavnaRezervacija {
  paketId: number;
  datum: string;
  vrijeme: string;
  ime: string;
  telefon: string;
  email: string;
  napomena?: string | null;
  web?: string;
}

export interface PotvrdaRezervacije {
  dogadjaj: string;
  tvrtka: string;
  telefonTvrtke: string | null;
  lokacija: string | null;
  datum: string;
  vrijeme: string;
  nazivPaketa: string;
  trajanjeMinuta: number;
  cijena: number;
  ime: string;
  otkazano: boolean;
  moguceOtkazati: boolean;
  terminProsao: boolean;
  otkazSati: number;
}

// Verzija u adresi znači da preglednik smije dugo čuvati sliku; nova slika dobije novu adresu
export function slikaUrl(kod: string, verzija: number): string | null {
  return verzija ? `/api/javno/termini/${encodeURIComponent(kod)}/slika?v=${verzija}` : null;
}

export interface NovaRezervacija {
  id: number;
  dogadjajId: number;
  dogadjaj: string;
  datum: string;
  vrijeme: string;
  ime: string;
  nazivPaketa: string;
  kreirano: string;
}

export interface NoveRezervacije {
  broj: number;
  rezervacije: NovaRezervacija[];
}

// Kratki link (npr. /r/bozicno) za objave i story; preusmjeri na javni link
export function kratkiLinkUrl(kratki: string): string {
  return `${window.location.origin}/r/${kratki}`;
}

// Javni link za klijente; isti oblik koristi i backend kad šalje mail
export function javniLink(kod: string): string {
  return `${window.location.origin}/rezervacija/${kod}`;
}

@Injectable({ providedIn: 'root' })
export class TerminiApi {
  private http = inject(HttpClient);

  popis(): Observable<DogadjajSazetak[]> {
    return this.http.get<DogadjajSazetak[]>('/api/termini');
  }

  detalji(id: number): Observable<DogadjajDetalji> {
    return this.http.get<DogadjajDetalji>(`/api/termini/${id}`);
  }

  stvori(d: DogadjajRequest): Observable<DogadjajDetalji> {
    return this.http.post<DogadjajDetalji>('/api/termini', d);
  }

  izmijeni(id: number, d: DogadjajRequest): Observable<DogadjajDetalji> {
    return this.http.put<DogadjajDetalji>(`/api/termini/${id}`, d);
  }

  obrisi(id: number): Observable<void> {
    return this.http.delete<void>(`/api/termini/${id}`);
  }

  noviLink(id: number): Observable<DogadjajDetalji> {
    return this.http.post<DogadjajDetalji>(`/api/termini/${id}/novi-link`, {});
  }

  spremiSliku(id: number, slika: Blob): Observable<{ slikaVerzija: number }> {
    const podaci = new FormData();
    podaci.append('slika', slika, 'naslovna.jpg');
    return this.http.put<{ slikaVerzija: number }>(`/api/termini/${id}/slika`, podaci);
  }

  obrisiSliku(id: number): Observable<void> {
    return this.http.delete<void>(`/api/termini/${id}/slika`);
  }

  rucnaRezervacija(id: number, r: RucnaRezervacija): Observable<RezervacijaTermina> {
    return this.http.post<RezervacijaTermina>(`/api/termini/${id}/rezervacije`, r);
  }

  nove(): Observable<NoveRezervacije> {
    return this.http.get<NoveRezervacije>('/api/termini/nove');
  }

  procitano(): Observable<void> {
    return this.http.post<void>('/api/termini/nove/procitano', {});
  }

  otkazi(rezervacijaId: number): Observable<void> {
    return this.http.post<void>(`/api/termini/rezervacije/${rezervacijaId}/otkazi`, {});
  }

  // ---------- javno ----------

  javniDogadjaj(kod: string): Observable<JavniDogadjaj> {
    return this.http.get<JavniDogadjaj>(`/api/javno/termini/${encodeURIComponent(kod)}`);
  }

  rezerviraj(kod: string, r: JavnaRezervacija): Observable<PotvrdaRezervacije> {
    return this.http.post<PotvrdaRezervacije>(`/api/javno/termini/${encodeURIComponent(kod)}/rezervacije`, r);
  }

  rezervacija(otkazniKod: string): Observable<PotvrdaRezervacije> {
    return this.http.get<PotvrdaRezervacije>(`/api/javno/otkazivanje/${encodeURIComponent(otkazniKod)}`);
  }

  otkaziJavno(otkazniKod: string): Observable<PotvrdaRezervacije> {
    return this.http.post<PotvrdaRezervacije>(`/api/javno/otkazivanje/${encodeURIComponent(otkazniKod)}`, {});
  }
}
