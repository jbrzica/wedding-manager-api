import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, of, retry, shareReplay, tap, throwError, timer } from 'rxjs';
import { Korisnik, Poruka, TokenOdgovor } from './modeli';

// Ime i tvrtka zadnjeg prijavljenog (bez tokena), da se bez mreže zna čiji su spremljeni podaci
const KLJUC_KORISNIKA = 'wm-korisnik';
// Prijavljen bez mreže: zahtjevi idu bez tokena, a čim se mreža vrati prvi 401 dohvati pravi
const BEZ_TOKENA = '';

function procitajKorisnika(): Korisnik | null {
  try {
    const spremljen = localStorage.getItem(KLJUC_KORISNIKA);
    return spremljen ? (JSON.parse(spremljen) as Korisnik) : null;
  } catch {
    return null;
  }
}

// Nakon odjave na uređaju ne smiju ostati vjenčanja koja je service worker spremio za rad bez mreže
function obrisiSpremljenePodatke(): void {
  if (typeof caches === 'undefined') {
    return;
  }
  caches.keys()
    .then((imena) => Promise.all(imena.filter((ime) => ime.includes(':data:')).map((ime) => caches.delete(ime))))
    .catch(() => undefined);
}

/**
 * Access token (JWT, 15 minuta) drži se samo u memoriji, a ne u localStorageu,
 * pa ga skripta ubačena u stranicu (XSS) ne može ukrasti iz pohrane.
 * Prijavu kroz osvježavanje stranice i dane drži refresh token u HttpOnly kolačiću,
 * koji JavaScript uopće ne vidi; browser ga sam šalje na /api/auth/osvjezi.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private _token = signal<string | null>(null);
  private _korisnik = signal<Korisnik | null>(null);
  // Više zahtjeva koji istovremeno dobiju 401 dijele jedno osvježavanje
  private osvjezavanjeUTijeku: Observable<TokenOdgovor | null> | null = null;

  readonly token = this._token.asReadonly();
  readonly korisnik = this._korisnik.asReadonly();
  readonly prijavljen = computed(() => this._token() !== null);
  readonly vlasnik = computed(() => this._korisnik()?.uloga === 'VLASNIK');

  constructor() {
    // Starije verzije spremale su token u localStorage; ostatak se briše
    try {
      localStorage.removeItem('wm_token');
      localStorage.removeItem('wm_korisnik');
    } catch {
      // pohrana nije dostupna, nema se što brisati
    }
  }

  prijava(email: string, lozinka: string): Observable<TokenOdgovor> {
    return this.http.post<TokenOdgovor>('/api/auth/prijava', { email, lozinka })
      .pipe(tap((o) => this.spremi(o)));
  }

  registracija(nazivTvrtke: string, ime: string, email: string, lozinka: string): Observable<Poruka> {
    return this.http.post<Poruka>('/api/auth/registracija', { nazivTvrtke, ime, email, lozinka });
  }

  potvrdiEmail(email: string, kod: string): Observable<TokenOdgovor> {
    return this.http.post<TokenOdgovor>('/api/auth/potvrdi-email', { email, kod })
      .pipe(tap((o) => this.spremi(o)));
  }

  ponovnoPosaljiKod(email: string): Observable<Poruka> {
    return this.http.post<Poruka>('/api/auth/ponovno-posalji-kod', { email });
  }

  prihvatiPoziv(email: string, kod: string, ime: string, lozinka: string): Observable<TokenOdgovor> {
    return this.http.post<TokenOdgovor>('/api/auth/prihvati-poziv', { email, kod, ime, lozinka })
      .pipe(tap((o) => this.spremi(o)));
  }

  zaboravljenaLozinka(email: string): Observable<Poruka> {
    return this.http.post<Poruka>('/api/auth/zaboravljena-lozinka', { email });
  }

  novaLozinka(email: string, kod: string, lozinka: string): Observable<TokenOdgovor> {
    return this.http.post<TokenOdgovor>('/api/auth/nova-lozinka', { email, kod, lozinka })
      .pipe(tap((o) => this.spremi(o)));
  }

  // Može li se registrirati nova tvrtka; stranica za prijavu prema tome pokaže ili sakrije poveznicu
  postavke(): Observable<{ registracijaOtvorena: boolean }> {
    return this.http.get<{ registracijaOtvorena: boolean }>('/api/auth/postavke');
  }

  // Novi access token preko kolačića; null ako prijave nema ili je istekla
  osvjezi(): Observable<TokenOdgovor | null> {
    if (!this.osvjezavanjeUTijeku) {
      this.osvjezavanjeUTijeku = this.http.post<TokenOdgovor>('/api/auth/osvjezi', {}).pipe(
        // Druga kartica je možda upravo zamijenila kolačić; preglednik ga je već spremio, pa jedan
        // ponovni pokušaj s novim kolačićem uspije umjesto da se korisnika odjavi
        retry({ count: 1, delay: (err: HttpErrorResponse) => (err.status === 401 ? timer(500) : throwError(() => err)) }),
        tap((o) => this.spremi(o)),
        catchError((err: HttpErrorResponse) => {
          // Bez mreže (0) ili kad service worker ne dobije server (504) prijava nije istekla, samo se
          // ne može provjeriti. Tko je na ovom uređaju već bio prijavljen, vidi spremljene podatke.
          const bezMreze = err.status === 0 || err.status === 504;
          const spremljen = bezMreze ? procitajKorisnika() : null;
          if (spremljen) {
            this._token.set(BEZ_TOKENA);
            this._korisnik.set(spremljen);
            return of(null);
          }
          this.ocisti();
          return of(null);
        }),
        finalize(() => (this.osvjezavanjeUTijeku = null)),
        shareReplay(1),
      );
    }
    return this.osvjezavanjeUTijeku;
  }

  // Nakon promjene naziva tvrtke osvježavamo podatke korisnika
  osvjeziKorisnika(): void {
    this.http.get<Korisnik>('/api/auth/ja').subscribe((k) => this._korisnik.set(k));
  }

  odjava(): void {
    // Server opoziva refresh token i briše kolačić; lokalno brišemo odmah, i ako server ne odgovori
    this.http.post('/api/auth/odjava', {}).subscribe({ error: () => undefined });
    this.istekla();
  }

  // Prijava je istekla ili opozvana: samo očisti i vrati na prijavu
  istekla(): void {
    this.ocisti();
    this.router.navigate(['/prijava']);
  }

  private spremi(o: TokenOdgovor): void {
    this._token.set(o.token);
    this._korisnik.set(o.korisnik);
    try {
      localStorage.setItem(KLJUC_KORISNIKA, JSON.stringify(o.korisnik));
    } catch {
      // bez pohrane nema rada bez mreže, sve ostalo radi
    }
  }

  private ocisti(): void {
    this._token.set(null);
    this._korisnik.set(null);
    try {
      localStorage.removeItem(KLJUC_KORISNIKA);
    } catch {
      // nema se što brisati
    }
    obrisiSpremljenePodatke();
  }
}
