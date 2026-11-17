-- Kratko ime za link, npr. "bozicno": app.../r/bozicno i bozicno.brzicaweddings.com vode na rezervaciju
ALTER TABLE dogadjaj_termina ADD COLUMN kratki_link VARCHAR(40) UNIQUE
    CHECK (kratki_link ~ '^[a-z0-9]([a-z0-9-]*[a-z0-9])?$');
