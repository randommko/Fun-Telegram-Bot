package org.example.fun.Chats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.telegram.telegrambots.meta.api.objects.Chat;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.fun.TablesDB.TG_CHATS_TABLE;

@Repository
public class ChatsRepository {
    private final Logger logger = LoggerFactory.getLogger(ChatsRepository.class);
    private final DataSource dataSource;

    public ChatsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insertChatInDB(Chat chat) {
        try (Connection connection = dataSource.getConnection()) {
            String insertChatQuery = "INSERT INTO " + TG_CHATS_TABLE
                    + " (chat_id, chat_title) VALUES (?, ?) ON CONFLICT (chat_id) DO NOTHING";
            try (PreparedStatement insertChat = connection.prepareStatement(insertChatQuery)) {
                insertChat.setLong(1, chat.getId());
                insertChat.setString(2, title(chat));
                insertChat.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при сохранении чата в БД: ", e);
        }
    }

    public Chat getChatByID(Long chatID) {
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT chat_id, chat_title FROM " + TG_CHATS_TABLE + " WHERE chat_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setLong(1, chatID);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    Chat chat = new Chat();
                    chat.setId(resultSet.getLong("chat_id"));
                    chat.setTitle(resultSet.getString("chat_title"));
                    return chat;
                }
                logger.error("ID чата не найден в БД");
                return null;
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске в БД чата: ", e);
            return null;
        }
    }

    public void updateChatInDB(Chat chat) {
        try (Connection connection = dataSource.getConnection()) {
            String updateChatQuery = "UPDATE " + TG_CHATS_TABLE + " SET " +
                    "chat_title = ?, " +
                    "updated_at = now() " +
                    // Обновляем не чаще раза в сутки: отметка живёт в БД, а не в памяти процесса.
                    "WHERE chat_id = ? AND updated_at::date IS DISTINCT FROM CURRENT_DATE";
            try (PreparedStatement updateChat = connection.prepareStatement(updateChatQuery)) {
                updateChat.setString(1, title(chat));
                updateChat.setLong(2, chat.getId());
                updateChat.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Ошибка при обновлении чата в БД: ", e);
        }
    }

    /** Название группы; для личного чата собирается из имени и фамилии без «null null». */
    private String title(Chat chat) {
        if (chat.getTitle() != null && !chat.getTitle().isBlank())
            return chat.getTitle();

        String name = Stream.of(chat.getFirstName(), chat.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return name.isBlank() ? "Личный чат" : "Личный чат с: " + name;
    }
}
