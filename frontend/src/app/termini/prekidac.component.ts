import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

// Vjenčanja i termini za kratka fotografiranja: dva odvojena popisa, jedan dodir između njih
@Component({
  selector: 'app-prekidac',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="prekidac">
      <a routerLink="/" routerLinkActive="on" [routerLinkActiveOptions]="{ exact: true }">Vjenčanja</a>
      <a routerLink="/termini" routerLinkActive="on">Termini</a>
    </nav>
  `,
  styles: `
    .prekidac { display: flex; gap: 4px; padding: 4px; margin-bottom: 14px; border-radius: 12px; background: var(--mat-sys-surface-container); }
    a { flex: 1; text-align: center; padding: 8px 4px; border-radius: 9px; font-size: 14px; text-decoration: none;
        color: var(--mat-sys-on-surface-variant); }
    a.on { background: var(--mat-sys-surface); color: var(--mat-sys-on-surface); font-weight: 600; box-shadow: 0 1px 3px rgba(0, 0, 0, .12); }
  `,
})
export class PrekidacComponent {}
