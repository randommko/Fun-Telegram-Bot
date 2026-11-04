package org.example.fun;

import org.example.fun.CockSize.CockSizeGame;
import org.example.fun.Horoscope.HoroscopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.function.Consumer;

@Component
public class CallbackDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(CallbackDispatcher.class);

    private final MessageSender messageSender;
    private final HoroscopeService horoscopeService;
    private final CockSizeGame cockSizeGame;
    private final Map<String, Consumer<CallbackQuery>> handlers;

    public CallbackDispatcher(MessageSender messageSender, HoroscopeService horoscopeService,
                              CockSizeGame cockSizeGame) {
        this.messageSender = messageSender;
        this.horoscopeService = horoscopeService;
        this.cockSizeGame = cockSizeGame;
        this.handlers = Map.of(
                "avg_cock_size_button_pressed", this::handleAverage
        );
    }

    public void dispatch(CallbackQuery callback) {
        String data = callback.getData();
        logger.debug("Callback: {}", data);

        KeyboardBuilder.Zodiac zodiac = KeyboardBuilder.Zodiac.fromCallback(data);
        if (zodiac != null) {
            horoscopeService.sendHoroscope(callback, zodiac.getId(), "today");
            answerCallbackQuery(callback);
            return;
        }

        Consumer<CallbackQuery> handler = handlers.get(data);
        if (handler != null) {
            handler.accept(callback);
            answerCallbackQuery(callback);
        } else {
            logger.warn("Неизвестный callback: {}", data);
            answerCallbackQuery(callback, "Неизвестная кнопка");
        }
    }

    private void handleAverage(CallbackQuery callback) {
        cockSizeGame.sendAVGCockSize(callback);
    }

    private void answerCallbackQuery(CallbackQuery callback) {
        answerCallbackQuery(callback, null);
    }

    private void answerCallbackQuery(CallbackQuery callback, String text) {
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callback.getId())
                .text(text)
                .build();
        try {
            messageSender.getBot().execute(answer);
        } catch (TelegramApiException e) {
            logger.error("Ошибка answerCallback: ", e);
        }
    }
}
