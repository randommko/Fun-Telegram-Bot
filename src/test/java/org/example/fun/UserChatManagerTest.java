package org.example.fun;

import org.example.fun.Chats.ChatsService;
import org.example.fun.Users.UsersService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserChatManagerTest {

    @Mock
    private UsersService usersService;
    @Mock
    private ChatsService chatsService;

    @InjectMocks
    private UserChatManager manager;

    @Test
    void addsUnknownUser() {
        User user = user(1L);
        when(usersService.checkUser(user)).thenReturn(false);

        manager.checkUser(user);

        verify(usersService).addUser(user);
        verify(usersService, never()).updateUser(user);
    }

    @Test
    void updatesKnownUser() {
        User user = user(1L);
        when(usersService.checkUser(user)).thenReturn(true);

        manager.checkUser(user);

        verify(usersService).updateUser(user);
        verify(usersService, never()).addUser(user);
    }

    @Test
    void addsUnknownChat() {
        Chat chat = chat(10L);
        when(chatsService.checkChat(10L)).thenReturn(false);

        manager.checkChat(chat);

        verify(chatsService).addChat(chat);
        verify(chatsService, never()).updateChat(chat);
    }

    @Test
    void updatesKnownChat() {
        Chat chat = chat(10L);
        when(chatsService.checkChat(10L)).thenReturn(true);

        manager.checkChat(chat);

        verify(chatsService).updateChat(chat);
        verify(chatsService, never()).addChat(chat);
    }

    private User user(Long id) {
        return new User(id, "Имя", false, null, "nick", "ru",
                null, null, null, false, false);
    }

    private Chat chat(Long id) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setTitle("Чат");
        return chat;
    }
}
