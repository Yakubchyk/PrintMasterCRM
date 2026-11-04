-- Приложение всегда читает настройки цен из строки с id = 1.
-- 1) если в таблице уже есть другие строки, но нет id = 1, переносим в id = 1 данные самой свежей;
-- 2) если таблица пуста, создаём строку с нулевыми ценами (их задаёт администратор в админ-панели).
INSERT INTO setting (id, montage_work_price, one_ottisk_price, one_quadrat_metter_foil_price, price_print)
SELECT 1, montage_work_price, one_ottisk_price, one_quadrat_metter_foil_price, price_print
FROM setting
WHERE NOT EXISTS (SELECT 1 FROM setting WHERE id = 1)
ORDER BY id DESC
LIMIT 1;

INSERT INTO setting (id, montage_work_price, one_ottisk_price, one_quadrat_metter_foil_price, price_print)
SELECT 1, 0, 0, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM setting);
