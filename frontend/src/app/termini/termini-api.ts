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
  paketi: { id: number; naziv: string; opis: string | null; trajanjeMinuta: number; cijena: number }[];
  dani: { datum: string; ukupno: number; slobodno: string[] }[];
}

export interface JavnaRezervacija {
  paketId: number;
  datum: string;
  vrijeme: string;
  ime: string;
  telefon: string;
  email: string;
  napomena?: string | null;
  privola: boolean;
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

  rucnaRezervacija(id: number, r: RucnaRezervacija): Observable<RezervacijaTermina> {
    return this.http.post<RezervacijaTermina>(`/api/termini/${id}/rezervacije`, r);
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
