import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { provideNativeDateAdapter } from '@angular/material/core';
import { BrziUpitComponent } from './brzi-upit.component';

describe('BrziUpitComponent', () => {
  let fixture: ComponentFixture<BrziUpitComponent>;
  let http: HttpTestingController;
  const prozor = { close: vi.fn() };
  const router = { navigate: vi.fn() };

  beforeEach(() => {
    prozor.close.mockClear();
    router.navigate.mockClear();
    TestBed.configureTestingModule({
      imports: [BrziUpitComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNativeDateAdapter(),
        { provide: MatDialogRef, useValue: prozor },
        { provide: Router, useValue: router },
      ],
    });
    fixture = TestBed.createComponent(BrziUpitComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('bez datuma i imena se ne sprema', () => {
    fixture.componentInstance.spremi();
    http.expectNone('/api/vjencanja/upit');
    expect(prozor.close).not.toHaveBeenCalled();
  });

  it('šalje datum, ime i telefon, zatvara prozor i otvara novo vjenčanje', () => {
    const c = fixture.componentInstance;
    c.forma.setValue({ datum: new Date(2027, 5, 12), ime: ' Marija Kovač ', telefon: '098 555 111', napomena: '' });
    c.spremi();

    const zahtjev = http.expectOne('/api/vjencanja/upit');
    expect(zahtjev.request.method).toBe('POST');
    // Datum ide kao lokalni dan, bez pomaka u UTC koji bi ga vratio na 11. 6.
    expect(zahtjev.request.body).toEqual({ datum: '2027-06-12', ime: 'Marija Kovač', telefon: '098 555 111', napomena: null });
    zahtjev.flush({ id: 77 });

    expect(prozor.close).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/vjencanja', 77]);
  });

  it('nakon odabira datuma provjerava je li slobodan i piše „Samo upit” kad je na njemu samo upit', () => {
    fixture.componentInstance.provjeri(new Date(2027, 5, 12));
    http.expectOne((r) => r.url === '/api/vjencanja/dostupnost' && r.params.get('datum') === '2027-06-12')
      .flush({ datum: '2027-06-12', slobodno: false, blizu: [], suradnici: [],
        vjencanja: [{ id: 1, status: 'UPIT', mlada: 'Ana Horvat', mladozenja: '' }] });
    fixture.detectChanges();

    const ishod = (fixture.nativeElement as HTMLElement).querySelector('.ishod')!;
    expect(ishod.textContent).toContain('Samo upit');
    expect(ishod.classList.contains('upit')).toBe(true);
  });

  it('greška servera ostavlja prozor otvoren i pokaže poruku', () => {
    const c = fixture.componentInstance;
    c.forma.setValue({ datum: new Date(2027, 5, 12), ime: 'Marija', telefon: '', napomena: '' });
    c.spremi();
    http.expectOne('/api/vjencanja/upit').flush({ poruka: 'Ime je obavezno' }, { status: 400, statusText: 'Bad Request' });

    expect(prozor.close).not.toHaveBeenCalled();
    expect(c.greska()).toBeTruthy();
    expect(c.sprema()).toBe(false);
  });
});
