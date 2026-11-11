-- Podaci tvrtke koji idu u zaglavlje ugovora. Nisu obavezni.
ALTER TABLE tvrtka ADD COLUMN adresa VARCHAR(255);
ALTER TABLE tvrtka ADD COLUMN oib VARCHAR(11);
ALTER TABLE tvrtka ADD COLUMN telefon VARCHAR(50);
ALTER TABLE tvrtka ADD COLUMN email VARCHAR(255);

-- Vlastiti Word predložak ugovora; tvrtka bez njega koristi zadani iz aplikacije
CREATE TABLE predlozak_ugovora (
    tvrtka_id      BIGINT PRIMARY KEY REFERENCES tvrtka (id) ON DELETE CASCADE,
    naziv_datoteke VARCHAR(255)             NOT NULL,
    sadrzaj        BYTEA                    NOT NULL,
    azurirano      TIMESTAMP WITH TIME ZONE NOT NULL
);
