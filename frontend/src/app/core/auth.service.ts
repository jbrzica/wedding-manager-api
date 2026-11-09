import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, of, shareReplay, tap } from 'rxjs';
import { Korisnik, Poruka, TokenOdgovor } from './modeli';

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

  // Novi access token preko kolačića; null ako prijave nema ili je istekla
  osvjezi(): Observable<TokenOdgovor | null> {
    if (!this.osvjezavanjeUTijeku) {
      this.osvjezavanjeUTijeku = this.http.post<TokenOdgovor>('/api/auth/osvjezi', {}).pipe(
        tap((o) => this.spremi(o)),
        catchError(() => {
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
  }

  private ocisti(): void {
    this._token.set(null);
    this._korisnik.set(null);
  }
}
