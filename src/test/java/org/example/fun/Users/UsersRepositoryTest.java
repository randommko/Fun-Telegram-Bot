package org.example.fun.Users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersRepositoryTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;

    private UsersRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        repo = new UsersRepository(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
    }

    @Test
    void insertsIntoSharedUsers() throws Exception {
        repo.insertUserInDB(user("nick"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("INSERT INTO shared.tg_users");
        verify(statement).setString(5, "@nick");
    }

    @Test
    void updatesSharedUsers() throws Exception {
        repo.updateUserInDB(user("nick"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("UPDATE shared.tg_users");
        assertThat(sql.getValue()).contains("updated_at::date IS DISTINCT FROM CURRENT_DATE");
    }

    @Test
    void writesNullNickForUserWithoutUserName() throws Exception {
        repo.insertUserInDB(user(null));

        verify(statement).setString(5, null);
    }

    private User user(String userName) {
        return new User(1L, "Имя", false, null, userName, "ru",
                null, null, null, false, false);
    }
}
