package org.example.fun;

import org.example.fun.Chats.ChatsService;
import org.example.fun.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * Ведёт общих пользователей и чаты в схеме shared.
 * Отметка «обновляли сегодня» хранится в БД, поэтому рестарт процесса её не сбрасывает.
 */
@Component
public class UserChatManager {
    private static final Logger logger = LoggerFactory.getLogger(UserChatManager.class);

    private final UsersService usersService;
    private final ChatsService chatsService;

    public UserChatManager(UsersService usersService, ChatsService chatsService) {
        this.usersService = usersService;
        this.chatsService = chatsService;
    }

    public void checkUser(User user) {
        if (!usersService.checkUser(user))
            usersService.addUser(user);
        else
            usersService.updateUser(user);
    }

    public void checkChat(Chat chat) {
        if (!chatsService.checkChat(chat.getId()))
            chatsService.addChat(chat);
        else
            chatsService.updateChat(chat);
    }
}
