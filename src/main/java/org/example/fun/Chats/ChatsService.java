package org.example.fun.Chats;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Chat;

@Service
public class ChatsService {
    private final ChatsRepository repo;

    public ChatsService(ChatsRepository repo) {
        this.repo = repo;
    }

    public boolean checkChat(Long chatID) {
        return repo.getChatByID(chatID) != null;
    }

    public void addChat(Chat chat) {
        repo.insertChatInDB(chat);
    }

    public void updateChat(Chat chat) {
        repo.updateChatInDB(chat);
    }

    public String getChatTitle(Long chatId) {
        Chat chat = repo.getChatByID(chatId);
        return chat == null ? null : chat.getTitle();
    }
}
