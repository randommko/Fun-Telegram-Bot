package org.example.fun.CockSize;

import org.example.fun.Users.UsersService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CockSizeServiceTest {

    @Mock
    private CockSizeRepository repo;
    @Mock
    private UsersService usersService;

    @InjectMocks
    private CockSizeService service;

    @Test
    void firstMeasurementIsSaved() {
        when(repo.getTodayPlayerCockSize(1L)).thenReturn(-1);
        when(repo.getPlayerCockSizeByDays(1L, 3)).thenReturn(List.of());

        int size = service.todayOrNew(1L);

        assertThat(size).isBetween(0, 49);
        verify(repo).setCockSizeWinner(1L, size);
    }

    @Test
    void repeatReturnsSavedSize() {
        when(repo.getTodayPlayerCockSize(1L)).thenReturn(7);

        assertThat(service.todayOrNew(1L)).isEqualTo(7);
        verify(repo, never()).setCockSizeWinner(anyLong(), anyInt());
    }

    @Test
    void recentSizesStayInPool() {
        when(repo.getPlayerCockSizeByDays(1L, 3)).thenReturn(List.of(7, 13));

        assertThat(service.sizePool(1L))
                .contains(7, 13)
                .allMatch(size -> size >= 0 && size <= 49);
    }
}
