# Wedding Manager

Web aplikacija za fotografe vjenčanja: vjenčanja po datumima, mladenci, raspored dana, ekipa, paketi, uplate i zarada po vjenčanju, te online rezervacija termina za kratka fotografiranja (npr. božićno).

**Uživo:** [app.brzicaweddings.com](https://app.brzicaweddings.com). Svakodnevno je koristi obiteljski fotografski studio: u njoj su svi ugovori za vjenčanja 2027. i 2028., a klijenti preko nje sami rezerviraju termine za božićno fotografiranje.

Projekt je nastao iz stvarne potrebe obiteljskog posla: vjenčanja su se vodila na papiru, a termini za fotografiranja preko poruka.

## Što aplikacija radi

**Za studio (prijava):**
- popis i kalendar vjenčanja, statusi (`UPIT`, `POTVRDENO`, `ODRADENO`, `ISPORUCENO`), brzi upis upita
- mladenci, raspored dana, ekipa s honorarima, paketi i dodaci, uplate (kapara, rate) i izračun duga i zarade
- upozorenje kad se datum ili član ekipe preklapa s drugim vjenčanjem
- popunjavanje ugovora iz vlastitog Word predloška, statistika po godinama, povijest promjena i koš za vraćanje obrisanog
- kalendar u Google/Apple kalendaru (iCal link) i javni raspored za suradnike
- više korisnika u istoj tvrtki (pozivnica mailom), svaka tvrtka vidi samo svoje podatke
- radi na mobitelu kao aplikacija (PWA), nadolazeća vjenčanja vidljiva i bez interneta

**Termini (online rezervacije):**
- studio upiše fotografiranje: pakete, dane i sate, razmak termina, naslovnu sliku
- klijent preko linka u tri koraka odabere paket, dan u kalendaru i slobodno vrijeme, bez registracije
- potvrda mailom s linkom za otkazivanje (najkasnije 24 sata prije), obavijest studiju mailom i u aplikaciji
- kratki link (`bozicno.brzicaweddings.com`) i kartica sa slikom kad se link pošalje na Viber ili WhatsApp

## Tehnologije

| | |
|---|---|
| Backend | Java 21, Spring Boot 4 (Web, Data JPA, Security, Validation, Mail), JWT, Flyway |
| Baza | PostgreSQL |
| Frontend | Angular 21 (standalone komponente, signali), Angular Material, service worker (PWA) |
| Testovi | JUnit 5 + MockMvc (integracijski, na pravoj PostgreSQL bazi), Vitest za frontend |
| Infrastruktura | Docker, Railway (aplikacija i baza), Brevo (mailovi), Cloudflare R2 (redoviti backup baze), GitHub Actions (CI) |

```
Preglednik / mobitel (Angular PWA)
        │  HTTPS, JSON, JWT
        ▼
Spring Boot  ──── PostgreSQL (Flyway migracije V1–V14)
   │    │
   │    └── Brevo (mailovi: šifre, pozivi, potvrde rezervacija)
   └── Angular build se poslužuje iz istog servisa
Backup CRON ──── Cloudflare R2 (redoviti dump baze)
```

## Pokretanje

Potreban je samo [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
git clone https://github.com/jbrzica/wedding-manager-api.git
cd wedding-manager-api
docker compose up -d --build
```

Aplikacija radi na `http://localhost:8080`.
Mailovi (šifre za potvrdu i pozive) lokalno se vide u Mailpitu: **http://localhost:8025**
Dokumentacija i testiranje API-ja: **http://localhost:8080/swagger-ui.html**

`compose.yaml` lokalno postavlja `JWT_TAJNA`, uključuje Swagger i registraciju. Na serveru se postavljaju varijable:

| Varijabla | Obavezno | Opis |
|---|---|---|
| `JWT_TAJNA` | da | Base64, najmanje 32 bajta (npr. `openssl rand -base64 48`); bez nje se aplikacija ne pokrene |
| `REGISTRACIJA_OTVORENA` | ne | `true` dopušta registraciju novih tvrtki; zadano je zatvoreno |
| `SWAGGER_UKLJUCEN` | ne | `true` uključuje Swagger; zadano je isključen |

Gašenje:

```bash
docker compose down
```

## Prijava i tvrtke

Svaki korisnik pripada jednoj tvrtki i vidi samo njezine podatke (vjenčanja, pakete, dodatke, suradnike).

1. `POST /api/auth/registracija` napravi tvrtku i vlasnika i pošalje šifru na email
2. `POST /api/auth/potvrdi-email` sa šifrom potvrdi email i vrati JWT token
3. `POST /api/auth/prijava` vrati token; svi ostali zahtjevi šalju zaglavlje `Authorization: Bearer <token>` (u Swaggeru gumb **Authorize**)
4. Vlasnik poziva članove: `POST /api/tvrtka/pozivi`; pozvani se pridružuje s `POST /api/auth/prihvati-poziv`

| Metoda | Putanja | Opis |
|---|---|---|
| POST | `/api/auth/registracija` | Nova tvrtka i vlasnik |
| POST | `/api/auth/potvrdi-email` | Potvrda emaila šifrom, vraća token |
| POST | `/api/auth/ponovno-posalji-kod` | Nova šifra za potvrdu |
| POST | `/api/auth/prijava` | Prijava, vraća token |
| POST | `/api/auth/prihvati-poziv` | Pridruživanje tvrtki šifrom iz poziva |
| GET | `/api/auth/ja` | Prijavljeni korisnik |
| GET / PUT | `/api/tvrtka` | Moja tvrtka i članovi / promjena naziva (vlasnik) |
| POST | `/api/tvrtka/pozivi` | Poziv novog člana mailom (vlasnik) |
| DELETE | `/api/tvrtka/clanovi/{id}` | Uklanjanje člana (vlasnik) |

Lozinke i šifre spremaju se samo kao BCrypt hash. Šifra vrijedi 15 minuta i dopušta 5 pokušaja.

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
- Termini su odvojen dio: `dogadjaj_termina` (fotografiranje) ima pakete, dane s radnim vremenom i rezervacije (`rezervacija_termina`)

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
| 401 | Niste prijavljeni ili je token neispravan |
| 403 | Nemate pravo (npr. samo vlasnik smije pozivati članove) |
| 404 | Zapis ne postoji ili pripada drugoj tvrtki |
| 409 | Baza odbila zapis (npr. brisanje paketa koji se koristi na vjenčanju) |

## Online rezervacija termina

| Metoda | Putanja | Opis |
|---|---|---|
| GET / POST | `/api/termini` | Fotografiranja tvrtke / novo fotografiranje (paketi, dani, sati, razmak) |
| GET / PUT / DELETE | `/api/termini/{id}` | Raspored po danima s rezervacijama / izmjena / brisanje |
| PUT / DELETE | `/api/termini/{id}/slika` | Naslovna slika javne stranice |
| POST | `/api/termini/{id}/rezervacije` | Ručni upis (klijent se javio telefonom) |
| GET | `/api/termini/nove` | Nove online rezervacije za obavijest u aplikaciji |
| GET | `/api/javno/termini/{kod}` | Javno: paketi, slobodna i zauzeta vremena (bez podataka o klijentima) |
| POST | `/api/javno/termini/{kod}/rezervacije` | Javno: rezervacija |
| GET / POST | `/api/javno/otkazivanje/{kod}` | Javno: pregled i otkazivanje linkom iz maila |

Kako je riješeno:
- **Isti termin ne mogu rezervirati dvije osobe**, ni ako kliknu u istoj sekundi: djelomični jedinstveni indeks u bazi `(dogadjaj_id, datum, vrijeme) WHERE otkazano IS NULL`. Drugi upis baza odbije, a aplikacija vrati 409 i poruku da odabere drugo vrijeme. Otkazani termin se odmah oslobađa.
- Razmak termina je fiksan (npr. 30 min) i svaki paket mora stati u njega, pa jedna rezervacija uvijek zauzima točno jedno mjesto.
- Javni link je nasumični token (32 bajta). Kod za otkazivanje se u bazi čuva samo kao SHA-256 hash.
- Najviše 2 aktivna termina po emailu i skriveno polje protiv botova.
- Cijena i trajanje paketa kopiraju se u rezervaciju, pa kasnija promjena cijene ne mijenja postojeće rezervacije.
- Za kratki link i poddomenu server u HTML upiše Open Graph oznake (naslov, opis, slika), jer Viber i WhatsApp ne pokreću Angular.

## Sigurnost

- JWT pristupni token s kratkim trajanjem i refresh token u `HttpOnly` kolačiću, s rotacijom i otkrivanjem ponovne upotrebe
- lozinke i šifre samo kao BCrypt hash, ograničenje pokušaja prijave i slanja šifri
- svaka tvrtka vidi samo svoje podatke: tuđi zapis vraća 404, ne 403, da se ne otkrije da postoji
- registracija novih tvrtki zatvorena (`REGISTRACIJA_OTVORENA`), Swagger isključen na serveru
- sigurnosna zaglavlja (CSP `frame-ancestors`, Referrer-Policy, Permissions-Policy)
- preusmjeravanja i linkovi u mailovima uvijek koriste adresu iz postavki, nikad adresu iz zahtjeva

## Testovi i CI

- **108 backend testova** pokreće se na pravoj PostgreSQL bazi: prijava i tokeni, izolacija tvrtki, financije, statusi, ugovor, rezervacije termina (dvostruka rezervacija, rokovi, otkazivanje, kartice za poruke)
- frontend testovi u Vitestu (prijava i osvježavanje tokena, servisi)
- GitHub Actions na svaku promjenu pokrene backend testove i frontend build; grana `main` prima samo promjene koje prođu

```bash
./gradlew test             # backend (treba PostgreSQL, npr. iz compose.yaml)
cd frontend && npx ng test # frontend
```

## Struktura projekta

```
src/main/java/hr/jbrzica/wedding_manager_api/
├── controller/   REST endpointi (vjenčanja, paketi, tvrtka, statistika...)
├── service/      poslovna logika
├── repository/   pristup bazi (Spring Data JPA)
├── entity/       JPA entiteti
├── dto/          ulazni i izlazni objekti API-ja
├── security/     JWT, refresh tokeni, ograničenje pokušaja
├── termini/      online rezervacije termina (javni i studijski dio)
├── ugovor/       ugovor iz Word predloška
└── exception/    centralna obrada grešaka
src/main/resources/db/migration/   Flyway migracije
frontend/src/app/                  Angular aplikacija (vjencanja, termini, javno, postavke...)
```
