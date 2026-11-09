import { Injectable, signal } from '@angular/core';

export type Tema = 'sustav' | 'svijetlo' | 'tamno';

const KLJUC = 'wm-tema';
// Moraju odgovarati pozadini (--mat-sys-surface) u styles.scss, da traka mobitela ne odskače
const BOJA_TRAKE = { svijetlo: '#fdfcfa', tamno: '#141311' };

function procitaj(): Tema {
  try {
    const t = localStorage.getItem(KLJUC);
    return t === 'svijetlo' || t === 'tamno' ? t : 'sustav';
  } catch {
    return 'sustav';
  }
}

/**
 * Svijetli ili tamni izgled. Izbor se pamti samo na ovom uređaju, jer netko može
 * htjeti tamno na mobitelu, a svijetlo na računalu.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly sustavTamno = window.matchMedia('(prefers-color-scheme: dark)');
  readonly tema = signal<Tema>(procitaj());

  constructor() {
    // Kad se mobitel sam prebaci u noćni način, i aplikacija prati
    this.sustavTamno.addEventListener('change', () => this.primijeni());
    this.primijeni();
  }

  postavi(tema: Tema): void {
    this.tema.set(tema);
    try {
      if (tema === 'sustav') {
        localStorage.removeItem(KLJUC);
      } else {
        localStorage.setItem(KLJUC, tema);
      }
    } catch {
      // bez pohrane izbor vrijedi do zatvaranja aplikacije
    }
    this.primijeni();
  }

  private primijeni(): void {
    const tema = this.tema();
    const html = document.documentElement;
    html.classList.remove('svijetlo', 'tamno');
    if (tema !== 'sustav') {
      html.classList.add(tema);
    }
    const tamno = tema === 'tamno' || (tema === 'sustav' && this.sustavTamno.matches);
    document.querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', tamno ? BOJA_TRAKE.tamno : BOJA_TRAKE.svijetlo);
  }
}
