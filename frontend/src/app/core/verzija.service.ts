import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { SwUpdate } from '@angular/service-worker';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Aplikacija je spremljena na uređaju, pa se nova verzija ne pojavi sama: service worker je
 * skine u pozadini i javi da je spremna. Tada se stranica osvježi, osim usred unosa.
 */
@Injectable({ providedIn: 'root' })
export class VerzijaService {
  private azuriranja = inject(SwUpdate);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  constructor() {
    if (!this.azuriranja.isEnabled) {
      return;
    }
    this.azuriranja.versionUpdates.subscribe((dogadaj) => {
      if (dogadaj.type === 'VERSION_READY') {
        this.novaVerzija();
      }
    });
    // Spremljena verzija je oštećena ili obrisana s uređaja: pomaže samo ponovno učitavanje
    this.azuriranja.unrecoverable.subscribe(() => location.reload());
    // Aplikacija na mobitelu danima stoji otvorena u pozadini; provjeri čim se vrati na ekran
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        this.azuriranja.checkForUpdate().catch(() => undefined);
      }
    });
  }

  private novaVerzija(): void {
    // Osvježavanje usred forme ili otvorenog prozora obrisalo bi upisano, pa se tada samo ponudi
    const usredUnosa = /\/(novo|uredi)(\?|$)/.test(this.router.url) || this.dialog.openDialogs.length > 0;
    if (!usredUnosa) {
      location.reload();
      return;
    }
    this.snack.open('Nova verzija aplikacije je spremna.', 'Osvježi').onAction().subscribe(() => location.reload());
  }
}
