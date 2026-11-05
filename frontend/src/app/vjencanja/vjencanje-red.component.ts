import { Component, input } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { VjencanjeSazetak } from '../core/modeli';
import { izIso } from '../core/datumi';
import { StatusOznakaComponent } from './status-oznaka.component';

// Jedan red u popisu i kalendaru
@Component({
  selector: 'app-vjencanje-red',
  imports: [RouterLink, DatePipe, CurrencyPipe, MatIconModule, StatusOznakaComponent],
  template: `
    <a class="red" [routerLink]="['/vjencanja', v().id]">
      <div class="datum">
        <span class="dan">{{ datum() | date: 'd' }}</span>
        <span class="dan-tjedna">{{ datum() | date: 'EEE' }}</span>
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
          @if (v().preostalo > 0) {
            <span class="duguje">još {{ v().preostalo | currency: 'EUR' }}</span>
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
    .red {
      display: flex; align-items: center; gap: 16px; padding: 12px 16px;
      color: inherit; text-decoration: none;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
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
    .placeno { color: #1e6b34; }
    .kapara { font-size: 11px; color: var(--mat-sys-on-surface-variant); }
  `,
})
export class VjencanjeRedComponent {
  v = input.required<VjencanjeSazetak>();
  datum = () => izIso(this.v().datum);
}
