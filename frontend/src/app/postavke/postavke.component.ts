import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../core/auth.service';
import { TvrtkaComponent } from './tvrtka.component';
import { LozinkaComponent } from './lozinka.component';
import { KalendarLinkComponent } from './kalendar.component';
import { IzgledComponent } from './izgled.component';
import { UgovorPredlozakComponent } from './ugovor-predlozak.component';

// Tri grupe umjesto jednog dugog niza kartica: što se tiče tvrtke, što aplikacije na ovom uređaju, što računa
@Component({
  selector: 'app-postavke',
  imports: [RouterLink, MatButtonModule, MatIconModule, TvrtkaComponent, UgovorPredlozakComponent, IzgledComponent,
    KalendarLinkComponent, LozinkaComponent],
  template: `
    <h1>Postavke</h1>
    <nav class="skok" aria-label="Dijelovi postavki">
      <a href="/postavke#tvrtka" (click)="idiNa($event, 'tvrtka')">Tvrtka</a>
      <a href="/postavke#aplikacija" (click)="idiNa($event, 'aplikacija')">Aplikacija</a>
      <a href="/postavke#racun" (click)="idiNa($event, 'racun')">Račun</a>
    </nav>

    <section id="tvrtka">
      <h2>Tvrtka</h2>
      <p class="sivo">Podaci za ugovor, članovi i predložak ugovora.</p>
      <app-tvrtka />
      <app-ugovor-predlozak />
    </section>

    <section id="aplikacija">
      <h2>Aplikacija</h2>
      <p class="sivo">Izgled i kalendar na mobitelu.</p>
      <app-izgled />
      <app-kalendar-link />
    </section>

    <section id="racun">
      <h2>Račun</h2>
      <p class="sivo">{{ auth.korisnik()?.ime }} · {{ auth.korisnik()?.email }}</p>
      <app-lozinka />
      <div class="poveznice">
        <a mat-stroked-button routerLink="/povijest"><mat-icon>history</mat-icon> Povijest promjena</a>
        <a mat-stroked-button routerLink="/kos"><mat-icon>delete</mat-icon> Koš</a>
        <button mat-stroked-button type="button" (click)="auth.odjava()"><mat-icon>logout</mat-icon> Odjava</button>
      </div>
    </section>
  `,
  styles: `
    :host { display: block; max-width: 720px; }
    .skok { display: flex; gap: 8px; margin-bottom: 8px; }
    .skok a {
      padding: 6px 14px; border-radius: 999px; text-decoration: none; font-size: 14px;
      background: var(--mat-sys-surface-container); color: var(--mat-sys-on-surface);
    }
    /* Naslov grupe ne smije završiti ispod gornje trake kad se skoči na njega */
    section { display: flex; flex-direction: column; gap: 16px; padding-top: 24px; scroll-margin-top: 64px; }
    section h2 { margin: 0; font-size: 20px; }
    section > .sivo { margin: -12px 0 0; }
    .poveznice { display: flex; flex-wrap: wrap; gap: 8px; }
    app-tvrtka { padding: 0; }
  `,
})
export class PostavkeComponent {
  auth = inject(AuthService);

  // Običan #link bi u Angularu promijenio adresu i ponovno učitao stranicu, pa se pomiče ručno
  idiNa(dogadaj: Event, id: string): void {
    dogadaj.preventDefault();
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }
}
