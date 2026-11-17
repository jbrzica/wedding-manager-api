-- Naslovna slika javne stranice za rezervaciju; posebna tablica da se bajtovi ne učitavaju uz svaki događaj
CREATE TABLE slika_termina (
    dogadjaj_id BIGINT PRIMARY KEY REFERENCES dogadjaj_termina (id) ON DELETE CASCADE,
    tip         VARCHAR(20)              NOT NULL,
    sadrzaj     BYTEA                    NOT NULL,
    azurirano   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Raste kod svake promjene slike: dio adrese slike, pa preglednik smije dugo čuvati staru verziju u cacheu
ALTER TABLE dogadjaj_termina ADD COLUMN slika_verzija INT NOT NULL DEFAULT 0;
