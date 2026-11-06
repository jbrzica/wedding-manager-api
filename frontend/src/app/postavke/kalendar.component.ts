import { Component, inject, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-kalendar-link',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header><mat-card-title>Kalendar na mobitelu</mat-card-title></mat-card-header>
      <mat-card-content>
        <p class="sivo">
          Dodajte ovaj link u Google ili Apple kalendar i sva vjenčanja pojavit će se u kalendaru na mobitelu.
          Novo vjenčanje se pojavi samo, ali Google kalendar osvježava i do nekoliko sati.
        </p>

        @if (link(); as l) {
          <div class="u-redu">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <input matInput [value]="l" readonly (focus)="$any($event.target).select()">
            </mat-form-field>
            <button mat-flat-button type="button" (click)="kopiraj(l)"><mat-icon>content_copy</mat-icon> Kopiraj</button>
          </div>
        }

        <h3>Google kalendar</h3>
        <ol>
          <li>Na računalu otvorite <strong>calendar.google.com</strong>.</li>
          <li>Lijevo kod „Ostali kalendari” kliknite <strong>+</strong> → <strong>Iz URL-a</strong>.</li>
          <li>Zalijepite link i kliknite <strong>Dodaj kalendar</strong>. Pojavit će se i na mobitelu.</li>
        </ol>

        <h3>iPhone</h3>
        <ol>
          <li><strong>Postavke</strong> → <strong>Kalendar</strong> → <strong>Računi</strong> → <strong>Dodaj račun</strong> → <strong>Ostalo</strong>.</li>
          <li><strong>Dodaj pretplatnički kalendar</strong>, zalijepite link i spremite.</li>
        </ol>

        <p class="upozorenje">
          <mat-icon inline>lock</mat-icon>
          Tko ima ovaj link vidi vjenčanja i brojeve mladenaca. Ne šaljite ga nikome izvan tvrtke.
        </p>
        @if (auth.vlasnik()) {
          <button mat-stroked-button type="button" (click)="noviLink()">
            <mat-icon>refresh</mat-icon> Napravi novi link
          </button>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .u-redu { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-bottom: 8px; }
    .u-redu mat-form-field { flex: 1; min-width: 220px; }
    h3 { font-size: 15px; margin: 16px 0 4px; }
    ol { margin: 0; padding-left: 20px; line-height: 1.6; }
    .upozorenje { margin: 16px 0 12px; color: var(--mat-sys-on-surface-variant); font-size: 14px; }
  `,
})
export class KalendarLinkComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  auth = inject(AuthService);

  link = signal<string | null>(null);

  ngOnInit(): void {
    this.api.kalendarLink().subscribe({
      next: (r) => this.postavi(r.putanja),
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  kopiraj(link: string): void {
    navigator.clipboard.writeText(link).then(
      () => this.poruka('Link je kopiran.'),
      () => this.poruka('Kopiranje nije uspjelo, označite link i kopirajte ga ručno.'),
    );
  }

  noviLink(): void {
    if (!confirm('Stari link prestat će raditi i kalendar ćete morati ponovno dodati na svim mobitelima. Nastaviti?')) {
      return;
    }
    this.api.noviKalendarLink().subscribe({
      next: (r) => {
        this.postavi(r.putanja);
        this.poruka('Napravljen je novi link.');
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  // Backend vraća samo putanju, a adresu servera znamo iz browsera.
  private postavi(putanja: string): void {
    this.link.set(window.location.origin + putanja);
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 4000 });
  }
}
