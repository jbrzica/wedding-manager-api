import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

const KORISNIK = { id: 1, ime: 'Josip', email: 'j@test.hr', uloga: 'VLASNIK', nazivTvrtke: 'Foto Test' };

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('prijava sprema token u memoriju, a u pohranu samo korisnika', () => {
    auth.prijava('j@test.hr', 'lozinka123').subscribe();
    http.expectOne('/api/auth/prijava').flush({ token: 'abc', korisnik: KORISNIK });

    expect(auth.prijavljen()).toBe(true);
    expect(auth.token()).toBe('abc');
    expect(JSON.stringify(localStorage)).not.toContain('abc');
    expect(JSON.parse(localStorage.getItem('wm-korisnik')!).nazivTvrtke).toBe('Foto Test');
  });

  // U crkvi bez signala: aplikacija se otvori sa spremljenim podacima umjesto stranice za prijavu
  it('bez mreže ostaje prijavljen tko je na uređaju već bio prijavljen', () => {
    localStorage.setItem('wm-korisnik', JSON.stringify(KORISNIK));

    auth.osvjezi().subscribe();
    http.expectOne('/api/auth/osvjezi').error(new ProgressEvent('error'), { status: 0 });

    expect(auth.prijavljen()).toBe(true);
    expect(auth.korisnik()?.ime).toBe('Josip');
    expect(auth.token()).toBe('');
  });

  it('bez mreže i bez ranije prijave nije prijavljen', () => {
    auth.osvjezi().subscribe();
    http.expectOne('/api/auth/osvjezi').error(new ProgressEvent('error'), { status: 0 });

    expect(auth.prijavljen()).toBe(false);
  });

  it('istekla prijava (401) odjavljuje i briše spremljenog korisnika', () => {
    localStorage.setItem('wm-korisnik', JSON.stringify(KORISNIK));

    auth.osvjezi().subscribe();
    http.expectOne('/api/auth/osvjezi').flush({ poruka: 'Prijava je istekla' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.prijavljen()).toBe(false);
    expect(localStorage.getItem('wm-korisnik')).toBeNull();
  });
});
