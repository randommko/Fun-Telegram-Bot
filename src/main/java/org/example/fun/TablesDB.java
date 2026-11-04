package org.example.fun;

/**
 * Имена таблиц с явной схемой. Общие таблицы — shared, игровые — fun.
 * Таблиц AI и админки здесь нет: Fun к ним не обращается.
 */
public final class TablesDB {
    public static final String TG_USERS_TABLE = "shared.tg_users";
    public static final String TG_CHATS_TABLE = "shared.tg_chats";

    public static final String PIDOR_MESSAGES_TABLE = "fun.pidor_messages";
    public static final String PIDOR_PLAYERS_TABLE = "fun.pidor_players";
    public static final String PIDOR_STATS_TABLE = "fun.pidor_stats";
    public static final String COCKSIZE_STATS_TABLE = "fun.cocksize_stats";
    public static final String COCKSIZE_IMAGES_TABLE = "fun.cocksize_imgs";

    private TablesDB() {
    }
}
