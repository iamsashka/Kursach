-- data.sql
-- Хранимые процедуры для PostgreSQL

-- Процедура 1: Расчет выручки за период
CREATE OR REPLACE FUNCTION calculate_revenue_by_period(
    start_date DATE,
    end_date DATE
)
RETURNS DECIMAL AS $$
DECLARE
    total_revenue DECIMAL;
BEGIN
    SELECT COALESCE(SUM(total_amount), 0) INTO total_revenue
    FROM orders
    WHERE order_date BETWEEN start_date AND end_date
    AND status = 'DELIVERED'
    AND deleted = false;

    RETURN total_revenue;
END;
$$ LANGUAGE plpgsql;

-- Процедура 2: Обновление рейтинга товара
CREATE OR REPLACE FUNCTION update_product_rating(product_id BIGINT)
RETURNS VOID AS $$
BEGIN
    UPDATE products
    SET rating = (
        SELECT COALESCE(AVG(rating), 0)
        FROM reviews
        WHERE product_id = $1
        AND approved = true
    ),
    review_count = (
        SELECT COUNT(*)
        FROM reviews
        WHERE product_id = $1
        AND approved = true
    )
    WHERE id = $1 AND deleted = false;
END;
$$ LANGUAGE plpgsql;

-- Процедура 3: Архивация старых корзин
CREATE OR REPLACE FUNCTION archive_old_cart_items(days_old INT)
RETURNS VOID AS $$
BEGIN
    -- Создаем таблицу для архива если не существует
    CREATE TABLE IF NOT EXISTS cart_items_archive AS TABLE cart_items WITH NO DATA;

    -- Архивируем старые записи
    INSERT INTO cart_items_archive
    SELECT *, NOW() FROM cart_items
    WHERE updated_at < (NOW() - (days_old || ' days')::INTERVAL);

    -- Удаляем архивированные записи
    DELETE FROM cart_items
    WHERE updated_at < (NOW() - (days_old || ' days')::INTERVAL);
END;
$$ LANGUAGE plpgsql;

-- Триггер для аудита изменений пользователей
CREATE OR REPLACE FUNCTION audit_user_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.email IS DISTINCT FROM NEW.email OR OLD.enabled IS DISTINCT FROM NEW.enabled THEN
        INSERT INTO audit_logs (user_id, username, action, entity_type, entity_id, old_values, new_values, created_at)
        VALUES (
            NEW.id,
            NEW.username,
            'UPDATE',
            'USER',
            NEW.id,
            CONCAT('email:', OLD.email, ',enabled:', OLD.enabled),
            CONCAT('email:', NEW.email, ',enabled:', NEW.enabled),
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Создаем триггер
DROP TRIGGER IF EXISTS audit_user_changes_trigger ON users;
CREATE TRIGGER audit_user_changes_trigger
    AFTER UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION audit_user_changes();