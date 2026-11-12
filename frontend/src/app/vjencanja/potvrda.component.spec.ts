import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNativeDateAdapter } from '@angular/material/core';
import { PotvrdaComponent } from './potvrda.component';

describe('PotvrdaComponent', () => {
  const prozor = { close: vi.fn() };

  function napravi(imaCijenu = true) {
    prozor.close.mockClear();
    TestBed.configureTestingModule({
      imports: [PotvrdaComponent],
      providers: [
        provideNativeDateAdapter(),
        { provide: MatDialogRef, useValue: prozor },
        { provide: MAT_DIALOG_DATA, useValue: { imena: 'Ana & Ivo', imaCijenu } },
      ],
    });
    const fixture = TestBed.createComponent(PotvrdaComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('s upisanom kaparom vraća iznos, datum i želju za ugovorom', () => {
    const c = napravi().componentInstance;
    c.forma.setValue({ kapara: 300, datum: new Date(2026, 10, 12), ugovor: true });
    c.potvrdi();
    expect(prozor.close).toHaveBeenCalledWith({ kapara: 300, datum: '2026-11-12', ugovor: true });
  });

  // Prazno polje ili 0 znači "kapara još nije plaćena": mijenja se samo status, uplata se ne upisuje
  it('bez kapare vraća null', () => {
    const c = napravi().componentInstance;
    c.forma.patchValue({ kapara: 0, ugovor: false });
    c.potvrdi();
    expect(prozor.close.mock.calls[0][0]).toMatchObject({ kapara: null, ugovor: false });
  });

  it('negativna kapara se ne prihvaća', () => {
    const c = napravi().componentInstance;
    c.forma.patchValue({ kapara: -50 });
    c.potvrdi();
    expect(prozor.close).not.toHaveBeenCalled();
  });

  it('upozorava kad cijena nije upisana', () => {
    const tekst = (napravi(false).nativeElement as HTMLElement).textContent;
    expect(tekst).toContain('Cijena još nije upisana');
  });
});
