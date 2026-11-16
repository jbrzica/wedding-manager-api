-- Kratka fotografiranja s rezervacijom preko javnog linka (npr. Božićno fotografiranje).
-- Potpuno odvojeno od vjenčanja.
CREATE TABLE dogadjaj_termina (
    id        BIGSERIAL PRIMARY KEY,
    tvrtka_id BIGINT       NOT NULL REFERENCES tvrtka (id) ON DELETE CASCADE,
    naziv     VARCHAR(200) NOT NULL,
    opis      VARCHAR(2000),
    lokacija  VARCHAR(255),
    -- Razmak između početaka termina; nijedan paket ne traje dulje, pa svaki termin zauzme točno jedno mjesto
    korak_minuta INT       NOT NULL DEFAULT 30 CHECK (korak_minuta BETWEEN 5 AND 240),
    javni_kod VARCHAR(64)  NOT NULL UNIQUE,
    otvoren   BOOLEAN      NOT NULL DEFAULT TRUE,
    kreirano  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_dogadjaj_termina_tvrtka ON dogadjaj_termina (tvrtka_id);

CREATE TABLE paket_termina (
    id              BIGSERIAL PRIMARY KEY,
    dogadjaj_id     BIGINT        NOT NULL REFERENCES dogadjaj_termina (id) ON DELETE CASCADE,
    naziv           VARCHAR(100)  NOT NULL,
    opis            VARCHAR(500),
    trajanje_minuta INT           NOT NULL CHECK (trajanje_minuta > 0),
    cijena          NUMERIC(10, 2) NOT NULL CHECK (cijena >= 0),
    redoslijed      INT           NOT NULL DEFAULT 0
);

-- Kad se radi: jedan red po danu (od-do)
CREATE TABLE dan_termina (
    id          BIGSERIAL PRIMARY KEY,
    dogadjaj_id BIGINT NOT NULL REFERENCES dogadjaj_termina (id) ON DELETE CASCADE,
    datum       DATE   NOT NULL,
    od          TIME   NOT NULL,
    do_vremena  TIME   NOT NULL,
    CHECK (do_vremena > od)
);

CREATE TABLE rezervacija_termina (
    id              BIGSERIAL PRIMARY KEY,
    dogadjaj_id     BIGINT        NOT NULL REFERENCES dogadjaj_termina (id) ON DELETE CASCADE,
    paket_id        BIGINT        REFERENCES paket_termina (id) ON DELETE SET NULL,
    -- Naziv, trajanje i cijena prepisani u trenutku rezervacije: kasnija promjena paketa ih ne mijenja
    naziv_paketa    VARCHAR(100)  NOT NULL,
    trajanje_minuta INT           NOT NULL,
    cijena          NUMERIC(10, 2) NOT NULL,
    datum           DATE          NOT NULL,
    vrijeme         TIME          NOT NULL,
    ime             VARCHAR(200)  NOT NULL,
    telefon         VARCHAR(50)   NOT NULL,
    email           VARCHAR(255),
    napomena        VARCHAR(1000),
    -- Hash tajnog koda iz linka za otkazivanje u mailu
    otkazni_hash    VARCHAR(64)   UNIQUE,
    rucno           BOOLEAN       NOT NULL DEFAULT FALSE,
    kreirano        TIMESTAMP WITH TIME ZONE NOT NULL,
    otkazano        TIMESTAMP WITH TIME ZONE
);
-- Isti termin ne mogu imati dvije aktivne rezervacije, ni kad dvoje klikne u istoj sekundi
CREATE UNIQUE INDEX uq_rezervacija_termina_aktivna
    ON rezervacija_termina (dogadjaj_id, datum, vrijeme) WHERE otkazano IS NULL;
