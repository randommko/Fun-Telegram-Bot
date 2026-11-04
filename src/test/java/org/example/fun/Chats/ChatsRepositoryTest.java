package org.example.fun.Chats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatsRepositoryTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet resultSet;

    private ChatsRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        repo = new ChatsRepository(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
    }

    @Test
    void insertsIntoSharedChats() throws Exception {
        repo.insertChatInDB(chat(10L, "Чат"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("INSERT INTO shared.tg_chats");
        verify(statement).setString(2, "Чат");
    }

    @Test
    void updatesSharedChats() throws Exception {
        repo.updateChatInDB(chat(10L, "Чат"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("UPDATE shared.tg_chats");
    }

    @Test
    void readsTitleFromSharedChats() throws Exception {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("chat_id")).thenReturn(10L);
        when(resultSet.getString("chat_title")).thenReturn("Чат");

        Chat chat = repo.getChatByID(10L);

        assertThat(chat).isNotNull();
        assertThat(chat.getId()).isEqualTo(10L);
        assertThat(chat.getTitle()).isEqualTo("Чат");
    }

    private Chat chat(Long id, String title) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setTitle(title);
        return chat;
    }
}
