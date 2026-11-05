import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

// Ugrađeni adapter ne razumije hrvatski zapis "14.6.2026." kad se datum upisuje rukom,
// pa ga ovdje sami čitamo. Prikaz (format) ostaje ugrađeni, prema jeziku 'hr'.
@Injectable()
export class HrDateAdapter extends NativeDateAdapter {
  override parse(value: unknown, parseFormat?: unknown): Date | null {
    if (typeof value === 'string') {
      const m = value.trim().match(/^(\d{1,2})\s*\.\s*(\d{1,2})\s*\.\s*(\d{4})\s*\.?$/);
      if (m) {
        const datum = new Date(Number(m[3]), Number(m[2]) - 1, Number(m[1]));
        // 31.2.2026. ne smije postati 3.3.2026.
        return datum.getDate() === Number(m[1]) ? datum : this.invalid();
      }
    }
    return super.parse(value, parseFormat);
  }
}
