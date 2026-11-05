-- Фольга на складе: тип фольги; материал (фольга) в расчёте тиснения.
ALTER TABLE material ADD COLUMN foil_type VARCHAR(30);
ALTER TABLE post_press ADD COLUMN material_id BIGINT;
ALTER TABLE post_press ADD CONSTRAINT fk_post_press_material FOREIGN KEY (material_id) REFERENCES material (id);

-- Заказ «Выполнен» вместо «Отгружен»: списание материалов происходит при выполнении.
ALTER TABLE customer_orders ADD COLUMN completed_at TIMESTAMP;
UPDATE customer_orders SET status = 'DONE' WHERE status = 'SHIPPED';
UPDATE customer_orders SET completed_at = created_at WHERE status = 'DONE';
