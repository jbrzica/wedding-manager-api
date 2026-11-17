import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { NovaRezervacija } from './termini-api';
import { NoveRezervacijeService } from './nove-rezervacije.service';
import { izIso } from '../core/datumi';

// Vjenčanja i termini za kratka fotografiranja: dva odvojena popisa, jedan dodir između njih.
// Ispod je obavijest o novim online rezervacijama; dodir vodi ravno na taj termin.
@Component({
  selector: 'app-prekidac',
  imports: [DatePipe, RouterLink, RouterLinkActive, MatIconModule, MatButtonModule],
  template: `
    <nav class="prekidac">
      <a routerLink="/" routerLinkActive="on" [routerLinkActiveOptions]="{ exact: true }">Vjenčanja</a>
      <a routerLink="/termini" routerLinkActive="on">Termini
        @if (nove.stanje().broj; as broj) { <span class="znacka" [attr.aria-label]="broj + ' novih'">{{ broj }}</span> }
      </a>
    </nav>

    @if (nove.stanje().broj; as broj) {
      <section class="obavijest" aria-live="polite">
        <div class="naslov">
          <mat-icon>notifications_active</mat-icon>
          <strong>{{ broj === 1 ? 'Nova rezervacija' : broj + (broj < 5 ? ' nove rezervacije' : ' novih rezervacija') }}</strong>
          <button mat-icon-button type="button" (click)="nove.procitano()" aria-label="Označi kao pročitano"><mat-icon>close</mat-icon></button>
        </div>
        @for (r of nove.stanje().rezervacije.slice(0, 3); track r.id) {
          <button type="button" class="stavka" (click)="otvori(r)">
            <span class="kad">{{ izIso(r.datum) | date: 'EEE d. M.' }} u {{ r.vrijeme.slice(0, 5) }}</span>
            <span class="tko">{{ r.ime }} · {{ r.nazivPaketa }}</span>
            <mat-icon>chevron_right</mat-icon>
          </button>
        }
        @if (broj > 3) {
          <button type="button" class="jos" (click)="otvori(nove.stanje().rezervacije[0])">i još {{ broj - 3 }} u Terminima</button>
        }
      </section>
    }
  `,
  styles: `
    .prekidac { display: flex; gap: 4px; padding: 4px; margin-bottom: 14px; border-radius: 12px; background: var(--mat-sys-surface-container); }
    a { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 8px 4px; border-radius: 9px;
        font-size: 14px; text-decoration: none; color: var(--mat-sys-on-surface-variant); }
    a.on { background: var(--mat-sys-surface); color: var(--mat-sys-on-surface); font-weight: 600; box-shadow: 0 1px 3px rgba(0, 0, 0, .12); }
    .znacka { min-width: 18px; height: 18px; padding: 0 5px; box-sizing: border-box; border-radius: 9px; background: #c0392b; color: #fff;
              font-size: 11.5px; font-weight: 700; line-height: 18px; text-align: center; }
    .obavijest { border: 1px solid var(--mat-sys-outline-variant); border-left: 4px solid #c0392b; border-radius: 14px;
                 background: var(--mat-sys-surface); padding: 4px 4px 6px 12px; margin: -4px 0 14px; }
    .naslov { display: flex; align-items: center; gap: 8px; font-size: 14.5px; }
    .naslov mat-icon { color: #c0392b; font-size: 20px; width: 20px; height: 20px; }
    .naslov strong { flex: 1; }
    .stavka { width: 100%; display: flex; align-items: center; gap: 10px; border: 0; border-top: 1px solid var(--mat-sys-outline-variant);
              background: none; font: inherit; color: inherit; text-align: left; padding: 9px 4px 9px 0; cursor: pointer; }
    .kad { font-weight: 700; font-size: 13.5px; white-space: nowrap; }
    .kad::first-letter { text-transform: uppercase; }
    .tko { flex: 1; min-width: 0; font-size: 13.5px; color: var(--mat-sys-on-surface-variant); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .stavka mat-icon { color: var(--mat-sys-on-surface-variant); }
    .jos { border: 0; background: none; font: inherit; font-size: 13px; color: var(--mat-sys-primary); padding: 6px 0 2px; cursor: pointer; }
  `,
})
export class PrekidacComponent {
  nove = inject(NoveRezervacijeService);
  private router = inject(Router);
  readonly izIso = izIso;

  otvori(r: NovaRezervacija): void {
    this.router.navigate(['/termini', r.dogadjajId], { queryParams: { dan: r.datum, rezervacija: r.id } });
  }
}
