-- Добавление полей настроек пользователя
ALTER TABLE users
ADD COLUMN IF NOT EXISTS user_theme VARCHAR(20) DEFAULT 'light',
ADD COLUMN IF NOT EXISTS date_format VARCHAR(20) DEFAULT 'dd.MM.yyyy',
ADD COLUMN IF NOT EXISTS number_format VARCHAR(20) DEFAULT 'COMMA',
ADD COLUMN IF NOT EXISTS page_size INTEGER DEFAULT 10,
ADD COLUMN IF NOT EXISTS saved_filters TEXT;

-- Комментарии к полям для документации
COMMENT ON COLUMN users.user_theme IS 'Тема оформления: light/dark';
COMMENT ON COLUMN users.date_format IS 'Формат даты';
COMMENT ON COLUMN users.number_format IS 'Формат чисел: COMMA/SPACE/DOT';
COMMENT ON COLUMN users.page_size IS 'Количество элементов на странице';
COMMENT ON COLUMN users.saved_filters IS 'Сохранённые фильтры в JSON формате';

-- Индекс для оптимизации
CREATE INDEX IF NOT EXISTS idx_users_theme ON users(user_theme);