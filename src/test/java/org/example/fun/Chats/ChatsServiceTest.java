package org.example.fun.Chats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatsServiceTest {

    @Mock
    private ChatsRepository repo;

    @InjectMocks
    private ChatsService service;

    @Test
    void addsChatWhenMissing() {
        Chat chat = chat(10L, "Чат");
        when(repo.getChatByID(10L)).thenReturn(null);

        assertThat(service.checkChat(10L)).isFalse();
        service.addChat(chat);

        verify(repo).insertChatInDB(chat);
    }

    @Test
    void updatesChatWhenExists() {
        Chat chat = chat(10L, "Чат");
        when(repo.getChatByID(10L)).thenReturn(chat);

        assertThat(service.checkChat(10L)).isTrue();
        service.updateChat(chat);

        verify(repo).updateChatInDB(chat);
    }

    @Test
    void returnsTitleForKnownChat() {
        when(repo.getChatByID(10L)).thenReturn(chat(10L, "Чат"));

        assertThat(service.getChatTitle(10L)).isEqualTo("Чат");
    }

    @Test
    void returnsNullForUnknownChat() {
        when(repo.getChatByID(10L)).thenReturn(null);

        assertThat(service.getChatTitle(10L)).isNull();
    }

    private Chat chat(Long id, String title) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setTitle(title);
        return chat;
    }
}
