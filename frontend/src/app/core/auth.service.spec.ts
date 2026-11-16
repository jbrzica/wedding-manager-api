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

  it('nakon 401 pokuša još jednom i uspije s kolačićem koji je druga kartica upravo dobila', () => {
    vi.useFakeTimers();
    auth.osvjezi().subscribe();
    http.expectOne('/api/auth/osvjezi').flush({}, { status: 401, statusText: 'Unauthorized' });
    vi.advanceTimersByTime(500);
    http.expectOne('/api/auth/osvjezi').flush({ token: 'novi', korisnik: KORISNIK });
    vi.useRealTimers();

    expect(auth.prijavljen()).toBe(true);
    expect(auth.token()).toBe('novi');
  });

  it('bez mreže i bez ranije prijave nije prijavljen', () => {
    auth.osvjezi().subscribe();
    http.expectOne('/api/auth/osvjezi').error(new ProgressEvent('error'), { status: 0 });

    expect(auth.prijavljen()).toBe(false);
  });

  it('istekla prijava (401) odjavljuje i briše spremljenog korisnika', () => {
    vi.useFakeTimers();
    localStorage.setItem('wm-korisnik', JSON.stringify(KORISNIK));

    auth.osvjezi().subscribe();
    http.expectOne('/api/auth/osvjezi').flush({ poruka: 'Prijava je istekla' }, { status: 401, statusText: 'Unauthorized' });
    // Jedan ponovni pokušaj, jer je druga kartica možda upravo zamijenila kolačić
    vi.advanceTimersByTime(500);
    http.expectOne('/api/auth/osvjezi').flush({ poruka: 'Prijava je istekla' }, { status: 401, statusText: 'Unauthorized' });
    vi.useRealTimers();

    expect(auth.prijavljen()).toBe(false);
    expect(localStorage.getItem('wm-korisnik')).toBeNull();
  });
});
