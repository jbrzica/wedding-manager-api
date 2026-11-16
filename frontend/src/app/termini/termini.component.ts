import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DogadjajSazetak, TerminiApi } from './termini-api';
import { PrekidacComponent } from './prekidac.component';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

// Popis fotografiranja s terminima (Božićno, Uskrsno...). Ne miješa se s vjenčanjima.
@Component({
  selector: 'app-termini',
  imports: [CurrencyPipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, PrekidacComponent],
  template: `
    <app-prekidac />
    @if (ucitava()) { <mat-progress-bar mode="indeterminate" /> }
    @if (greska()) { <p class="greska">{{ greska() }}</p> }

    @for (d of dogadjaji(); track d.id) {
      <a class="dogadjaj" [routerLink]="['/termini', d.id]">
        <div class="vrh">
          <span>{{ raspon(d) }}</span>
          <span class="oznaka" [class.zatvoren]="!d.otvoren">{{ d.otvoren ? 'Rezervacije otvorene' : 'Zatvoreno' }}</span>
        </div>
        <div class="naziv">{{ d.naziv }}</div>
        <div class="traka"><div [style.width.%]="d.ukupnoTermina ? (d.zauzeto / d.ukupnoTermina) * 100 : 0"></div></div>
        <div class="dno">
          <span><strong>{{ d.zauzeto }}</strong> od {{ d.ukupnoTermina }} termina</span>
          <span>{{ d.prihod | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
        </div>
      </a>
    } @empty {
      @if (!ucitava() && !greska()) {
        <div class="prazno">
          <mat-icon>event_available</mat-icon>
          <h2>Termini za kratka fotografiranja</h2>
          <p>Npr. Božićno fotografiranje: upišeš dane, sate i pakete, a klijenti preko linka sami rezerviraju slobodan termin.
             Ovdje vidiš tko dolazi i kada. Ne miješa se s vjenčanjima.</p>
          <a mat-flat-button routerLink="/termini/novi"><mat-icon>add</mat-icon> Novo fotografiranje</a>
        </div>
      }
    }

    @if (dogadjaji().length) {
      <a mat-stroked-button routerLink="/termini/novi" class="novo"><mat-icon>add</mat-icon> Novo fotografiranje</a>
    }
  `,
  styles: `
    .dogadjaj { display: block; padding: 16px 18px; margin-bottom: 12px; border-radius: 20px; text-decoration: none;
                color: #fff; background: linear-gradient(140deg, #4f5b4e, #6b7a69); }
    .vrh { display: flex; justify-content: space-between; gap: 8px; font-size: 12px; letter-spacing: .06em; text-transform: uppercase; opacity: .9; }
    .oznaka { padding: 2px 8px; border-radius: 999px; background: rgba(255, 255, 255, .18); letter-spacing: 0; text-transform: none; }
    .oznaka.zatvoren { background: rgba(0, 0, 0, .25); }
    .naziv { font-size: 21px; font-weight: 700; margin: 6px 0 10px; }
    .traka { height: 6px; border-radius: 6px; background: rgba(255, 255, 255, .25); overflow: hidden; }
    .traka div { height: 100%; background: #fff; border-radius: 6px; }
    .dno { display: flex; justify-content: space-between; margin-top: 8px; font-size: 14px; }
    .prazno { text-align: center; padding: 32px 16px; color: var(--mat-sys-on-surface-variant); }
    .prazno mat-icon { font-size: 44px; width: 44px; height: 44px; }
    .prazno h2 { color: var(--mat-sys-on-surface); font-size: 18px; margin: 8px 0; }
    .prazno p { max-width: 420px; margin: 0 auto 16px; line-height: 1.5; }
    .novo { margin-top: 4px; }
    .greska { color: var(--mat-sys-error); }
  `,
})
export class TerminiComponent implements OnInit {
  private api = inject(TerminiApi);
  dogadjaji = signal<DogadjajSazetak[]>([]);
  ucitava = signal(true);
  greska = signal('');

  ngOnInit(): void {
    this.api.popis().subscribe({
      next: (d) => { this.dogadjaji.set(d); this.ucitava.set(false); },
      error: (err) => { this.greska.set(porukaGreske(err)); this.ucitava.set(false); },
    });
  }

  raspon(d: DogadjajSazetak): string {
    if (!d.prviDan || !d.zadnjiDan) {
      return 'Bez dana';
    }
    const f = (iso: string) => { const x = izIso(iso); return `${x.getDate()}. ${x.getMonth() + 1}.`; };
    return d.prviDan === d.zadnjiDan ? f(d.prviDan) : `${f(d.prviDan)} – ${f(d.zadnjiDan)}`;
  }
}
