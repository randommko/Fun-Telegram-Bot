package org.example.fun;

import org.example.fun.CockSize.CockSizeGame;
import org.example.fun.PidorGame.PidorGame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

    private static final Long CHAT_ID = 10L;
    private static final Long USER_ID = 1L;

    @Mock
    private MessageSender sender;
    @Mock
    private CockSizeGame cockSizeGame;
    @Mock
    private PidorGame pidorGame;
    @Mock
    private KeyboardBuilder keyboardBuilder;

    @InjectMocks
    private CommandDispatcher dispatcher;

    @Test
    void dispatchesCockSize() {
        Update update = update("/cocksize");

        dispatcher.dispatch(update);

        verify(cockSizeGame).sendTodayCockSize(update.getMessage());
    }

    @Test
    void dispatchesPidorRegistration() {
        dispatcher.dispatch(update("/pidor_reg"));

        verify(pidorGame).registerPlayer(CHAT_ID, USER_ID);
    }

    @Test
    void dispatchesPidorStats() {
        dispatcher.dispatch(update("/pidor_stats"));

        verify(pidorGame).sendPidorStats(CHAT_ID);
    }

    @Test
    void dispatchesPidorStart() {
        dispatcher.dispatch(update("/pidor_start"));

        verify(pidorGame).startPidorGame(CHAT_ID, USER_ID);
    }

    @Test
    void dispatchesHoroscopeKeyboard() {
        dispatcher.dispatch(update("/horoscope_today"));

        verify(keyboardBuilder).sendHoroscopeKeyboard(sender, CHAT_ID);
    }

    @Test
    void helpListsOnlyFunCommands() {
        dispatcher.dispatch(update("/bot_info"));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(eq(CHAT_ID), text.capture());
        assertThat(text.getValue())
                .contains("/cocksize", "/pidor_start", "/pidor_reg", "/pidor_stats", "/horoscope_today")
                .doesNotContain("/ai", "/clear", "/summary");
    }

    @Test
    void acceptsCommandAddressedToBot() {
        dispatcher.dispatch(update("/cocksize@FunBot"));

        verify(cockSizeGame).sendTodayCockSize(any(Message.class));
    }

    @Test
    void ignoresForeignCommand() {
        dispatcher.dispatch(update("/ai"));
        dispatcher.dispatch(update("/summary"));

        verifyNoInteractions(cockSizeGame, pidorGame, keyboardBuilder, sender);
    }

    @Test
    void ignoresPlainText() {
        dispatcher.dispatch(update("привет"));

        verifyNoInteractions(cockSizeGame, pidorGame, keyboardBuilder, sender);
    }

    private Update update(String text) {
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setTitle("Чат");

        User user = new User(USER_ID, "Имя", false, null, "nick", "ru",
                null, null, null, false, false);

        Message message = new Message();
        message.setChat(chat);
        message.setFrom(user);
        message.setText(text);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
