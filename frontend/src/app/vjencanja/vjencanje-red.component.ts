import { Component, input } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { Prognoza, VjencanjeSazetak } from '../core/modeli';
import { opisVremena } from '../core/vrijeme';
import { izIso, danas as danasIso } from '../core/datumi';
import { StatusOznakaComponent } from './status-oznaka.component';

// Jedan red u popisu i kalendaru
@Component({
  selector: 'app-vjencanje-red',
  imports: [RouterLink, DatePipe, CurrencyPipe, DecimalPipe, MatIconModule, StatusOznakaComponent],
  template: `
    <a class="red" [class.otkazano]="v().status === 'OTKAZANO'" [routerLink]="['/vjencanja', v().id]">
      <div class="datum">
        <span class="dan">{{ datum() | date: 'd' }}</span>
        <span class="dan-tjedna">{{ datum() | date: 'EEE' }}</span>
        @if (prognoza(); as p) {
          <span class="vrijeme" [title]="vrijeme(p.kod).opis + ', ' + p.mjesto">{{ vrijeme(p.kod).ikona }} {{ p.max | number: '1.0-0' }}°</span>
        }
      </div>
      <div class="glavno">
        <div class="imena">{{ v().mlada }} &amp; {{ v().mladozenja }}</div>
        <div class="lokacija">
          @if (v().lokacija) {
            <mat-icon inline>place</mat-icon> {{ v().lokacija }}
          }
        </div>
      </div>
      <div class="desno">
        <app-status [status]="v().status" />
        <div class="novac">
          @if (v().status === 'OTKAZANO') {
          } @else if (v().preostalo > 0) {
            <!-- Crveno tek kad je vjenčanje prošlo; prije toga je neplaćeni ostatak normalan -->
            <span [class.duguje]="proslo()" [class.ceka]="!proslo()">još {{ v().preostalo | currency: 'EUR' }}</span>
          } @else {
            <span class="placeno">plaćeno</span>
          }
        </div>
        @if (v().kaparaPlacena) {
          <div class="kapara">kapara plaćena</div>
        }
      </div>
    </a>
  `,
  styles: `
    .vrijeme { font-size: 11px; margin-top: 2px; white-space: nowrap; color: var(--mat-sys-on-surface-variant); }
    .red {
      display: flex; align-items: center; gap: 16px; padding: 12px 16px;
      color: inherit; text-decoration: none;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .otkazano { opacity: .55; }
    .red:hover { background: var(--mat-sys-surface-container-low); }
    .datum {
      display: flex; flex-direction: column; align-items: center; width: 44px; flex-shrink: 0;
    }
    .dan { font-size: 22px; font-weight: 500; line-height: 1; }
    .dan-tjedna { font-size: 12px; color: var(--mat-sys-on-surface-variant); text-transform: uppercase; }
    .glavno { flex: 1; min-width: 0; }
    .imena { font-weight: 500; }
    .lokacija {
      font-size: 13px; color: var(--mat-sys-on-surface-variant);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .desno { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; font-size: 13px; }
    .duguje { color: var(--mat-sys-error); }
    .ceka { color: var(--mat-sys-on-surface-variant); }
    .placeno { color: var(--wm-uspjeh); }
    .kapara { font-size: 11px; color: var(--mat-sys-on-surface-variant); }
  `,
})
export class VjencanjeRedComponent {
  prognoza = input<Prognoza | undefined>(undefined);
  readonly vrijeme = opisVremena;
  v = input.required<VjencanjeSazetak>();
  datum = () => izIso(this.v().datum);
  proslo = () => this.v().datum < danasIso();
}
