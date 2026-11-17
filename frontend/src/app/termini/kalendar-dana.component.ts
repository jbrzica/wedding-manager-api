import { Component, computed, effect, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { izIso, uIso } from '../core/datumi';

export interface DanUKalendaru {
  datum: string;
  zauzeto: number;
  ukupno: number;
}

const DANI_U_TJEDNU = ['Pon', 'Uto', 'Sri', 'Čet', 'Pet', 'Sub', 'Ned'];

// Mjesečni kalendar za odabir dana fotografiranja; ispod broja je koliko je termina zauzeto
@Component({
  selector: 'app-kalendar-dana',
  imports: [DatePipe, MatIconModule],
  template: `
    <div class="kal">
      <div class="vrh">
        <button type="button" class="strelica" [disabled]="indeks() === 0" (click)="pomakni(-1)" aria-label="Prethodni mjesec">
          <mat-icon>chevron_left</mat-icon></button>
        <b>{{ mjeseci()[indeks()] | date: 'LLLL yyyy' }}</b>
        <button type="button" class="strelica" [disabled]="indeks() >= mjeseci().length - 1" (click)="pomakni(1)" aria-label="Sljedeći mjesec">
          <mat-icon>chevron_right</mat-icon></button>
      </div>
      <div class="mreza">
        @for (t of daniUTjednu; track t) { <span class="t">{{ t }}</span> }
        @for (p of polja(); track $index) {
          @if (p) {
            <button type="button" class="d" [class.ima]="p.dan" [class.pun]="p.dan && p.dan.zauzeto === p.dan.ukupno"
                    [class.danas]="p.iso === danas" [class.on]="odabran() === p.iso" [disabled]="!p.dan" (click)="odaberi.emit(p.iso)">
              <span class="br">{{ p.broj }}</span>
              @if (p.dan) { <span class="sl">{{ p.dan.zauzeto }}/{{ p.dan.ukupno }}</span> }
            </button>
          } @else {
            <span></span>
          }
        }
      </div>
    </div>
  `,
  styles: `
    .kal { background: var(--mat-sys-surface); border: 1px solid var(--mat-sys-outline-variant); border-radius: 18px; padding: 10px 10px 12px; }
    .vrh { display: flex; align-items: center; justify-content: space-between; padding: 0 2px 8px; font-size: 15px; }
    .vrh b::first-letter { text-transform: uppercase; }
    .strelica { width: 36px; height: 36px; border-radius: 50%; border: 0; background: var(--mat-sys-surface-container); color: inherit;
                cursor: pointer; display: grid; place-items: center; }
    .strelica:disabled { opacity: .3; cursor: default; }
    .mreza { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 4px; }
    .t { text-align: center; font-size: 11.5px; font-weight: 700; color: var(--mat-sys-on-surface-variant); padding-bottom: 4px; }
    .d { min-width: 0; aspect-ratio: 1 / 1; max-height: 56px; border-radius: 12px; border: 1.5px solid transparent; background: none; font: inherit;
         color: var(--mat-sys-outline); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1px;
         padding: 0; cursor: default; }
    .br { font-size: 15px; font-weight: 600; line-height: 1.1; }
    .sl { font-size: 10px; line-height: 1; white-space: nowrap; overflow: hidden; max-width: 100%; }
    .d.ima { border-color: var(--mat-sys-outline-variant); color: var(--mat-sys-on-surface); cursor: pointer; }
    .d.ima .sl { color: var(--mat-sys-primary); font-weight: 600; }
    .d.pun { background: var(--mat-sys-surface-container); }
    .d.danas .br { text-decoration: underline; text-underline-offset: 3px; }
    .d.on { background: var(--mat-sys-primary); border-color: var(--mat-sys-primary); color: var(--mat-sys-on-primary); }
    .d.on .sl { color: var(--mat-sys-on-primary); }
  `,
})
export class KalendarDanaComponent {
  dani = input.required<DanUKalendaru[]>();
  odabran = input<string | null>(null);
  odaberi = output<string>();

  readonly daniUTjednu = DANI_U_TJEDNU;
  readonly danas = uIso(new Date());
  indeks = signal(0);

  mjeseci = computed(() => [...new Set(this.dani().map((d) => d.datum.slice(0, 7)))].sort().map((k) => izIso(`${k}-01`)));

  polja = computed(() => {
    const prvi = this.mjeseci()[this.indeks()];
    if (!prvi) {
      return [];
    }
    const poDatumu = new Map(this.dani().map((d) => [d.datum, d]));
    const polja: ({ iso: string; broj: number; dan: DanUKalendaru | undefined } | null)[] = Array((prvi.getDay() + 6) % 7).fill(null);
    const brojDana = new Date(prvi.getFullYear(), prvi.getMonth() + 1, 0).getDate();
    for (let i = 1; i <= brojDana; i++) {
      const iso = uIso(new Date(prvi.getFullYear(), prvi.getMonth(), i));
      polja.push({ iso, broj: i, dan: poDatumu.get(iso) });
    }
    return polja;
  });

  constructor() {
    // Kalendar se otvori na mjesecu odabranog dana
    effect(() => {
      const o = this.odabran();
      const i = o ? this.mjeseci().findIndex((m) => uIso(m).slice(0, 7) === o.slice(0, 7)) : -1;
      if (i >= 0) {
        this.indeks.set(i);
      }
    });
  }

  pomakni(smjer: number): void {
    this.indeks.update((i) => Math.min(Math.max(i + smjer, 0), this.mjeseci().length - 1));
  }
}
