import { AbstractControl, ValidationErrors } from '@angular/forms';

// Zadnja znamenka OIB-a je kontrolna (ISO 7064, MOD 11,10), pa se tipfeler uhvati odmah.
export function oibIspravan(oib: string): boolean {
  if (!/^\d{11}$/.test(oib)) {
    return false;
  }
  let a = 10;
  for (let i = 0; i < 10; i++) {
    a = (a + Number(oib[i])) % 10;
    if (a === 0) {
      a = 10;
    }
    a = (a * 2) % 11;
  }
  const kontrolna = (11 - a) % 10;
  return kontrolna === Number(oib[10]);
}

export function oibValidator(control: AbstractControl): ValidationErrors | null {
  const v = (control.value ?? '').trim();
  return !v || oibIspravan(v) ? null : { oib: true };
}
