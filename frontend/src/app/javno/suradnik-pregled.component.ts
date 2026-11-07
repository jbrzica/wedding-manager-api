import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ApiService } from '../core/api.service';
import { SuradnikPregled } from '../core/modeli';
import { ULOGE_SURADNIKA } from '../core/oznake';
import { izIso } from '../core/datumi';

/**
 * Stranica koju suradnik otvara preko svog tajnog linka, bez prijave.
 * Backend za nju ni ne šalje cijene, pa ih ovdje nema što prikazati.
 */
@Component({
  selector: 'app-suradnik-pregled',
  imports: [DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="stranica">
      <header>
        <span class="marka">Wedding Manager</span>
        @if (pregled(); as p) { <span class="sivo">{{ p.tvrtka }}</span> }
      </header>

      @if (ucitava()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (greska()) {
        <mat-card appearance="outlined" class="poruka">
          <mat-icon>link_off</mat-icon>
          <h1>Link ne vrijedi</h1>
          <p class="sivo">Možda je zamijenjen novim. Zatražite novi link od fotografa.</p>
        </mat-card>
      } @else if (pregled(); as p) {
        <h1>Bok, {{ p.ime.split(' ')[0] }}</h1>
        <p class="sivo uvod">Vjenčanja na kojima radiš. Stranica je uvijek ažurna, pa je možeš spremiti i otvarati kad trebaš.</p>

        <div class="kalendar">
          <a mat-stroked-button [href]="webcal(p.kalendar)"><mat-icon>event</mat-icon> Dodaj u kalendar</a>
          <button mat-button type="button" (click)="kopirajKalendar(p.kalendar)">
            <mat-icon>content_copy</mat-icon> {{ kopirano() ? 'Kopirano' : 'Kopiraj link kalendara' }}
          </button>
        </div>

        @for (v of p.vjencanja; track $index) {
          <mat-card appearance="outlined" class="vjencanje">
            <div class="vrh">
              <div class="datum">
                <span class="dan">{{ izIso(v.datum) | date: 'd' }}</span>
                <span class="mjesec">{{ izIso(v.datum) | date: 'MMM yyyy' }}</span>
              </div>
              <div class="naslov">
                <h2>{{ v.mlada?.ime }} & {{ v.mladozenja?.ime }}</h2>
                <div class="sivo">{{ izIso(v.datum) | date: 'EEEE' }}@if (v.mojaUloga) { · {{ uloge[v.mojaUloga] }} }</div>
                @if (!v.potvrdeno) { <span class="nepotvrdeno">Još nije potvrđeno</span> }
              </div>
            </div>

            @if (v.lokacija) {
              <a class="lokacija" [href]="karta(v.lokacija)" target="_blank" rel="noopener">
                <mat-icon inline>place</mat-icon> {{ v.lokacija }}
              </a>
            }

            @if (v.raspored.length) {
              <h3>Raspored</h3>
              <ul class="raspored">
                @for (s of v.raspored; track $index) {
                  <li>
                    <span class="vrijeme">{{ s.vrijeme ? s.vrijeme.slice(0, 5) : '--:--' }}</span>
                    <div>
                      <strong>{{ s.naziv }}</strong>
                      @if (s.adresa) {
                        <div><a [href]="karta(s.adresa)" target="_blank" rel="noopener">{{ s.adresa }}</a></div>
                      }
                      @if (s.napomena) { <div class="sivo">{{ s.napomena }}</div> }
                    </div>
                  </li>
                }
              </ul>
            }

            <h3>Kontakti</h3>
            <ul class="kontakti">
              @for (o of [v.mlada, v.mladozenja]; track $index) {
                @if (o) {
                  <li>
                    <span>{{ o.ime }}</span>
                    @if (o.telefon) { <a [href]="'tel:' + o.telefon">{{ o.telefon }}</a> }
                  </li>
                }
              }
              @for (c of v.ekipa; track $index) {
                @if (!c.ja) {
                  <li>
                    <span>{{ c.ime }}@if (c.uloga) { <span class="sivo"> · {{ uloge[c.uloga] }}</span> }</span>
                    @if (c.telefon) { <a [href]="'tel:' + c.telefon">{{ c.telefon }}</a> }
                  </li>
                }
              }
            </ul>
          </mat-card>
        } @empty {
          <mat-card appearance="outlined" class="poruka">
            <p class="sivo">Trenutno nemaš zakazanih vjenčanja.</p>
          </mat-card>
        }
      }
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; background: var(--mat-sys-surface); }
    .stranica { max-width: 640px; margin: 0 auto; padding: 16px 16px 48px; }
    header { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; padding: 8px 0 24px; }
    .marka { font-weight: 700; font-size: 17px; }
    h1 { margin: 0 0 4px; font-size: 26px; }
    .uvod { margin: 0 0 16px; }
    .kalendar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 20px; }
    .vjencanje { padding: 16px; margin-bottom: 16px; }
    .vrh { display: flex; gap: 16px; align-items: flex-start; }
    .datum { display: flex; flex-direction: column; align-items: center; min-width: 64px; padding: 6px 8px;
             border-radius: 12px; background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container); }
    .dan { font-size: 26px; font-weight: 700; line-height: 1.1; }
    .mjesec { font-size: 12px; text-transform: uppercase; }
    .naslov h2 { margin: 2px 0; font-size: 19px; }
    .naslov .sivo::first-letter { text-transform: uppercase; }
    .nepotvrdeno { display: inline-block; margin-top: 6px; padding: 2px 8px; border-radius: 999px; font-size: 12px;
                   background: var(--wm-upozorenje-pozadina); color: var(--wm-upozorenje); }
    .lokacija { display: inline-flex; align-items: center; gap: 4px; margin-top: 12px; }
    h3 { font-size: 13px; text-transform: uppercase; letter-spacing: .04em; color: var(--mat-sys-on-surface-variant);
         margin: 20px 0 8px; }
    ul { list-style: none; margin: 0; padding: 0; }
    .raspored li { display: flex; gap: 14px; padding: 8px 0; border-top: 1px solid var(--mat-sys-outline-variant); }
    .raspored li:first-child { border-top: none; }
    .vrijeme { font-weight: 600; min-width: 46px; font-variant-numeric: tabular-nums; }
    .kontakti li { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; padding: 6px 0; }
    .poruka { padding: 32px 24px; text-align: center; }
    .poruka mat-icon { font-size: 40px; width: 40px; height: 40px; color: var(--mat-sys-on-surface-variant); }
  `,
})
export class SuradnikPregledComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);

  readonly uloge = ULOGE_SURADNIKA;
  readonly izIso = izIso;
  pregled = signal<SuradnikPregled | null>(null);
  ucitava = signal(true);
  greska = signal(false);
  kopirano = signal(false);

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token') ?? '';
    this.api.suradnikPregled(token).subscribe({
      next: (p) => {
        this.pregled.set(p);
        this.ucitava.set(false);
      },
      error: () => {
        this.greska.set(true);
        this.ucitava.set(false);
      },
    });
  }

  karta(adresa: string): string {
    return 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(adresa);
  }

  // webcal:// na iPhoneu i u većini kalendara odmah nudi pretplatu
  webcal(putanja: string): string {
    return 'webcal://' + window.location.host + putanja;
  }

  kopirajKalendar(putanja: string): void {
    navigator.clipboard.writeText(window.location.origin + putanja).then(() => this.kopirano.set(true));
  }
}
