import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BrziUpit, Dodatak, Dostupnost, Stranica, PodaciTvrtke, PredlozakUgovora, Prognoza, Promjena, StavkaRasporeda, FilterVjencanja, KosStavka, SuradnikPregled, Financije, Statistika, Paket, Poruka, Pregled, StatusVjencanja, Suradnik, Tvrtka,
  Uplata, VjencanjeDetalji, Zadatak, VjencanjeSazetak, VjencanjeZahtjev, VrstaUplate,
} from './modeli';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  // Vjenčanja

  popis(filter: FilterVjencanja, pretraga: string, stranica: number, velicina: number): Observable<Stranica<VjencanjeSazetak>> {
    let params = new HttpParams().set('razdoblje', filter.razdoblje).set('stranica', stranica).set('velicina', velicina);
    for (const s of filter.statusi) {
      params = params.append('status', s);
    }
    if (filter.godina) {
      params = params.set('godina', filter.godina);
    }
    if (filter.nenaplaceno) {
      params = params.set('nenaplaceno', true);
    }
    if (filter.kasniIsporuka) {
      params = params.set('kasniIsporuka', true);
    }
    if (filter.bezKapare) {
      params = params.set('bezKapare', true);
    }
    if (pretraga) {
      params = params.set('pretraga', pretraga);
    }
    return this.http.get<Stranica<VjencanjeSazetak>>('/api/vjencanja', { params });
  }

  kalendar(od: string, doDatuma: string): Observable<VjencanjeSazetak[]> {
    const params = new HttpParams().set('od', od).set('do', doDatuma);
    return this.http.get<VjencanjeSazetak[]>('/api/vjencanja/kalendar', { params });
  }

  pregled(): Observable<Pregled> {
    return this.http.get<Pregled>('/api/vjencanja/pregled');
  }

  upozorenja(datum: string, suradnikIds: number[], izuzmiId?: number): Observable<{ upozorenja: string[] }> {
    let params = new HttpParams().set('datum', datum);
    suradnikIds.forEach((id) => (params = params.append('suradnikIds', id)));
    if (izuzmiId) {
      params = params.set('izuzmiId', izuzmiId);
    }
    return this.http.get<{ upozorenja: string[] }>('/api/vjencanja/upozorenja', { params });
  }

  detalji(id: number): Observable<VjencanjeDetalji> {
    return this.http.get<VjencanjeDetalji>(`/api/vjencanja/${id}`);
  }

  financije(id: number): Observable<Financije> {
    return this.http.get<Financije>(`/api/vjencanja/${id}/financije`);
  }

  stvoriVjencanje(z: VjencanjeZahtjev): Observable<VjencanjeDetalji> {
    return this.http.post<VjencanjeDetalji>('/api/vjencanja', z);
  }

  brziUpit(upit: BrziUpit): Observable<VjencanjeDetalji> {
    return this.http.post<VjencanjeDetalji>('/api/vjencanja/upit', upit);
  }

  izmijeniVjencanje(id: number, z: VjencanjeZahtjev): Observable<VjencanjeDetalji> {
    return this.http.put<VjencanjeDetalji>(`/api/vjencanja/${id}`, z);
  }

  promijeniStatus(id: number, status: StatusVjencanja): Observable<VjencanjeDetalji> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<VjencanjeDetalji>(`/api/vjencanja/${id}/status`, null, { params });
  }

  obrisiVjencanje(id: number): Observable<void> {
    return this.http.delete<void>(`/api/vjencanja/${id}`);
  }

  predlozakRasporeda(): Observable<StavkaRasporeda[]> {
    return this.http.get<StavkaRasporeda[]>('/api/tvrtka/predlozak-rasporeda');
  }

  spremiPredlozakRasporeda(stavke: StavkaRasporeda[]): Observable<StavkaRasporeda[]> {
    return this.http.put<StavkaRasporeda[]>('/api/tvrtka/predlozak-rasporeda', stavke);
  }

  prviKoraci(): Observable<{ paketi: boolean; suradnici: boolean; vjencanje: boolean; kalendar: boolean; brojVjencanja: number }> {
    return this.http.get<{ paketi: boolean; suradnici: boolean; vjencanje: boolean; kalendar: boolean; brojVjencanja: number }>('/api/tvrtka/prvi-koraci');
  }

  dostupnost(datum: string): Observable<Dostupnost> {
    return this.http.get<Dostupnost>('/api/vjencanja/dostupnost', { params: { datum } });
  }

  kos(): Observable<KosStavka[]> {
    return this.http.get<KosStavka[]>('/api/kos');
  }

  vratiIzKosa(id: number): Observable<void> {
    return this.http.post<void>(`/api/kos/${id}/vrati`, {});
  }

  obrisiTrajno(id: number): Observable<void> {
    return this.http.delete<void>(`/api/kos/${id}`);
  }

  // Uplate

  dodajUplatu(vjencanjeId: number, u: { iznos: number; datum: string; vrsta: VrstaUplate; opis?: string | null }): Observable<Uplata> {
    return this.http.post<Uplata>(`/api/vjencanja/${vjencanjeId}/uplate`, u);
  }

  obrisiUplatu(vjencanjeId: number, uplataId: number): Observable<void> {
    return this.http.delete<void>(`/api/vjencanja/${vjencanjeId}/uplate/${uplataId}`);
  }

  // Kontrolna lista

  dodajZadatak(vjencanjeId: number, naziv: string): Observable<Zadatak> {
    return this.http.post<Zadatak>(`/api/vjencanja/${vjencanjeId}/zadaci`, { naziv });
  }

  oznaciZadatak(vjencanjeId: number, zadatakId: number, gotovo: boolean): Observable<Zadatak> {
    const params = new HttpParams().set('gotovo', gotovo);
    return this.http.patch<Zadatak>(`/api/vjencanja/${vjencanjeId}/zadaci/${zadatakId}`, null, { params });
  }

  obrisiZadatak(vjencanjeId: number, zadatakId: number): Observable<void> {
    return this.http.delete<void>(`/api/vjencanja/${vjencanjeId}/zadaci/${zadatakId}`);
  }

  // Paketi, dodaci i suradnici imaju isti oblik: /api/<resurs>[/id]

  paketi(): Observable<Paket[]> {
    return this.http.get<Paket[]>('/api/paketi');
  }

  spremiPaket(p: Omit<Paket, 'id'>, id?: number): Observable<Paket> {
    return id ? this.http.put<Paket>(`/api/paketi/${id}`, p) : this.http.post<Paket>('/api/paketi', p);
  }

  obrisiPaket(id: number): Observable<void> {
    return this.http.delete<void>(`/api/paketi/${id}`);
  }

  dodaci(): Observable<Dodatak[]> {
    return this.http.get<Dodatak[]>('/api/dodaci');
  }

  spremiDodatak(d: Omit<Dodatak, 'id'>, id?: number): Observable<Dodatak> {
    return id ? this.http.put<Dodatak>(`/api/dodaci/${id}`, d) : this.http.post<Dodatak>('/api/dodaci', d);
  }

  obrisiDodatak(id: number): Observable<void> {
    return this.http.delete<void>(`/api/dodaci/${id}`);
  }

  suradnici(): Observable<Suradnik[]> {
    return this.http.get<Suradnik[]>('/api/suradnici');
  }

  spremiSuradnika(s: Omit<Suradnik, 'id'>, id?: number): Observable<Suradnik> {
    return id ? this.http.put<Suradnik>(`/api/suradnici/${id}`, s) : this.http.post<Suradnik>('/api/suradnici', s);
  }

  suradnikLink(id: number): Observable<{ putanja: string }> {
    return this.http.get<{ putanja: string }>(`/api/suradnici/${id}/link`);
  }

  noviSuradnikLink(id: number): Observable<{ putanja: string }> {
    return this.http.post<{ putanja: string }>(`/api/suradnici/${id}/link/novi`, {});
  }

  suradnikPregled(token: string): Observable<SuradnikPregled> {
    return this.http.get<SuradnikPregled>(`/api/javno/suradnik/${encodeURIComponent(token)}`);
  }

  obrisiSuradnika(id: number): Observable<void> {
    return this.http.delete<void>(`/api/suradnici/${id}`);
  }

  // Statistika

  statistika(godina: number): Observable<Statistika> {
    return this.http.get<Statistika>('/api/statistika', { params: new HttpParams().set('godina', godina) });
  }

  // Tvrtka

  // Prognoza za vjenčanja idućih 7 dana, ključ je id vjenčanja
  prognoze(): Observable<Record<number, Prognoza>> {
    return this.http.get<Record<number, Prognoza>>('/api/vjencanja/prognoze');
  }

  povijestVjencanja(id: number): Observable<Promjena[]> {
    return this.http.get<Promjena[]>(`/api/vjencanja/${id}/povijest`);
  }

  povijest(korisnikId: number | null): Observable<Promjena[]> {
    const params = korisnikId ? new HttpParams().set('korisnikId', korisnikId) : undefined;
    return this.http.get<Promjena[]>('/api/povijest', { params });
  }

  tvrtka(): Observable<Tvrtka> {
    return this.http.get<Tvrtka>('/api/tvrtka');
  }

  spremiTvrtku(podaci: PodaciTvrtke): Observable<Tvrtka> {
    return this.http.put<Tvrtka>('/api/tvrtka', podaci);
  }

  // Ugovor

  ugovor(vjencanjeId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`/api/vjencanja/${vjencanjeId}/ugovor`, { observe: 'response', responseType: 'blob' });
  }

  predlozakUgovora(): Observable<PredlozakUgovora> {
    return this.http.get<PredlozakUgovora>('/api/tvrtka/ugovor');
  }

  preuzmiPredlozakUgovora(): Observable<HttpResponse<Blob>> {
    return this.http.get('/api/tvrtka/ugovor/predlozak', { observe: 'response', responseType: 'blob' });
  }

  ucitajPredlozakUgovora(datoteka: File): Observable<PredlozakUgovora> {
    const podaci = new FormData();
    podaci.append('datoteka', datoteka);
    return this.http.put<PredlozakUgovora>('/api/tvrtka/ugovor/predlozak', podaci);
  }

  vratiZadaniPredlozak(): Observable<PredlozakUgovora> {
    return this.http.delete<PredlozakUgovora>('/api/tvrtka/ugovor/predlozak');
  }

  kalendarLink(): Observable<{ putanja: string }> {
    return this.http.get<{ putanja: string }>('/api/kalendar/link');
  }

  noviKalendarLink(): Observable<{ putanja: string }> {
    return this.http.post<{ putanja: string }>('/api/kalendar/link/novi', {});
  }

  promjenaLozinke(staraLozinka: string, novaLozinka: string): Observable<Poruka> {
    return this.http.post<Poruka>('/api/auth/promjena-lozinke', { staraLozinka, novaLozinka });
  }

  pozovi(email: string): Observable<Poruka> {
    return this.http.post<Poruka>('/api/tvrtka/pozivi', { email });
  }

  ukloniClana(korisnikId: number): Observable<void> {
    return this.http.delete<void>(`/api/tvrtka/clanovi/${korisnikId}`);
  }
}
