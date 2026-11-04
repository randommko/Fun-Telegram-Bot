package org.example.fun.CockSize;

import org.example.fun.DTO.AVGCockSizeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.example.fun.TablesDB.COCKSIZE_IMAGES_TABLE;
import static org.example.fun.TablesDB.COCKSIZE_STATS_TABLE;

@Repository
public class CockSizeRepository {
    private final Logger logger = LoggerFactory.getLogger(CockSizeRepository.class);
    private final DataSource dataSource;

    public CockSizeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setCockSizeWinner(Long userID, Integer size) {
        LocalDate today = LocalDate.now();
        String insertQuery = "INSERT INTO " + COCKSIZE_STATS_TABLE + " (user_id, size, date) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setLong(1, userID);
            insertStmt.setInt(2, size);
            insertStmt.setDate(3, Date.valueOf(today));
            insertStmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Ошибка при записи замера в БД: ", e);
        }
    }

    public String getCockSizeImageName(Integer size) {
        String selectQuery = "SELECT img FROM " + COCKSIZE_IMAGES_TABLE
                + " WHERE cock_size = ? AND is_using";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectQuery)) {
            statement.setInt(1, size);
            ResultSet resultImageSet = statement.executeQuery();
            if (resultImageSet.next())
                return resultImageSet.getString("img");
        } catch (Exception e) {
            logger.error("Ошибка при чтении картинки замера: ", e);
        }
        return null;
    }

    public int getTodayPlayerCockSize(Long userID) {
        LocalDate currentDate = LocalDate.now();
        String checkQuery = "SELECT size FROM " + COCKSIZE_STATS_TABLE + " WHERE user_id = ? AND date = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setLong(1, userID);
            checkStmt.setDate(2, Date.valueOf(currentDate));
            ResultSet resultSet = checkStmt.executeQuery();
            if (resultSet.next())
                return resultSet.getInt("size");

            logger.info("Для user_id: {} не найден замер на дату {}", userID, currentDate);
            return -1;
        } catch (Exception e) {
            logger.error("Ошибка при поиске замера в БД: ", e);
            return -1;
        }
    }

    /** Размеры за последние days дней; ошибка чтения не должна ронять выбор размера. */
    public List<Integer> getPlayerCockSizeByDays(Long userID, Integer days) {
        String checkQuery = "SELECT size FROM " + COCKSIZE_STATS_TABLE
                + " WHERE user_id = ? AND date >= ? LIMIT ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setLong(1, userID);
            checkStmt.setDate(2, Date.valueOf(LocalDate.now().minusDays(Math.max(0, days - 1))));
            checkStmt.setInt(3, days);
            ResultSet resultSet = checkStmt.executeQuery();
            List<Integer> lastSizes = new ArrayList<>();
            while (resultSet.next())
                lastSizes.add(resultSet.getInt("size"));
            return lastSizes;
        } catch (Exception e) {
            logger.error("Ошибка при поиске замеров за последние {} дней: ", days, e);
            return List.of();
        }
    }

    public AVGCockSizeDTO getAVGCockSize(Long userID) {
        String checkQuery = "SELECT " +
                "    AVG(size) AS average_size, " +
                "    COUNT(size) AS measurement_count, " +
                "    MIN(date) AS first_measurement_date, " +
                "    MAX(date) AS last_measurement_date " +
                "FROM " + COCKSIZE_STATS_TABLE + " WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setLong(1, userID);
            ResultSet resultSet = checkStmt.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt("measurement_count");
                if (count == 0) {
                    logger.info("Для user_id: {} не найдено ни одного замера", userID);
                    return null;
                }
                return new AVGCockSizeDTO(
                        userID,
                        resultSet.getFloat("average_size"),
                        resultSet.getDate("first_measurement_date").toLocalDate(),
                        resultSet.getDate("last_measurement_date").toLocalDate(),
                        count
                );
            }
            return null;
        } catch (Exception e) {
            logger.error("Ошибка при поиске средней длины замера: ", e);
            return null;
        }
    }
}
