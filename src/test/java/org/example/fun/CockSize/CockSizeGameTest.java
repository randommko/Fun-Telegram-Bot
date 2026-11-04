package org.example.fun.CockSize;

import org.example.fun.Config.FunProperties;
import org.example.fun.DTO.AVGCockSizeDTO;
import org.example.fun.MessageSender;
import org.example.fun.Users.UsersService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.io.File;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CockSizeGameTest {

    @Mock
    private CockSizeService service;
    @Mock
    private UsersService usersService;
    @Mock
    private MessageSender sender;
    @Mock
    private FunProperties properties;

    @InjectMocks
    private CockSizeGame game;

    @Test
    void sendsPhotoWithStatisticsButton() {
        stubMeasurement(7, "7.png");
        when(properties.resourcesPathOrDefault()).thenReturn("/tmp");
        when(sender.sendImage(eq(10L), eq("текст"), any(File.class), any(InlineKeyboardMarkup.class)))
                .thenReturn(true);

        game.sendTodayCockSize(message());

        verify(sender).sendImage(eq(10L), eq("текст"), any(File.class), any(InlineKeyboardMarkup.class));
        verify(sender, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void fallsBackToTextWhenPhotoFails() {
        stubMeasurement(7, "7.png");
        when(properties.resourcesPathOrDefault()).thenReturn("/tmp");
        when(sender.sendImage(eq(10L), eq("текст"), any(File.class), any(InlineKeyboardMarkup.class)))
                .thenReturn(false);

        game.sendTodayCockSize(message());

        verify(sender).sendMessage(10L, "текст");
    }

    @Test
    void sendsTextWhenImageIsNotConfigured() {
        stubMeasurement(7, null);

        game.sendTodayCockSize(message());

        verify(sender).sendMessage(10L, "текст");
        verify(sender, never()).sendImage(anyLong(), anyString(), any(File.class), any(InlineKeyboardMarkup.class));
    }

    @Test
    void reportsNoMeasurements() {
        when(usersService.getUserNameByID(1L)).thenReturn("Имя");
        when(service.getAVGCockSize(1L)).thenReturn(null);

        game.sendAVGCockSize(callback());

        verify(sender).sendMessage(eq(10L), contains("ни разу"));
    }

    @Test
    void reportsAverageWithPeriodAndCount() {
        when(usersService.getUserNameByID(1L)).thenReturn("Имя");
        when(service.getAVGCockSize(1L)).thenReturn(new AVGCockSizeDTO(
                1L, 12.5f, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 2));

        game.sendAVGCockSize(callback());

        verify(sender).sendMessage(eq(10L), contains("2 замеров"));
        verify(sender).sendMessage(eq(10L), contains("12"));
    }

    private void stubMeasurement(int size, String imageName) {
        when(service.todayOrNew(1L)).thenReturn(size);
        when(service.getUserNameByID(1L)).thenReturn("Имя");
        when(service.phraseSelection(size, "Имя")).thenReturn("текст");
        when(service.getCockSizeImageName(size)).thenReturn(imageName);
    }

    private Message message() {
        Message message = new Message();
        message.setChat(chat());
        message.setFrom(user());
        return message;
    }

    private CallbackQuery callback() {
        CallbackQuery callback = new CallbackQuery();
        callback.setFrom(user());
        callback.setMessage(message());
        return callback;
    }

    private Chat chat() {
        Chat chat = new Chat();
        chat.setId(10L);
        chat.setTitle("Чат");
        return chat;
    }

    private User user() {
        return new User(1L, "Имя", false, null, "nick", "ru",
                null, null, null, false, false);
    }
}
