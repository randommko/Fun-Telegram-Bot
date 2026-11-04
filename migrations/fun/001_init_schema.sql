-- Схема fun для независимого Fun-бота.
-- Предпосылка: схема shared и таблицы shared.tg_users, shared.tg_chats уже созданы
-- внешним набором скриптов (DDL общей схемы живёт вне репозиториев).
-- Миграция идемпотентна: безопасно накатывать повторно.
SET client_encoding TO 'UTF8';

CREATE SCHEMA IF NOT EXISTS fun;

CREATE TABLE IF NOT EXISTS fun.pidor_messages (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    text text NOT NULL,
    group_num integer NOT NULL,
    created_at date NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS fun.pidor_players (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_id bigint NOT NULL,
    user_id bigint NOT NULL REFERENCES shared.tg_users(user_id),
    created_at timestamp with time zone DEFAULT now(),
    CONSTRAINT pidor_players_un UNIQUE (user_id, chat_id)
);

CREATE TABLE IF NOT EXISTS fun.pidor_stats (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chat_id bigint NOT NULL,
    date date NOT NULL,
    user_id bigint NOT NULL REFERENCES shared.tg_users(user_id),
    created_at timestamp with time zone DEFAULT now()
);

-- Один победитель на чат и дату: старая схема допускала второго победителя.
DELETE FROM fun.pidor_stats older
    USING fun.pidor_stats newer
    WHERE older.chat_id = newer.chat_id
      AND older.date = newer.date
      AND older.id > newer.id;
CREATE UNIQUE INDEX IF NOT EXISTS pidor_stats_chat_date_un ON fun.pidor_stats (chat_id, date);

CREATE TABLE IF NOT EXISTS fun.cocksize_stats (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES shared.tg_users(user_id),
    size integer NOT NULL,
    date date NOT NULL,
    created_at timestamp with time zone DEFAULT now()
);

-- Один замер на пользователя в сутки.
DELETE FROM fun.cocksize_stats older
    USING fun.cocksize_stats newer
    WHERE older.user_id = newer.user_id
      AND older.date = newer.date
      AND older.id > newer.id;
CREATE UNIQUE INDEX IF NOT EXISTS cocksize_stats_user_date_un ON fun.cocksize_stats (user_id, date);

CREATE TABLE IF NOT EXISTS fun.cocksize_imgs (
    cock_size integer PRIMARY KEY,
    img text,
    is_using boolean NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS pidor_players_chat_idx ON fun.pidor_players (chat_id);
CREATE INDEX IF NOT EXISTS pidor_messages_group_idx ON fun.pidor_messages (group_num);

-- Права роли Fun. Если роли ещё нет, блок ничего не делает.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'fun_user') THEN
        EXECUTE 'GRANT USAGE ON SCHEMA fun TO fun_user';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA fun TO fun_user';
        EXECUTE 'GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA fun TO fun_user';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA fun GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO fun_user';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA fun GRANT USAGE, SELECT ON SEQUENCES TO fun_user';

        EXECUTE 'GRANT USAGE ON SCHEMA shared TO fun_user';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON shared.tg_users TO fun_user';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON shared.tg_chats TO fun_user';
    END IF;
END $$;
