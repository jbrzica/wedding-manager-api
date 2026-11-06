import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <mat-toolbar class="traka">
      <a routerLink="/" class="logo">
        <span>Wedding Manager</span>
      </a>

      <nav class="nav-desktop">
        <a mat-button routerLink="/" routerLinkActive="aktivno" [routerLinkActiveOptions]="{ exact: true }">
          <mat-icon>list</mat-icon> Vjenčanja
        </a>
        <a mat-button routerLink="/kalendar" routerLinkActive="aktivno">
          <mat-icon>calendar_month</mat-icon> Kalendar
        </a>
        <a mat-button routerLink="/statistika" routerLinkActive="aktivno">
          <mat-icon>bar_chart</mat-icon> Statistika
        </a>
        <a mat-button routerLink="/paketi" routerLinkActive="aktivno">
          <mat-icon>inventory_2</mat-icon> Paketi i ekipa
        </a>
      </nav>

      <span class="razmak"></span>

      <button mat-icon-button [matMenuTriggerFor]="izbornik" aria-label="Račun">
        <mat-icon>account_circle</mat-icon>
      </button>
      <mat-menu #izbornik="matMenu">
        <div class="korisnik-info">
          <strong>{{ auth.korisnik()?.ime }}</strong>
          <small>{{ auth.korisnik()?.nazivTvrtke }}</small>
          <small>{{ auth.korisnik()?.email }}</small>
        </div>
        <a mat-menu-item routerLink="/postavke">
          <mat-icon>settings</mat-icon> Postavke
        </a>
        <button mat-menu-item (click)="auth.odjava()">
          <mat-icon>logout</mat-icon> Odjava
        </button>
      </mat-menu>
    </mat-toolbar>

    <main class="sadrzaj">
      <router-outlet />
    </main>

    <!-- Na mobitelu navigacija ide na dno ekrana, kao u pravim aplikacijama -->
    <nav class="nav-mobitel">
      <a routerLink="/" routerLinkActive="aktivno" [routerLinkActiveOptions]="{ exact: true }">
        <mat-icon>list</mat-icon><span>Vjenčanja</span>
      </a>
      <a routerLink="/kalendar" routerLinkActive="aktivno">
        <mat-icon>calendar_month</mat-icon><span>Kalendar</span>
      </a>
      <a routerLink="/vjencanja/novo" routerLinkActive="aktivno">
        <mat-icon>add_circle</mat-icon><span>Novo</span>
      </a>
      <a routerLink="/statistika" routerLinkActive="aktivno">
        <mat-icon>bar_chart</mat-icon><span>Statistika</span>
      </a>
      <a routerLink="/paketi" routerLinkActive="aktivno">
        <mat-icon>inventory_2</mat-icon><span>Paketi</span>
      </a>
    </nav>
  `,
  styles: `
    :host { display: block; min-height: 100vh; }
    .traka {
      position: sticky; top: 0; z-index: 10;
      background: rgba(255, 255, 255, .92); backdrop-filter: blur(8px);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .logo span { font-size: 17px; font-weight: 650; letter-spacing: -0.01em; }
    .nav-desktop a { color: var(--mat-sys-on-surface-variant); }
    .nav-desktop a.aktivno { color: var(--mat-sys-on-surface); }
    .logo {
      display: flex; align-items: center; gap: 8px;
      color: inherit; text-decoration: none; font-weight: 500; margin-right: 24px;
      mat-icon { color: var(--mat-sys-primary); }
    }
    .razmak { flex: 1; }
    .aktivno { background: var(--mat-sys-secondary-container); }
    .korisnik-info { display: flex; flex-direction: column; padding: 8px 16px 12px; }
    .korisnik-info small { color: var(--mat-sys-on-surface-variant); }
    .sadrzaj { max-width: 1100px; margin: 0 auto; padding: 24px 16px 96px; }
    .nav-mobitel { display: none; }

    @media (max-width: 720px) {
      .nav-desktop { display: none; }
      .sadrzaj { padding-top: 16px; }
      .nav-mobitel {
        display: flex; position: fixed; bottom: 0; left: 0; right: 0; z-index: 10;
        background: rgba(255, 255, 255, .96); backdrop-filter: blur(8px);
        border-top: 1px solid var(--mat-sys-outline-variant);
        padding-bottom: env(safe-area-inset-bottom);
      }
      .nav-mobitel a {
        flex: 1; display: flex; flex-direction: column; align-items: center;
        padding: 8px 0; font-size: 12px; text-decoration: none;
        color: var(--mat-sys-on-surface-variant);
      }
      .nav-mobitel a.aktivno { color: var(--mat-sys-primary); background: none; }
    }
  `,
})
export class ShellComponent {
  auth = inject(AuthService);
}
