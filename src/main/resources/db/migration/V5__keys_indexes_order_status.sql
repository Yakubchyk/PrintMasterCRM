-- 1. account_roles: у таблицы не было первичного ключа (возможны дубликаты ролей).
--    Сначала удаляем уже существующие дубликаты, затем добавляем ключ.
DELETE FROM account_roles a
    USING account_roles b
WHERE a.ctid > b.ctid
  AND a.account_id = b.account_id
  AND a.roles = b.roles;

ALTER TABLE account_roles
    ADD CONSTRAINT pk_account_roles PRIMARY KEY (account_id, roles);

-- 2. Индексы по внешним ключам и по полю, по которому ищут клиентов менеджера
CREATE INDEX idx_customer_account_id ON customer (account_id);
CREATE INDEX idx_customer_manager_username ON customer (manager_username);
CREATE INDEX idx_post_press_customer_id ON post_press (customer_id);
CREATE INDEX idx_post_press_manager_id ON post_press (manager_id);
CREATE INDEX idx_printing_customer_id ON printing (customer_id);

-- 3. Статус заказа: обязателен, по умолчанию NEW. Старые заказы без статуса считаем новыми.
UPDATE customer_orders
SET status = 'NEW'
WHERE status IS NULL;

ALTER TABLE customer_orders
    ALTER COLUMN status SET DEFAULT 'NEW';

ALTER TABLE customer_orders
    ALTER COLUMN status SET NOT NULL;
