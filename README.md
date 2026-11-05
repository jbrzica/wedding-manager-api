# Wedding Manager API

Aplikacija za fotografe vjenčanja: vođenje vjenčanja po datumima, mladenaca, rasporeda dana, ekipe (videografi, drugi fotografi), paketa, dodataka i uplata, s izračunom preostalog duga i zarade po vjenčanju.

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
paket 1 ──── N vjencanje 1 ──── N uplata
                 │  │
                 │  └──── 1:N stavka_rasporeda
                 │
     ┌───────────┴───────────┐
     N                       N
vjencanje_dodatak        clan_ekipe (uloga, honorar)
     N                       N
     │                       │
     1                       1
  dodatak                suradnik
```

- Mladenci (mlada i mladoženja) su dio vjenčanja: klasa `Osoba` je `@Embeddable` i koristi se dvaput
- Vjenčanje ima više uplata i više stavki rasporeda dana (1:N)
- Vjenčanja i dodaci su N:M preko spojne tablice
- Vjenčanja i suradnici su povezani preko entiteta `ClanEkipe`, jer veza ima svoje podatke (uloga i honorar)
- Vjenčanje pamti svoju dogovorenu cijenu: kasnija promjena cijene paketa ne mijenja stara vjenčanja

## API

| Metoda | Putanja | Opis |
|---|---|---|
| GET | `/api/vjencanja?razdoblje=NADOLAZECA` | Popis po datumima (`NADOLAZECA`, `PROSLA`, `SVA`), opcionalno `godina`, `status`, `pretraga` |
| GET | `/api/vjencanja/kalendar?od=2028-06-01&do=2028-06-30` | Vjenčanja u razdoblju, za kalendar |
| GET | `/api/vjencanja/pregled` | Sljedeće vjenčanje, broj vjenčanja ove godine, ukupno nenaplaćeno |
| GET | `/api/vjencanja/upozorenja?datum=...&suradnikIds=1,2` | Upozorenja na preklapanje datuma i ekipe (samo upozorenje, spremanje je dopušteno) |
| POST | `/api/vjencanja` | Novo vjenčanje (mladenci, raspored, ekipa, paket, dodaci, cijena) |
| GET / PUT / DELETE | `/api/vjencanja/{id}` | Detalji / izmjena / brisanje |
| PATCH | `/api/vjencanja/{id}/status?status=...` | Promjena statusa |
| GET | `/api/vjencanja/{id}/financije` | Cijena, uplaćeno, preostalo, je li kapara plaćena, honorari ekipe, zarada |
| GET / POST | `/api/vjencanja/{id}/uplate` | Uplate / nova uplata (`KAPARA`, `RATA`, `OSTATAK`) |
| DELETE | `/api/vjencanja/{id}/uplate/{uplataId}` | Brisanje uplate |
| GET / POST, GET / PUT / DELETE `{id}` | `/api/paketi` | Paketi i njihove cijene |
| GET / POST, GET / PUT / DELETE `{id}` | `/api/dodaci` | Dodaci |
| GET / POST, GET / PUT / DELETE `{id}` | `/api/suradnici` | Suradnici (fotografi, videografi, dron...) |

Statusi vjenčanja: `UPIT`, `POTVRDENO`, `ODRADENO`, `ISPORUCENO`. Upisana kapara automatski potvrđuje vjenčanje koje je bilo upit.

Ako se kod vjenčanja ne pošalje `dogovorenaCijena`, izračuna se iz cijene paketa i dodataka.

### Primjer: financije vjenčanja

Dogovoreno 3250 €, uplaćena kapara 1000 €, videograf 800 €:

```json
GET /api/vjencanja/1/financije

{
  "dogovorenaCijena": 3250.00,
  "uplaceno": 1000.00,
  "preostalo": 2250.00,
  "kaparaPlacena": true,
  "honorariEkipe": 800.00,
  "zarada": 2450.00
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
| 409 | Baza odbila zapis (npr. brisanje paketa koji se koristi na vjenčanju) |

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

- Web sučelje (Angular) s kalendarom
- Prijava korisnika, potvrda emaila šifrom i više korisnika na istom računu firme
- Objava na internet i migracije baze (Flyway)
