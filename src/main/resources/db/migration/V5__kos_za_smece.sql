-- Koš za smeće: obrisano vjenčanje dobije vrijeme brisanja i 30 dana se može vratiti.
ALTER TABLE vjencanje ADD COLUMN obrisano TIMESTAMP WITH TIME ZONE;
CREATE INDEX vjencanje_tvrtka_obrisano_idx ON vjencanje (tvrtka_id, obrisano);

-- Kod trajnog brisanja baza sama briše i sve što pripada vjenčanju.
ALTER TABLE vjencanje_dodatak DROP CONSTRAINT vjencanje_dodatak_vjencanje_id_fkey,
    ADD CONSTRAINT vjencanje_dodatak_vjencanje_id_fkey FOREIGN KEY (vjencanje_id) REFERENCES vjencanje (id) ON DELETE CASCADE;
ALTER TABLE stavka_rasporeda DROP CONSTRAINT stavka_rasporeda_vjencanje_id_fkey,
    ADD CONSTRAINT stavka_rasporeda_vjencanje_id_fkey FOREIGN KEY (vjencanje_id) REFERENCES vjencanje (id) ON DELETE CASCADE;
ALTER TABLE clan_ekipe DROP CONSTRAINT clan_ekipe_vjencanje_id_fkey,
    ADD CONSTRAINT clan_ekipe_vjencanje_id_fkey FOREIGN KEY (vjencanje_id) REFERENCES vjencanje (id) ON DELETE CASCADE;
ALTER TABLE uplata DROP CONSTRAINT uplata_vjencanje_id_fkey,
    ADD CONSTRAINT uplata_vjencanje_id_fkey FOREIGN KEY (vjencanje_id) REFERENCES vjencanje (id) ON DELETE CASCADE;
ALTER TABLE zadatak DROP CONSTRAINT zadatak_vjencanje_id_fkey,
    ADD CONSTRAINT zadatak_vjencanje_id_fkey FOREIGN KEY (vjencanje_id) REFERENCES vjencanje (id) ON DELETE CASCADE;
