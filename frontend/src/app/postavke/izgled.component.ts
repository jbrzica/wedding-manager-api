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
        <mat-button-toggle-group [value]="tema.tema()" (change)="tema.postavi($event.value)"
                                 hideSingleSelectionIndicator aria-label="Izgled aplikacije">
          @for (o of opcije; track o.vrijednost) {
            <mat-button-toggle [value]="o.vrijednost"><mat-icon>{{ o.ikona }}</mat-icon> {{ o.naziv }}</mat-button-toggle>
          }
        </mat-button-toggle-group>
        <p class="sivo">„Kao uređaj” prati postavku mobitela ili računala. Izbor vrijedi samo na ovom uređaju.</p>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    mat-button-toggle-group { margin-top: 4px; flex-wrap: wrap; }
    mat-icon { vertical-align: -6px; margin-right: 2px; }
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
