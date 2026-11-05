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
    .upit { background: var(--wm-upozorenje-pozadina); color: var(--wm-upozorenje); }
    .potvrdeno { background: var(--wm-uspjeh-pozadina); color: var(--wm-uspjeh); }
    .odradeno { background: var(--wm-info-pozadina); color: var(--wm-info); }
    .isporuceno { background: var(--wm-neutralno-pozadina); color: var(--wm-neutralno); }
    .otkazano { background: var(--wm-opasno-pozadina); color: var(--wm-opasno); text-decoration: line-through; }
  `,
})
export class StatusOznakaComponent {
  status = input.required<StatusVjencanja>();
  tekst = computed(() => STATUSI[this.status()]);
}
