# Wedding Manager — frontend

Angular 21 + Angular Material. Razgovara s backendom preko `/api` (u razvoju proxy na `http://localhost:8080`).

## Pokretanje

```bash
npm ci
npm start
```

Otvori http://localhost:4200. Backend mora raditi na portu 8080 (i `docker compose up -d db mailpit`).

## Ekrani

- prijava, registracija tvrtke, potvrda emaila šifrom, pridruživanje tvrtki pozivom
- popis vjenčanja (nadolazeća / prošla, grupirano po mjesecu, pretraga, sažetak)
- kalendar s brzim skokom na mjesec i godinu
- detalji vjenčanja: raspored, ekipa, financije, uplate, status, dijeljenje rasporeda
- unos i uređivanje vjenčanja (cijena iz paketa, upozorenje na preklapanje)
- postavke: paketi, dodaci, suradnici, članovi tvrtke
