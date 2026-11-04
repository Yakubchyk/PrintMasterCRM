-- Адрес клиента.
ALTER TABLE customer ADD COLUMN postal_code VARCHAR(20);
ALTER TABLE customer ADD COLUMN city VARCHAR(100);
ALTER TABLE customer ADD COLUMN street VARCHAR(150);
ALTER TABLE customer ADD COLUMN house_number VARCHAR(20);

-- Клише: технологические поля (на сторону) и материал, из которого оно делается.
ALTER TABLE cliche ADD COLUMN margin_cm NUMERIC(19, 3) DEFAULT 0 NOT NULL;
ALTER TABLE cliche ADD COLUMN material_id BIGINT;
ALTER TABLE cliche ADD CONSTRAINT fk_cliche_material FOREIGN KEY (material_id) REFERENCES material (id);

-- Склад: резерв под заказы; списание при отгрузке.
ALTER TABLE material ADD COLUMN reserved NUMERIC(19, 3) DEFAULT 0 NOT NULL;
ALTER TABLE order_material ADD COLUMN written_off BOOLEAN DEFAULT TRUE NOT NULL;

-- Описание состава заказа (снимок расчётов на момент оформления).
ALTER TABLE customer_orders ADD COLUMN details VARCHAR(4000);
