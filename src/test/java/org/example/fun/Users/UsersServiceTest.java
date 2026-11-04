package org.example.fun.Users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private UsersRepository repo;

    @InjectMocks
    private UsersService service;

    @Test
    void addsUserWhenMissing() {
        User user = user(1L, "Имя", "nick");
        when(repo.getUserByID(1L)).thenReturn(null);

        assertThat(service.checkUser(user)).isFalse();
        service.addUser(user);

        verify(repo).insertUserInDB(user);
    }

    @Test
    void updatesUserWhenExists() {
        User user = user(1L, "Имя", "nick");
        when(repo.getUserByID(1L)).thenReturn(user);

        assertThat(service.checkUser(user)).isTrue();
        service.updateUser(user);

        verify(repo).updateUserInDB(user);
    }

    @Test
    void prefersNickForStatistics() {
        when(repo.getUserByID(1L)).thenReturn(user(1L, "Имя", "@nick"));

        assertThat(service.getUserNameByID(1L)).isEqualTo("@nick");
    }

    @Test
    void fallsBackToFirstNameWithoutNick() {
        when(repo.getUserByID(1L)).thenReturn(user(1L, "Имя", null));

        assertThat(service.getUserNameByID(1L)).isEqualTo("Имя");
    }

    @Test
    void returnsPlaceholderForUnknownUser() {
        when(repo.getUserByID(1L)).thenReturn(null);

        assertThat(service.getUserNameByID(1L)).isEqualTo("неизвестный");
    }

    private User user(Long id, String firstName, String userName) {
        return new User(id, firstName, false, null, userName, "ru",
                null, null, null, false, false);
    }
}
