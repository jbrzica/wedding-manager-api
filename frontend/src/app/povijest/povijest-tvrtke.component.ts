import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../core/api.service';
import { ClanTvrtke, Promjena } from '../core/modeli';

const U_SAZETKU = 5;
const PO_KORAKU = 30;

/**
 * Promjene u tvrtki. U Postavkama samo zadnjih 5 (sazeto), a cijela povijest s filtrom
 * po osobi je na posebnoj stranici. Vidi se i ono čega na vjenčanju više nema.
 */
@Component({
  selector: 'app-povijest-tvrtke',
  imports: [DatePipe, RouterLink, MatCardModule, MatButtonModule, MatFormFieldModule, MatSelectModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>Povijest promjena</mat-card-title>
        <mat-card-subtitle>tko je i kada mijenjao vjenčanja, cijene i uplate</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        @if (!sazeto() && clanovi().length > 1) {
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="osoba">
            <mat-label>Osoba</mat-label>
            <mat-select [value]="korisnikId()" (selectionChange)="filtriraj($event.value)">
              <mat-option [value]="null">Svi</mat-option>
              @for (c of clanovi(); track c.id) { <mat-option [value]="c.id">{{ c.ime }}</mat-option> }
            </mat-select>
          </mat-form-field>
        }
        @for (p of promjene().slice(0, prikazano()); track p.id) {
          <div class="zapis">
            <span class="vrijeme">{{ p.vrijeme | date: 'd. M. yyyy. HH:mm' }}</span>
            <div>
              <strong>{{ p.korisnik }}</strong>
              @if (p.vjencanje) {
                ·
                @if (p.stanje === 'AKTIVNO') {
                  <a [routerLink]="['/vjencanja', p.vjencanjeId]">{{ p.vjencanje }}</a>
                } @else if (p.stanje === 'U_KOSU') {
                  <a routerLink="/kos">{{ p.vjencanje }}</a> <span class="sivo">(u košu)</span>
                } @else {
                  {{ p.vjencanje }} <span class="sivo">(obrisano)</span>
                }
              }
              <div>{{ p.opis }}</div>
            </div>
          </div>
        } @empty {
          <p class="sivo">Još nema zabilježenih promjena.</p>
        }
        @if (sazeto()) {
          @if (promjene().length > prikazano()) {
            <a mat-button routerLink="/povijest">Prikaži svu povijest</a>
          }
        } @else if (promjene().length > prikazano()) {
          <button mat-button type="button" (click)="prikazano.set(prikazano() + korak)">Prikaži još</button>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    mat-card-content { padding-top: 8px; }
    .osoba { width: 220px; margin-bottom: 8px; }
    .zapis { display: flex; gap: 16px; padding: 8px 0; font-size: 14px; border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .zapis:last-of-type { border-bottom: none; }
    .vrijeme { color: var(--mat-sys-on-surface-variant); white-space: nowrap; min-width: 128px; font-variant-numeric: tabular-nums; }
    @media (max-width: 480px) { .zapis { flex-direction: column; gap: 2px; } }
  `,
})
export class PovijestTvrtkeComponent implements OnInit {
  private api = inject(ApiService);

  promjene = signal<Promjena[]>([]);
  clanovi = signal<ClanTvrtke[]>([]);
  korisnikId = signal<number | null>(null);
  sazeto = input(false);
  prikazano = signal(PO_KORAKU);
  readonly korak = PO_KORAKU;

  ngOnInit(): void {
    if (this.sazeto()) {
      this.prikazano.set(U_SAZETKU);
    } else {
      this.api.tvrtka().subscribe((t) => this.clanovi.set(t.clanovi));
    }
    this.ucitaj();
  }

  filtriraj(id: number | null): void {
    this.korisnikId.set(id);
    this.prikazano.set(PO_KORAKU);
    this.ucitaj();
  }

  private ucitaj(): void {
    this.api.povijest(this.korisnikId()).subscribe({ next: (p) => this.promjene.set(p), error: () => this.promjene.set([]) });
  }
}
