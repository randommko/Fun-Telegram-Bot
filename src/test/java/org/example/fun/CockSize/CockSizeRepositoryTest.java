package org.example.fun.CockSize;

import org.example.fun.DTO.AVGCockSizeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CockSizeRepositoryTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet resultSet;

    private CockSizeRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        repo = new CockSizeRepository(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
    }

    @Test
    void savesMeasurementIntoFunSchema() throws Exception {
        repo.setCockSizeWinner(1L, 7);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("INSERT INTO fun.cocksize_stats");
    }

    @Test
    void returnsMinusOneWhenNoMeasurementToday() throws Exception {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThat(repo.getTodayPlayerCockSize(1L)).isEqualTo(-1);
    }

    @Test
    void readsMeasurementForToday() throws Exception {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("size")).thenReturn(13);

        assertThat(repo.getTodayPlayerCockSize(1L)).isEqualTo(13);
    }

    @Test
    void returnsNullAverageWhenThereAreNoMeasurements() throws Exception {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("measurement_count")).thenReturn(0);

        assertThat(repo.getAVGCockSize(1L)).isNull();
    }

    @Test
    void readsAverageWithPeriodAndCount() throws Exception {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("measurement_count")).thenReturn(2);
        when(resultSet.getFloat("average_size")).thenReturn(12.5f);
        when(resultSet.getDate("first_measurement_date")).thenReturn(Date.valueOf("2026-11-01"));
        when(resultSet.getDate("last_measurement_date")).thenReturn(Date.valueOf("2026-11-04"));

        AVGCockSizeDTO average = repo.getAVGCockSize(1L);

        assertThat(average).isNotNull();
        assertThat(average.measurementCount()).isEqualTo(2);
        assertThat(average.AVGSize()).isEqualTo(12.5f);
        assertThat(average.firstMeasurementDate()).isEqualTo(Date.valueOf("2026-11-01").toLocalDate());
    }

    @Test
    void imageIsChosenOnlyAmongUsedOnes() throws Exception {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("img")).thenReturn("7.png");

        assertThat(repo.getCockSizeImageName(7)).isEqualTo("7.png");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue()).contains("fun.cocksize_imgs");
        assertThat(sql.getValue()).contains("is_using");
    }
}
