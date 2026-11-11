import { Component, computed, input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Prognoza, VjencanjeSazetak } from '../core/modeli';
import { opisVremena } from '../core/vrijeme';
import { izIso, danas as danasIso } from '../core/datumi';
import { par } from '../core/imena';
import { StatusOznakaComponent } from './status-oznaka.component';

// Jedan red u popisu i kalendaru
@Component({
  selector: 'app-vjencanje-red',
  imports: [RouterLink, DatePipe, DecimalPipe, StatusOznakaComponent],
  template: `
    <a class="red" [class.otkazano]="v().status === 'OTKAZANO'" [routerLink]="['/vjencanja', v().id]">
      <div class="datum">
        <span class="dan-tjedna">{{ datum() | date: 'EEE' }}</span>
        <span class="dan">{{ datum() | date: 'd' }}</span>
      </div>
      <div class="glavno">
        <div class="imena">{{ imena() }}</div>
        <div class="lokacija">{{ podnaslov() }}</div>
        @if (imaCijenu()) {
          <div class="traka" [class.kasni]="proslo() && v().preostalo > 0">
            <div class="ispuna" [style.width.%]="postotak()"></div>
          </div>
        }
      </div>
      <div class="desno">
        <app-status [status]="v().status" />
        @if (imaCijenu()) {
          <!-- Crveno tek kad je vjenčanje prošlo; prije toga je neplaćeni ostatak normalan -->
          <span class="novac" [class.duguje]="proslo() && v().preostalo > 0" [class.placeno]="v().preostalo <= 0">
            @if (v().preostalo <= 0) {
              plaćeno
            } @else {
              {{ v().uplaceno | number: '1.0-0' }} / {{ v().dogovorenaCijena | number: '1.0-0' }} €
            }
          </span>
        }
        @if (prognoza(); as p) {
          <span class="vrijeme" [title]="vrijeme(p.kod).opis + ', ' + p.mjesto">{{ vrijeme(p.kod).ikona }} {{ p.max | number: '1.0-0' }}°</span>
        }
      </div>
    </a>
  `,
  styles: `
    .red {
      display: flex; align-items: center; gap: 14px; padding: 12px 16px;
      color: inherit; text-decoration: none;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    :host:last-child .red { border-bottom: 0; }
    .otkazano { opacity: .55; }
    .red:hover { background: var(--mat-sys-surface-container-low); }
    .datum {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      width: 46px; height: 50px; flex-shrink: 0; border-radius: 12px;
      background: var(--mat-sys-surface-container);
    }
    .dan { font-size: 20px; font-weight: 650; line-height: 1.1; font-variant-numeric: tabular-nums; }
    .dan-tjedna { font-size: 11px; color: var(--mat-sys-on-surface-variant); text-transform: uppercase; letter-spacing: .04em; }
    .glavno { flex: 1; min-width: 0; }
    .imena { font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .lokacija {
      font-size: 13px; color: var(--mat-sys-on-surface-variant); min-height: 18px;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .traka { height: 4px; border-radius: 2px; background: var(--mat-sys-surface-container-high); overflow: hidden; margin-top: 7px; max-width: 220px; }
    .ispuna { height: 100%; border-radius: 2px; background: var(--wm-uspjeh); }
    .traka.kasni .ispuna { background: var(--mat-sys-error); }
    .desno { display: flex; flex-direction: column; align-items: flex-end; gap: 5px; font-size: 12px; flex-shrink: 0; }
    .novac { color: var(--mat-sys-on-surface-variant); font-variant-numeric: tabular-nums; white-space: nowrap; }
    .duguje { color: var(--mat-sys-error); }
    .placeno { color: var(--wm-uspjeh); }
    .vrijeme { font-size: 11px; white-space: nowrap; color: var(--mat-sys-on-surface-variant); }
  `,
})
export class VjencanjeRedComponent {
  prognoza = input<Prognoza | undefined>(undefined);
  readonly vrijeme = opisVremena;
  v = input.required<VjencanjeSazetak>();
  datum = () => izIso(this.v().datum);
  proslo = () => this.v().datum < danasIso();
  imena = computed(() => par(this.v().mlada, this.v().mladozenja));
  podnaslov = computed(() => [this.v().lokacija, this.v().paket].filter(Boolean).join(' · '));
  // Upit bez cijene i otkazano vjenčanje nemaju što pokazati o novcu
  imaCijenu = computed(() => this.v().dogovorenaCijena > 0 && this.v().status !== 'OTKAZANO');
  postotak = computed(() => Math.min(100, Math.round((this.v().uplaceno / this.v().dogovorenaCijena) * 100)));
}
