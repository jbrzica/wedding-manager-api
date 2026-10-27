# Wedding Manager API

REST API za fotografe vjenčanja: vođenje klijenata, vjenčanja, paketa, dodataka i uplata, s automatskim izračunom duga po vjenčanju.

Projekt je nastao iz stvarne potrebe obiteljskog fotografskog posla.

## Tehnologije

- Java 21
- Spring Boot 4 (Spring Web, Spring Data JPA, Validation)
- PostgreSQL 17
- Gradle
- Docker i Docker Compose
- Swagger / OpenAPI (springdoc)
- Lombok

## Pokretanje

Potreban je samo [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
git clone https://github.com/jbrzica/wedding-manager-api.git
cd wedding-manager-api
docker compose up -d --build
```

Aplikacija radi na `http://localhost:8080`.
Dokumentacija i testiranje API-ja: **http://localhost:8080/swagger-ui.html**

Gašenje:

```bash
docker compose down
```

## Model podataka

```
klijent 1 ──── N vjencanje N ──── 1 paket
                    │
          ┌─────────┴─────────┐
          N                   N
       uplata          vjencanje_dodatak  N ──── 1 dodatak
```

- Klijent može imati više vjenčanja (1:N)
- Vjenčanje ima jedan paket i više uplata (N:1, 1:N)
- Vjenčanje i dodaci su u odnosu N:M preko spojne tablice

## API

| Metoda | Putanja | Opis |
|---|---|---|
| GET / POST | `/api/klijenti` | Popis / novi klijent |
| GET / DELETE | `/api/klijenti/{id}` | Jedan klijent / brisanje |
| GET / POST | `/api/paketi` | Popis / novi paket |
| GET / DELETE | `/api/paketi/{id}` | Jedan paket / brisanje |
| GET / POST | `/api/dodaci` | Popis / novi dodatak |
| GET / DELETE | `/api/dodaci/{id}` | Jedan dodatak / brisanje |
| GET | `/api/vjencanja?status=POTVRDENO` | Popis vjenčanja, opcionalni filter po statusu |
| POST | `/api/vjencanja` | Novo vjenčanje |
| GET / DELETE | `/api/vjencanja/{id}` | Jedno vjenčanje / brisanje |
| PATCH | `/api/vjencanja/{id}/status?status=...` | Promjena statusa |
| POST | `/api/vjencanja/{id}/dodaci/{dodatakId}` | Dodavanje dodatka vjenčanju |
| GET / POST | `/api/vjencanja/{id}/uplate` | Uplate za vjenčanje / nova uplata |
| GET | `/api/vjencanja/{id}/dug` | Ukupna cijena, uplaćeno i preostali dug |

Statusi vjenčanja: `UPIT`, `POTVRDENO`, `ODRADENO`, `ISPORUCENO`.

### Primjer: izračun duga

Paket 2500 € + dron 300 € + album 450 €, uplaćena kapara 1000 €:

```json
GET /api/vjencanja/1/dug

{
  "ukupnaCijena": 3250.00,
  "uplaceno": 1000.00,
  "dug": 2250.00
}
```

## Obrada grešaka

Sve greške vraćaju isti oblik:

```json
{
  "status": 400,
  "poruka": "Neispravni podaci",
  "greske": {
    "email": "Email nije ispravan"
  }
}
```

| Kod | Kada |
|---|---|
| 400 | Neispravni podaci (validacija) |
| 404 | Zapis ne postoji |
| 409 | Duplikat (npr. email već postoji) |

## Struktura projekta

```
src/main/java/hr/jbrzica/wedding_manager_api/
├── controller/   REST endpointi
├── service/      poslovna logika
├── repository/   pristup bazi (Spring Data JPA)
├── entity/       JPA entiteti
├── dto/          ulazni i izlazni objekti API-ja
└── exception/    centralna obrada grešaka
```

## Planirano

- Web sučelje (React)
- Prijava korisnika