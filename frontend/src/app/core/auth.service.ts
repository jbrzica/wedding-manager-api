import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { Korisnik, Poruka, TokenOdgovor } from './modeli';

const KLJUC_TOKENA = 'wm_token';
const KLJUC_KORISNIKA = 'wm_korisnik';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private _token = signal<string | null>(citaj(KLJUC_TOKENA));
  private _korisnik = signal<Korisnik | null>(parsiraj(citaj(KLJUC_KORISNIKA)));

  readonly token = this._token.asReadonly();
  readonly korisnik = this._korisnik.asReadonly();
  readonly prijavljen = computed(() => this._token() !== null);
  readonly vlasnik = computed(() => this._korisnik()?.uloga === 'VLASNIK');

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

  // Nakon promjene naziva tvrtke osvježavamo spremljenog korisnika
  osvjeziKorisnika(): void {
    this.http.get<Korisnik>('/api/auth/ja').subscribe((k) => {
      this._korisnik.set(k);
      pisi(KLJUC_KORISNIKA, JSON.stringify(k));
    });
  }

  odjava(): void {
    this._token.set(null);
    this._korisnik.set(null);
    brisi(KLJUC_TOKENA);
    brisi(KLJUC_KORISNIKA);
    this.router.navigate(['/prijava']);
  }

  private spremi(o: TokenOdgovor): void {
    this._token.set(o.token);
    this._korisnik.set(o.korisnik);
    pisi(KLJUC_TOKENA, o.token);
    pisi(KLJUC_KORISNIKA, JSON.stringify(o.korisnik));
  }
}

// localStorage može baciti grešku (privatni prozor), pa ga omotamo
function citaj(kljuc: string): string | null {
  try {
    return localStorage.getItem(kljuc);
  } catch {
    return null;
  }
}

function pisi(kljuc: string, vrijednost: string): void {
  try {
    localStorage.setItem(kljuc, vrijednost);
  } catch {
    // aplikacija radi i bez spremanja, samo traži ponovnu prijavu
  }
}

function brisi(kljuc: string): void {
  try {
    localStorage.removeItem(kljuc);
  } catch {
    // isto kao gore
  }
}

function parsiraj(json: string | null): Korisnik | null {
  try {
    return json ? JSON.parse(json) : null;
  } catch {
    return null;
  }
}
