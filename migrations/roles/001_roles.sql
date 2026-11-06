-- Роль Fun-бота и её права.
-- Пароль в репозитории не хранится: заменить CHANGE_ME_FUN перед запуском
-- (или создать роль вручную — тогда блок IF NOT EXISTS её не тронет).
-- Права на будущие таблицы задаёт ALTER DEFAULT PRIVILEGES — они действуют
-- для объектов, которые создаст та роль, под которой выполняется этот файл.
SET client_encoding TO 'UTF8';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fun_user') THEN
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', 'fun_user', 'CHANGE_ME_FUN');
    END IF;
END $$;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO fun_user', current_database());
END $$;

-- Fun читает и пишет общих пользователей и чаты, а игровые таблицы — только свои.
GRANT USAGE ON SCHEMA shared, fun TO fun_user;
GRANT SELECT, INSERT, UPDATE ON shared.tg_users, shared.tg_chats TO fun_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA fun TO fun_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA fun TO fun_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA fun
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO fun_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA fun
    GRANT USAGE, SELECT ON SEQUENCES TO fun_user;
