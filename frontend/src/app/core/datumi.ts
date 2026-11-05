// Backend šalje i prima datume kao "2026-06-14" (LocalDate).
// Ovdje ih pretvaramo bez new Date("2026-06-14"), jer bi to bio UTC i mogao pomaknuti dan.

export function uIso(datum: Date): string {
  const m = String(datum.getMonth() + 1).padStart(2, '0');
  const d = String(datum.getDate()).padStart(2, '0');
  return `${datum.getFullYear()}-${m}-${d}`;
}

export function izIso(iso: string): Date {
  const [g, m, d] = iso.split('-').map(Number);
  return new Date(g, m - 1, d);
}

export function danas(): string {
  return uIso(new Date());
}
