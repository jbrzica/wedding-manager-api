import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ApiService } from '../core/api.service';
import { Statistika } from '../core/modeli';
import { IZVORI_UPITA } from '../core/oznake';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

const MJESECI = ['Sij', 'Velj', 'Ožu', 'Tra', 'Svi', 'Lip', 'Srp', 'Kol', 'Ruj', 'Lis', 'Stu', 'Pro'];

@Component({
  selector: 'app-statistika',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './statistika.component.html',
  styleUrl: './statistika.component.scss',
})
export class StatistikaComponent implements OnInit {
  private api = inject(ApiService);

  godina = signal(new Date().getFullYear());
  s = signal<Statistika | null>(null);
  ucitava = signal(true);
  greska = signal('');

  readonly mjeseci = MJESECI;
  readonly oznakeIzvora = IZVORI_UPITA;
  readonly izIso = izIso;

  // Najveći mjesec određuje visinu grafa
  najviseMjesec = computed(() => Math.max(1,
    ...(this.s()?.poMjesecima.map((m) => m.dogovoreno) ?? [0]),
    ...(this.s()?.proslaGodinaPoMjesecima ?? [0])));
  najviseIzvor = computed(() => Math.max(1, ...(this.s()?.izvori.map((i) => i.upita) ?? [0])));
  slobodnihSubota = computed(() => this.s()?.subote.filter((x) => x.dogovoreno === 0).length ?? 0);

  ngOnInit(): void {
    this.ucitaj();
  }

  pomakni(za: number): void {
    this.godina.update((g) => g + za);
    this.ucitaj();
  }

  // Postotak promjene prema prošloj godini; null kad prošle godine nije bilo ničega
  promjena(ova: number, prosla: number): number | null {
    return prosla > 0 ? Math.round(((ova - prosla) / prosla) * 100) : null;
  }

  visina(iznos: number): number {
    return (iznos / this.najviseMjesec()) * 100;
  }

  private ucitaj(): void {
    this.ucitava.set(true);
    this.greska.set('');
    this.api.statistika(this.godina()).subscribe({
      next: (s) => {
        this.s.set(s);
        this.ucitava.set(false);
      },
      error: (err) => {
        this.greska.set(porukaGreske(err));
        this.ucitava.set(false);
      },
    });
  }
}
