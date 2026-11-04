package org.example.fun;

import org.example.fun.CockSize.CockSizeGame;
import org.example.fun.Horoscope.HoroscopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackDispatcherTest {

    @Mock
    private MessageSender sender;
    @Mock
    private HoroscopeService horoscopeService;
    @Mock
    private CockSizeGame cockSizeGame;
    @Mock
    private TelegramLongPollingBot bot;

    @InjectMocks
    private CallbackDispatcher dispatcher;

    @Test
    void dispatchesZodiacCallback() throws TelegramApiException {
        when(sender.getBot()).thenReturn(bot);
        CallbackQuery callback = callback("aries_button_pressed");

        dispatcher.dispatch(callback);

        verify(horoscopeService).sendHoroscope(callback, "aries", "today");
        verify(bot).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    void dispatchesAverageCockSizeCallback() throws TelegramApiException {
        when(sender.getBot()).thenReturn(bot);
        CallbackQuery callback = callback("avg_cock_size_button_pressed");

        dispatcher.dispatch(callback);

        verify(cockSizeGame).sendAVGCockSize(callback);
        verify(bot).execute(any(AnswerCallbackQuery.class));
    }

    @Test
    void tellsAboutUnknownCallback() throws TelegramApiException {
        when(sender.getBot()).thenReturn(bot);
        CallbackQuery callback = callback("unknown");

        dispatcher.dispatch(callback);

        ArgumentCaptor<AnswerCallbackQuery> answer = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(bot).execute(answer.capture());
        assertThat(answer.getValue().getText()).isEqualTo("Неизвестная кнопка");
        verifyNoInteractions(horoscopeService, cockSizeGame);
    }

    private CallbackQuery callback(String data) {
        CallbackQuery callback = new CallbackQuery();
        callback.setId("1");
        callback.setData(data);
        return callback;
    }
}
