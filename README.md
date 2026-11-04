# Fun-Telegram-Bot

Независимый бот для развлечений: замер достоинства, «Пидор дня» и гороскоп. Не использует код AI-бота и админки, не читает их схемы и не обращается к модели.

## Команды

- `/cocksize` — измерить достоинство (один замер в сутки)
- `/pidor_reg` — вступить в игру «Пидор дня»
- `/pidor_stats` — статистика побед в чате
- `/pidor_start` — выбрать победителя дня
- `/horoscope_today` — гороскоп на сегодня
- `/bot_info` — список команд

Сообщения без слэша и команды других ботов игнорируются.

## Требования

- JDK 17
- Maven 3.8+
- PostgreSQL с схемами `shared` и `fun`

## Сборка

```
mvn clean package
```

Результат — `target/fun-telegram-bot-1.0.jar`.

## Конфигурация

Только переменные окружения; позиционные аргументы не используются.

| Переменная | Значение |
|---|---|
| `BOT_TOKEN` | токен Fun-бота из BotFather |
| `BOT_DB_URL` | JDBC-URL базы |
| `BOT_DB_USER` | роль `fun_user` |
| `BOT_DB_PASSWORD` | пароль роли |
| `BOT_OWNER_CHAT_ID` | Telegram ID владельца для уведомления о запуске |
| `FUN_RESOURCES_PATH` | каталог картинок замера, по умолчанию `/bin/tg_bot/resources` |
| `BOT_PROXY_TYPE/HOST/PORT/USER/PASS` | прокси для Telegram, опционален |

Windows (cmd):

```
set BOT_TOKEN=<token>
set BOT_DB_URL=jdbc:postgresql://host:5432/db
set BOT_DB_USER=fun_user
set BOT_DB_PASSWORD=<password>
set BOT_OWNER_CHAT_ID=<telegram user id>
java -jar target/fun-telegram-bot-1.0.jar
```

Linux:

```
BOT_TOKEN=<token> BOT_DB_URL=jdbc:postgresql://host:5432/db \
BOT_DB_USER=fun_user BOT_DB_PASSWORD=<password> \
BOT_OWNER_CHAT_ID=<telegram user id> \
java -jar target/fun-telegram-bot-1.0.jar
```

Обязательные переменные проверяются при старте: без них приложение завершается с ненулевым кодом и именем переменной.

## База данных

Схема `shared` с `tg_users` и `tg_chats` создаётся внешним набором скриптов — Fun её не создаёт и не меняет структуру.

Миграции Fun:

```
psql "$BOT_DB_URL" -f migrations/fun/001_init_schema.sql
```

Миграция идемпотентна. Она создаёт схему `fun`, таблицы `pidor_messages`, `pidor_players`, `pidor_stats`, `cocksize_stats`, `cocksize_imgs`, индексы «один победитель в день» и «один замер в сутки», а также выдаёт права роли `fun_user`, если она существует.

## Запуск рядом с AI-ботом

В старых чатах Fun-бот не работает: игры переносятся в новые чаты. Накопленная статистика остаётся с прежними `chat_id`, в новых чатах счёт начинается с нуля. AI-бот и админка в этих чатах не участвуют.

## Деплой

- unit: `deploy/fun-telegram-bot.service`
- шаблон окружения: `deploy/fun.env.example`
- CI: `.github/workflows/deploy.yml`, пуш в `main` собирает и перезапускает `fun-telegram-bot`

Секреты GitHub: `SSH_PRIVATE_KEY`, `SERVER_IP`, `SERVER_USER`, `TOKEN_TG_FUN_BOT`, `DB_ADDR_FUN_BOT`, `DB_USER_FUN_BOT`, `DB_PASS_FUN_BOT`, `FUN_OWNER_CHAT_ID`, `FUN_RESOURCES_PATH`, `BOT_PROXY_*`.

Картинки замера лежат в `FUN_RESOURCES_PATH`; при выкатке каталог должен существовать.
