-- Расчёты тиснения закрепляем за менеджером клиента (раньше manager_id всегда оставался пустым)
UPDATE post_press p
SET manager_id = c.account_id
FROM customer c
WHERE p.customer_id = c.id
  AND p.manager_id IS NULL;

-- manager_username дублировал связь customer.account_id и мог разойтись с ней.
-- Имя менеджера теперь берётся через account.
DROP INDEX IF EXISTS idx_customer_manager_username;
ALTER TABLE customer DROP COLUMN manager_username;
