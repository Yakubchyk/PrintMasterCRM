-- Деньги храним в NUMERIC(19,2), а не в DOUBLE PRECISION.
ALTER TABLE setting
    ALTER COLUMN montage_work_price            TYPE NUMERIC(19, 2) USING ROUND(montage_work_price::numeric, 2),
    ALTER COLUMN one_ottisk_price              TYPE NUMERIC(19, 2) USING ROUND(one_ottisk_price::numeric, 2),
    ALTER COLUMN one_quadrat_metter_foil_price TYPE NUMERIC(19, 2) USING ROUND(one_quadrat_metter_foil_price::numeric, 2),
    ALTER COLUMN price_print                   TYPE NUMERIC(19, 2) USING ROUND(price_print::numeric, 2);

ALTER TABLE printing
    ALTER COLUMN total_cost TYPE NUMERIC(19, 2) USING ROUND(total_cost::numeric, 2);

ALTER TABLE post_press
    ALTER COLUMN total_price TYPE NUMERIC(19, 2) USING ROUND(total_price, 2);
