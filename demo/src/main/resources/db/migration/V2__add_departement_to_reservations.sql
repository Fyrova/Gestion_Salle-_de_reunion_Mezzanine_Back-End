-- Migration script to add 'departement' column to 'reservations' table in PostgreSQL

ALTER TABLE reservations
ADD COLUMN departement VARCHAR(255);
