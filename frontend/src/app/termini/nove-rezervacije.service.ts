import { Injectable, inject, signal } from '@angular/core';
import { NoveRezervacije, TerminiApi } from './termini-api';

/**
 * Obavijest o novim online rezervacijama (Božićno i sl.). Provjerava se kad se aplikacija otvori,
 * kad se vrati na ekran i svake 2 minute dok je otvorena. "Pročitano" pamti server, po korisniku.
 */
@Injectable({ providedIn: 'root' })
export class NoveRezervacijeService {
  private api = inject(TerminiApi);
  private interval: ReturnType<typeof setInterval> | null = null;
  private readonly naVidljivost = () => document.visibilityState === 'visible' && this.osvjezi();

  stanje = signal<NoveRezervacije>({ broj: 0, rezervacije: [] });

  pokreni(): void {
    if (this.interval) {
      return;
    }
    this.osvjezi();
    this.interval = setInterval(() => document.visibilityState === 'visible' && this.osvjezi(), 120_000);
    document.addEventListener('visibilitychange', this.naVidljivost);
  }

  zaustavi(): void {
    if (this.interval) {
      clearInterval(this.interval);
      this.interval = null;
    }
    document.removeEventListener('visibilitychange', this.naVidljivost);
    this.stanje.set({ broj: 0, rezervacije: [] });
  }

  osvjezi(): void {
    // Bez mreže ili kod greške obavijest jednostavno ostane kakva je
    this.api.nove().subscribe({ next: (s) => this.stanje.set(s), error: () => {} });
  }

  procitano(): void {
    if (!this.stanje().broj) {
      return;
    }
    this.stanje.set({ broj: 0, rezervacije: [] });
    this.api.procitano().subscribe({ error: () => {} });
  }
}
