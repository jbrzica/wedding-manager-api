import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { Tema, TemaService } from '../core/tema.service';

@Component({
  selector: 'app-izgled',
  imports: [MatCardModule, MatButtonToggleModule, MatIconModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header><mat-card-title>Izgled</mat-card-title></mat-card-header>
      <mat-card-content>
        <mat-button-toggle-group class="izgled" [value]="tema.tema()" (change)="tema.postavi($event.value)"
                                 hideSingleSelectionIndicator aria-label="Izgled aplikacije">
          @for (o of opcije; track o.vrijednost) {
            <mat-button-toggle [value]="o.vrijednost"><mat-icon>{{ o.ikona }}</mat-icon><span>{{ o.naziv }}</span></mat-button-toggle>
          }
        </mat-button-toggle-group>
        <p class="sivo">„Kao uređaj” prati postavku mobitela ili računala. Izbor vrijedi samo na ovom uređaju.</p>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    /* Tri jednaka dijela u jednom redu; na mobitelu ikona iznad teksta da sve stane */
    mat-button-toggle-group { margin-top: 4px; width: 100%; max-width: 480px; }
    mat-button-toggle { flex: 1; }
    mat-icon { vertical-align: -6px; margin-right: 6px; }
    @media (max-width: 480px) {
      ::ng-deep .izgled .mat-button-toggle-label-content { display: flex; flex-direction: column; align-items: center;
        line-height: 18px; padding: 8px 4px; font-size: 13px; }
      mat-icon { margin: 0 0 2px; vertical-align: 0; }
    }
    p { margin: 12px 0 0; }
  `,
})
export class IzgledComponent {
  readonly tema = inject(TemaService);
  readonly opcije: { vrijednost: Tema; naziv: string; ikona: string }[] = [
    { vrijednost: 'svijetlo', naziv: 'Svijetlo', ikona: 'light_mode' },
    { vrijednost: 'tamno', naziv: 'Tamno', ikona: 'dark_mode' },
    { vrijednost: 'sustav', naziv: 'Kao uređaj', ikona: 'brightness_auto' },
  ];
}
