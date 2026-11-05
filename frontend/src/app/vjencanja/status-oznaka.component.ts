import { Component, computed, input } from '@angular/core';
import { StatusVjencanja } from '../core/modeli';
import { STATUSI } from '../core/oznake';

@Component({
  selector: 'app-status',
  template: `<span class="status" [class]="'status ' + status().toLowerCase()">{{ tekst() }}</span>`,
  styles: `
    .status {
      display: inline-block; padding: 2px 10px; border-radius: 12px;
      font-size: 12px; font-weight: 500; white-space: nowrap;
    }
    .upit { background: #fff3cd; color: #7a5b00; }
    .potvrdeno { background: #d6f0dd; color: #1e6b34; }
    .odradeno { background: #dbe7fb; color: #1d4f9c; }
    .isporuceno { background: #e8e8e8; color: #4a4a4a; }
    .otkazano { background: #fde2e1; color: #9b2c2c; text-decoration: line-through; }
  `,
})
export class StatusOznakaComponent {
  status = input.required<StatusVjencanja>();
  tekst = computed(() => STATUSI[this.status()]);
}
