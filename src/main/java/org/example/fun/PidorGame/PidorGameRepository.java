package org.example.fun.PidorGame;

import org.example.fun.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.example.fun.TablesDB.PIDOR_MESSAGES_TABLE;
import static org.example.fun.TablesDB.PIDOR_PLAYERS_TABLE;
import static org.example.fun.TablesDB.PIDOR_STATS_TABLE;
import static org.example.fun.TablesDB.TG_USERS_TABLE;

@Repository
public class PidorGameRepository {
    private final Logger logger = LoggerFactory.getLogger(PidorGameRepository.class);
    private final UsersService usersService;
    private final DataSource dataSource;

    public PidorGameRepository(DataSource dataSource, UsersService usersService) {
        this.dataSource = dataSource;
        this.usersService = usersService;
    }

    public void setPidorWinner(Long chatID, Long userID) {
        LocalDate today = LocalDate.now();
        String insertQuery = "INSERT INTO " + PIDOR_STATS_TABLE + " (chat_id, date, user_id) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setLong(1, chatID);
            insertStmt.setDate(2, Date.valueOf(today));
            insertStmt.setLong(3, userID);
            insertStmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Ошибка при записи победителя в БД: ", e);
        }
    }

    public Set<Long> getPidorGamePlayers(Long chatID) {
        Set<Long> chatPlayers = new HashSet<>();
        String query = "SELECT user_id FROM " + PIDOR_PLAYERS_TABLE + " WHERE chat_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, chatID);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    chatPlayers.add(rs.getLong("user_id"));
            }
        } catch (Exception e) {
            logger.error("Ошибка получения списка игроков из БД: ", e);
        }
        return chatPlayers;
    }

    /** Возвращает 1 для новой записи и 0, если игрок уже был зарегистрирован. */
    public Integer registerPlayer(Long chatID, Long userID) {
        String insertQuery = "INSERT INTO " + PIDOR_PLAYERS_TABLE
                + " (chat_id, user_id) VALUES (?, ?) ON CONFLICT (chat_id, user_id) DO NOTHING";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setLong(1, chatID);
            preparedStatement.setLong(2, userID);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка при регистрации игрока в БД: {}", String.valueOf(e));
            return 0;
        }
    }

    public Map<String, Integer> getPidorStats(Long chatID) {
        Map<String, Integer> winnersList = new HashMap<>();
        String query = "SELECT pst.user_id, COUNT(*) AS count " +
                "FROM " + PIDOR_STATS_TABLE +
                " AS pst JOIN " + TG_USERS_TABLE +
                " AS tut ON pst.user_id = tut.user_id " +
                "WHERE pst.chat_id = ? " +
                "GROUP BY pst.user_id";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, chatID);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next())
                winnersList.put(
                        usersService.getUserNameByID(resultSet.getLong("user_id")),
                        resultSet.getInt("count")
                );
        } catch (Exception e) {
            logger.error("Ошибка получения статистики игр: {}", String.valueOf(e));
        }
        return winnersList;
    }

    public Long getTodayWinner(Long chatID) {
        LocalDate today = LocalDate.now();
        String checkQuery = "SELECT user_id FROM " + PIDOR_STATS_TABLE + " WHERE chat_id = ? AND date = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setLong(1, chatID);
            checkStmt.setDate(2, Date.valueOf(today));
            try (ResultSet resultSet = checkStmt.executeQuery()) {
                if (resultSet.next())
                    return resultSet.getLong("user_id");
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке состоявшейся игры: ", e);
        }
        return null;
    }

    public String getWinnerResponse() {
        String sql = "SELECT text FROM " + PIDOR_MESSAGES_TABLE
                + " WHERE group_num = 100 ORDER BY RANDOM() LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next())
                return rs.getString("text");
        } catch (Exception e) {
            logger.error("Ошибка при получении текста победителя из БД: ", e);
        }
        return "Победитель сегодняшней игры: ";
    }

    public List<String> getRandomResponses() {
        String sql = "SELECT text FROM " + PIDOR_MESSAGES_TABLE
                + " WHERE group_num = ? ORDER BY RANDOM() LIMIT 1";
        List<String> responses = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            for (int groupNum = 1; groupNum <= 3; groupNum++) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, groupNum);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next())
                            responses.add(rs.getString("text"));
                        else
                            responses.add("Запасной текст для группы " + groupNum);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при получении текста сообщений из БД: ", e);
            return List.of("Подготовка...", "Скоро узнаем...", "Держитесь крепче...");
        }

        return responses;
    }
}
