-- Магний и латунь: размер исходного листа (см), чтобы вести остаток в см² по листам.
ALTER TABLE material ADD COLUMN sheet_width_cm NUMERIC(12, 3);
ALTER TABLE material ADD COLUMN sheet_length_cm NUMERIC(12, 3);
