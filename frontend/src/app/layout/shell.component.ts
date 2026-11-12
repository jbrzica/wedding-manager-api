import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../core/auth.service';
import { inicijali } from '../core/imena';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe, MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <mat-toolbar class="traka">
      <a routerLink="/" class="logo">
        <span class="tvrtka">{{ auth.korisnik()?.nazivTvrtke || 'Wedding Manager' }}</span>
        <span class="danas">{{ danas | date: 'EEEE, d. MMMM' }}</span>
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

      <button mat-flat-button class="novo-desktop" [matMenuTriggerFor]="novo">
        <mat-icon>add</mat-icon> Novo
      </button>
      <button type="button" class="avatar" [matMenuTriggerFor]="izbornik" aria-label="Račun">{{ inicijali() }}</button>
    </mat-toolbar>

    <mat-menu #novo="matMenu">
      <button mat-menu-item (click)="brziUpit()">
        <mat-icon>bolt</mat-icon> Brzi upit
      </button>
      <a mat-menu-item routerLink="/vjencanja/novo">
        <mat-icon>favorite</mat-icon> Novo vjenčanje
      </a>
      <button mat-menu-item (click)="provjeriDatum()">
        <mat-icon>event_available</mat-icon> Provjeri datum
      </button>
    </mat-menu>

    <mat-menu #izbornik="matMenu">
      <div class="korisnik-info">
        <strong>{{ auth.korisnik()?.ime }}</strong>
        <small>{{ auth.korisnik()?.nazivTvrtke }}</small>
        <small>{{ auth.korisnik()?.email }}</small>
      </div>
      <a mat-menu-item routerLink="/postavke">
        <mat-icon>settings</mat-icon> Postavke
      </a>
      <a mat-menu-item routerLink="/kos">
        <mat-icon>delete</mat-icon> Koš
      </a>
      <a mat-menu-item routerLink="/pomoc">
        <mat-icon>help</mat-icon> Pomoć
      </a>
      <button mat-menu-item (click)="auth.odjava()">
        <mat-icon>logout</mat-icon> Odjava
      </button>
    </mat-menu>

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
      <div class="sredina">
        <button type="button" class="plus" [matMenuTriggerFor]="novo" aria-label="Novo"><mat-icon>add</mat-icon></button>
      </div>
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
      background: var(--wm-traka); backdrop-filter: blur(8px);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      padding-left: max(16px, env(safe-area-inset-left));
      padding-right: max(16px, env(safe-area-inset-right));
    }
    .logo { display: flex; flex-direction: column; color: inherit; text-decoration: none; margin-right: 24px; min-width: 0; }
    .tvrtka { font-size: 17px; font-weight: 650; letter-spacing: -0.01em; line-height: 1.2; overflow: hidden; text-overflow: ellipsis; }
    .danas { font-size: 12px; font-weight: 400; line-height: 1.2; color: var(--mat-sys-on-surface-variant); }
    .nav-desktop a { color: var(--mat-sys-on-surface-variant); }
    .nav-desktop a.aktivno { color: var(--mat-sys-on-surface); }
    .razmak { flex: 1; }
    .aktivno { background: var(--mat-sys-secondary-container); }
    .avatar {
      width: 36px; height: 36px; border-radius: 50%; border: 0; margin-left: 12px; flex-shrink: 0; cursor: pointer;
      font: inherit; font-size: 13px; font-weight: 650;
      background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container);
    }
    .korisnik-info { display: flex; flex-direction: column; padding: 8px 16px 12px; }
    .korisnik-info small { color: var(--mat-sys-on-surface-variant); }
    .sadrzaj {
      max-width: 1100px; margin: 0 auto;
      padding: 24px max(16px, env(safe-area-inset-right)) 96px max(16px, env(safe-area-inset-left));
    }
    .nav-mobitel { display: none; }

    @media (max-width: 720px) {
      .nav-desktop, .novo-desktop { display: none; }
      .sadrzaj { padding-top: 16px; padding-bottom: calc(104px + env(safe-area-inset-bottom)); }
      .nav-mobitel {
        display: flex; position: fixed; bottom: 0; left: 0; right: 0; z-index: 10;
        background: var(--wm-traka); backdrop-filter: blur(8px);
        border-top: 1px solid var(--mat-sys-outline-variant);
        /* Iznad crtice za povratak na početni zaslon i dalje od zaobljenih kutova. Cijeli razmak
           od iPhonea (34 pt) izgleda kao prazan prozor; tekst smije ići do same crtice, ne preko nje. */
        padding: 0 max(8px, env(safe-area-inset-right)) max(4px, calc(env(safe-area-inset-bottom) - 16px)) max(8px, env(safe-area-inset-left));
      }
      .nav-mobitel > a {
        flex: 1; display: flex; flex-direction: column; align-items: center;
        padding: 8px 0 6px; font-size: 12px; text-decoration: none;
        color: var(--mat-sys-on-surface-variant);
      }
      .nav-mobitel > a.aktivno { color: var(--mat-sys-primary); background: none; }
      .sredina { flex: 1; display: flex; justify-content: center; }
      /* Najčešća radnja: veći gumb koji viri iznad trake, da ga palac nađe bez gledanja */
      .plus {
        width: 54px; height: 54px; margin-top: -16px; border-radius: 50%; border: 0; cursor: pointer;
        display: flex; align-items: center; justify-content: center;
        background: var(--mat-sys-primary); color: var(--mat-sys-on-primary);
        box-shadow: 0 4px 12px rgba(0, 0, 0, .22);
      }
      .plus mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }
  `,
})
export class ShellComponent {
  auth = inject(AuthService);
  private dialog = inject(MatDialog);

  readonly danas = new Date();
  inicijali = computed(() => inicijali(this.auth.korisnik()?.ime) || '?');

  // Prozori se učitaju tek na dodir, da ne usporavaju prvo otvaranje aplikacije
  async brziUpit(): Promise<void> {
    const { BrziUpitComponent } = await import('../vjencanja/brzi-upit.component');
    this.dialog.open(BrziUpitComponent, { width: '480px', maxWidth: '100vw', autoFocus: false, panelClass: 'donji-list' });
  }

  async provjeriDatum(): Promise<void> {
    const { ProvjeraDatumaComponent } = await import('../vjencanja/provjera-datuma.component');
    this.dialog.open(ProvjeraDatumaComponent, { width: '480px', maxWidth: '95vw', autoFocus: false });
  }
}
