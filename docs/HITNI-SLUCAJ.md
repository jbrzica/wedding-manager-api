# Hitni slučaj

Upute kad aplikacija ne radi ili treba vratiti podatke.

## 1. Stigla je obavijest da aplikacija ne radi

UptimeRobot svakih 5 minuta provjerava `https://app.brzicaweddings.com/actuator/health`.
Ta adresa vraća `UP` samo ako rade i aplikacija i baza.

1. **Pričekaj 10 minuta.** Većina padova prođe sama (restart, mreža, deploy).
   Kad proradi, stiže obavijest „Monitor is UP“ i ništa ne treba raditi.
2. **Ako i dalje ne radi**, na railway.app provjeri redom:
   - **Usage limit** (tvrdi limit 20 $). Ako je dosegnut, Railway gasi aplikaciju: podigni limit.
   - **wedding-manager-api**: je li zadnji deploy crven? Otvori zadnji zeleni deploy → ⋯ → **Redeploy**.
   - **Postgres**: radi li? Ako ne, ⋯ → **Restart**.
   - Inače aplikacija → ⋯ → **Restart**.
3. Ako ne možeš ništa napraviti odmah, javi korisniku da aplikacija privremeno ne radi.
   Podaci se ne gube: u bazi su, a backup ide svaku noć.

## 2. Backup baze

- Svaku noć u 3:00 UTC (4:00 ili 5:00 po našem vremenu) servis **Backup** na Railwayu
  napravi `pg_dump` i spremi ga u Cloudflare R2, bucket **`wedding-backup`**.
- Datoteka: `backup-<datum>.tar.gz` = `pg_dump` u tar formatu, zapakiran gzipom.
- Backup sadrži prave podatke klijenata: ne stavljati ga u git ni slati drugima.
  Lokalno ga držati samo u mapi `build/` (nije u gitu) i obrisati nakon upotrebe.

## 3. Provjera backupa (isprobano 9. 11. 2026.)

Vraća backup u privremenu bazu u Dockeru, bez diranja prave baze. Treba Docker Desktop.

1. Cloudflare → R2 → `wedding-backup` → preuzmi najnoviju datoteku u `build/`.
2. U glavnoj mapi projekta (zamijeni ime datoteke):

```
docker run -d --name backup-test -e POSTGRES_PASSWORD=test postgres:18
docker cp build\backup-DATUM.tar.gz backup-test:/backup.tar.gz
docker exec backup-test sh -c "gunzip -c /backup.tar.gz > /backup.tar && pg_restore -U postgres -d postgres --no-owner --no-privileges /backup.tar"
docker exec backup-test psql -U postgres -c "select count(*) as vjencanja from vjencanje" -c "select count(*) as uplate from uplata" -c "select version, description from flyway_schema_history order by installed_rank"
docker rm -f backup-test
```

3. Brojke moraju odgovarati stanju u aplikaciji na dan backupa, a sve migracije moraju biti na popisu.
4. Obriši backup iz `build/`.

Preporuka: ponoviti svakih par mjeseci.

## 4. Vraćanje backupa u pravu bazu (nije isprobano)

Samo ako je baza na Railwayu izgubljena ili pokvarena.

1. Na Railwayu napravi novu Postgres bazu, ako stara ne postoji, i u servisu
   `wedding-manager-api` poveži varijable `SPRING_DATASOURCE_*` na nju.
2. U servisu Postgres → **Variables** kopiraj `DATABASE_PUBLIC_URL`.
   To je tajna: ne spremati je u kod ni slati u chat.
3. Preuzmi backup u `build/` i u glavnoj mapi projekta pokreni (PowerShell):

```
$env:DB_URL = "<zalijepi DATABASE_PUBLIC_URL>"
docker run --rm -e DB_URL -v "${PWD}\build:/b" postgres:18 sh -c 'gunzip -c /b/backup-DATUM.tar.gz > /tmp/b.tar && pg_restore -d "$DB_URL" --no-owner --no-privileges --clean --if-exists /tmp/b.tar'
Remove-Item Env:DB_URL
```

4. Restartaj `wedding-manager-api`. Flyway vidi da su migracije već tu i pokreće samo novije.
5. Provjeri `https://app.brzicaweddings.com/actuator/health` i prijavu u aplikaciju.
6. Podaci upisani nakon zadnjeg backupa su izgubljeni: dogovori s korisnikom da ih ponovno upiše.
