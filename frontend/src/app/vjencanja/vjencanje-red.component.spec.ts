import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import hr from '@angular/common/locales/hr';
import { LOCALE_ID } from '@angular/core';
import { VjencanjeRedComponent } from './vjencanje-red.component';
import { VjencanjeSazetak } from '../core/modeli';

const OSNOVA: VjencanjeSazetak = {
  id: 5, datum: '2099-06-12', lokacija: 'Hotel Osijek', mlada: 'Ana Horvat', mladozenja: 'Ivo Kovač', status: 'POTVRDENO',
  dogovorenaCijena: 2000, uplaceno: 500, preostalo: 1500, kaparaPlacena: true, paket: 'Classic',
};

describe('VjencanjeRedComponent', () => {
  beforeAll(() => registerLocaleData(hr));

  function prikazi(v: Partial<VjencanjeSazetak>): HTMLElement {
    TestBed.configureTestingModule({
      imports: [VjencanjeRedComponent],
      providers: [provideRouter([]), { provide: LOCALE_ID, useValue: 'hr' }],
    });
    const fixture = TestBed.createComponent(VjencanjeRedComponent);
    fixture.componentRef.setInput('v', { ...OSNOVA, ...v });
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('pokazuje par, dvoranu s paketom i plaćeno od dogovorenog', () => {
    const red = prikazi({});
    expect(red.querySelector('.imena')!.textContent).toBe('Ana Horvat & Ivo Kovač');
    expect(red.querySelector('.lokacija')!.textContent).toBe('Hotel Osijek · Classic');
    expect(red.querySelector('.novac')!.textContent!.replace(/\s+/g, ' ').trim()).toBe('500 / 2.000 €');
    expect((red.querySelector('.ispuna') as HTMLElement).style.width).toBe('25%');
    expect(red.querySelector('a')!.getAttribute('href')).toBe('/vjencanja/5');
  });

  it('do kraja plaćeno vjenčanje piše „plaćeno”', () => {
    const red = prikazi({ uplaceno: 2000, preostalo: 0 });
    expect(red.querySelector('.novac')!.textContent!.trim()).toBe('plaćeno');
  });

  // Dug je crven tek kad je vjenčanje prošlo; prije toga je neplaćeni ostatak normalan
  it('dug je označen samo za prošlo vjenčanje', () => {
    expect(prikazi({}).querySelector('.novac')!.classList.contains('duguje')).toBe(false);
    TestBed.resetTestingModule();
    expect(prikazi({ datum: '2020-06-12' }).querySelector('.novac')!.classList.contains('duguje')).toBe(true);
  });

  it('upit bez cijene i bez mladoženje nema traku ni znak &', () => {
    const red = prikazi({ mladozenja: '', status: 'UPIT', dogovorenaCijena: 0, uplaceno: 0, preostalo: 0, paket: null });
    expect(red.querySelector('.imena')!.textContent).toBe('Ana Horvat');
    expect(red.querySelector('.traka')).toBeNull();
    expect(red.querySelector('.novac')).toBeNull();
  });
});
