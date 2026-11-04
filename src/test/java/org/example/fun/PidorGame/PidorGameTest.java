package org.example.fun.PidorGame;

import org.example.fun.Delay;
import org.example.fun.MessageSender;
import org.example.fun.Users.UsersService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PidorGameTest {

    private static final Long CHAT = 10L;

    @Mock
    private PidorGameRepository repo;
    @Mock
    private UsersService usersService;
    @Mock
    private MessageSender sender;
    @Mock
    private Delay delay;

    @InjectMocks
    private PidorGame game;

    @Test
    void registersNewPlayer() {
        when(usersService.getUserNameByID(1L)).thenReturn("@nick");
        when(repo.registerPlayer(CHAT, 1L)).thenReturn(1);

        game.registerPlayer(CHAT, 1L);

        verify(sender).sendMessage(CHAT, "Игрок @nick зарегистрирован!");
    }

    @Test
    void reportsAlreadyRegisteredPlayer() {
        when(usersService.getUserNameByID(1L)).thenReturn("@nick");
        when(repo.registerPlayer(CHAT, 1L)).thenReturn(0);

        game.registerPlayer(CHAT, 1L);

        verify(sender).sendMessage(eq(CHAT), contains("был зарегистрирован ранее"));
    }

    @Test
    void requiresRegisteredStarter() {
        when(repo.getTodayWinner(CHAT)).thenReturn(null);
        when(repo.getPidorGamePlayers(CHAT)).thenReturn(Set.of(2L));

        game.startPidorGame(CHAT, 1L);

        verify(sender).sendMessage(eq(CHAT), contains("Зарегистрируйтесь"));
        verify(repo, never()).setPidorWinner(anyLong(), anyLong());
    }

    @Test
    void requiresAtLeastTwoPlayers() {
        when(repo.getTodayWinner(CHAT)).thenReturn(null);
        when(repo.getPidorGamePlayers(CHAT)).thenReturn(Set.of(1L));

        game.startPidorGame(CHAT, 1L);

        verify(sender).sendMessage(eq(CHAT), contains("хотя бы два игрока"));
        verify(repo, never()).setPidorWinner(anyLong(), anyLong());
    }

    @Test
    void reportsNoPlayers() {
        when(repo.getTodayWinner(CHAT)).thenReturn(null);
        when(repo.getPidorGamePlayers(CHAT)).thenReturn(Set.of());

        game.startPidorGame(CHAT, 1L);

        verify(sender).sendMessage(CHAT, "Нет зарегистрированных игроков.");
    }

    @Test
    void reportsExistingWinnerWithoutChoosingAnother() {
        when(repo.getTodayWinner(CHAT)).thenReturn(2L);
        when(usersService.getUserNameByID(2L)).thenReturn("Имя");

        game.startPidorGame(CHAT, 1L);

        verify(sender).sendMessage(eq(CHAT), contains("Победитель"));
        verify(repo, never()).setPidorWinner(anyLong(), anyLong());
    }

    @Test
    void choosesWinnerOncePerDay() {
        when(repo.getTodayWinner(CHAT)).thenReturn(null);
        when(repo.getPidorGamePlayers(CHAT)).thenReturn(Set.of(1L, 2L));
        when(repo.getRandomResponses()).thenReturn(List.of("раз", "два"));
        when(repo.getWinnerResponse()).thenReturn("Победитель: ");
        when(usersService.getUserNameByID(anyLong())).thenReturn("Имя");

        game.startPidorGame(CHAT, 1L);

        verify(repo).setPidorWinner(eq(CHAT), anyLong());
        verify(delay, times(2)).pause(2000);
        verify(sender, atLeastOnce()).sendMessage(eq(CHAT), contains("Победитель"));
    }

    @Test
    void keepsGameGoingWhenResponsesFail() {
        when(repo.getTodayWinner(CHAT)).thenReturn(null);
        when(repo.getPidorGamePlayers(CHAT)).thenReturn(Set.of(1L, 2L));
        when(repo.getRandomResponses()).thenThrow(new RuntimeException("db"));
        when(repo.getWinnerResponse()).thenReturn("Победитель: ");
        when(usersService.getUserNameByID(anyLong())).thenReturn("Имя");

        game.startPidorGame(CHAT, 1L);

        verify(sender).sendMessage(eq(CHAT), contains("запасным"));
        verify(repo).setPidorWinner(eq(CHAT), anyLong());
    }

    @Test
    void blocksParallelStartInSameChat() throws Exception {
        Long chat = 99L;
        CountDownLatch responseStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        when(repo.getTodayWinner(chat)).thenReturn(null);
        when(repo.getPidorGamePlayers(chat)).thenReturn(Set.of(1L, 2L));
        when(repo.getRandomResponses()).thenAnswer(invocation -> {
            responseStarted.countDown();
            release.await(5, TimeUnit.SECONDS);
            return List.of();
        });
        when(repo.getWinnerResponse()).thenReturn("Победитель: ");
        when(usersService.getUserNameByID(anyLong())).thenReturn("Имя");

        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> game.startPidorGame(chat, 1L));
        assertThat(responseStarted.await(5, TimeUnit.SECONDS)).isTrue();

        game.startPidorGame(chat, 2L);

        verify(sender).sendMessage(eq(chat), contains("уже запущен"));

        release.countDown();
        first.get(5, TimeUnit.SECONDS);

        verify(repo).setPidorWinner(eq(chat), anyLong());
    }

    @Test
    void sortsStatisticsByWins() {
        when(repo.getPidorStats(CHAT)).thenReturn(Map.of("a", 1, "b", 3));

        game.sendPidorStats(CHAT);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(eq(CHAT), message.capture());
        assertThat(message.getValue().indexOf("b: 3"))
                .isLessThan(message.getValue().indexOf("a: 1"));
    }

    @Test
    void reportsNoWinsYet() {
        when(repo.getPidorStats(CHAT)).thenReturn(Map.of());

        game.sendPidorStats(CHAT);

        verify(sender).sendMessage(eq(CHAT), contains("Побед пока нет"));
    }
}
