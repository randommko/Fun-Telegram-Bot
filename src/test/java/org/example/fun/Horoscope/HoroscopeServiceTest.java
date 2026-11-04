package org.example.fun.Horoscope;

import org.example.fun.MessageSender;
import org.example.fun.Users.UsersService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoroscopeServiceTest {

    private static final long CHAT_ID = -1001347887839L;
    private static final long USER_ID = 42L;
    private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

    private final UsersService usersService = mock(UsersService.class);
    private final MessageSender sender = mock(MessageSender.class);
    private final HoroscopeClient client = mock(HoroscopeClient.class);

    @Test
    void sendsHoroscopeOnceAndServesSecondClickFromCache() throws IOException {
        when(usersService.getUserNameByID(USER_ID)).thenReturn("Вася");
        when(client.fetch("aries", "today")).thenReturn("Спокойное начало недели.");

        HoroscopeService service = serviceAt("2026-11-04T10:00:00Z");
        service.sendHoroscope(callback(), "aries", "today");
        service.sendHoroscope(callback(), "aries", "today");

        verify(client, times(1)).fetch("aries", "today");

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sender, times(2)).sendMessage(eq(CHAT_ID), message.capture());
        assertThat(message.getValue())
                .contains("04.11.2026")
                .contains("ОВЕН")
                .contains("Вася")
                .contains("Спокойное начало недели.");
    }

    @Test
    void tellsUserWhenSourceIsUnavailable() throws IOException {
        when(usersService.getUserNameByID(USER_ID)).thenReturn("Вася");
        when(client.fetch("aries", "today")).thenThrow(new IOException("horo.mail.ru недоступен"));

        serviceAt("2026-11-04T10:00:00Z").sendHoroscope(callback(), "aries", "today");

        verify(sender).sendMessage(CHAT_ID, "Гороскоп сейчас недоступен, попробуй позже.");
    }

    @Test
    void clearsCacheOnNewDay() throws IOException {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-04T10:00:00Z"), ZONE);
        HoroscopeService service = new HoroscopeService(usersService, sender, client, clock);
        when(usersService.getUserNameByID(USER_ID)).thenReturn("Вася");
        when(client.fetch("aries", "today")).thenReturn("Текст.");

        service.sendHoroscope(callback(), "aries", "today");
        clock.setInstant(Instant.parse("2026-11-05T10:00:00Z"));
        service.sendHoroscope(callback(), "aries", "today");

        verify(client, times(2)).fetch("aries", "today");
    }

    @Test
    void cachesDifferentSignsSeparately() throws IOException {
        when(usersService.getUserNameByID(USER_ID)).thenReturn("Вася");
        when(client.fetch("aries", "today")).thenReturn("Овен.");
        when(client.fetch("taurus", "today")).thenReturn("Телец.");

        HoroscopeService service = serviceAt("2026-11-04T10:00:00Z");
        service.sendHoroscope(callback(), "aries", "today");
        service.sendHoroscope(callback(), "taurus", "today");

        verify(client).fetch("aries", "today");
        verify(client).fetch("taurus", "today");
    }

    private HoroscopeService serviceAt(String instant) {
        return new HoroscopeService(usersService, sender, client,
                Clock.fixed(Instant.parse(instant), ZONE));
    }

    private CallbackQuery callback() {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(CHAT_ID);

        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);

        CallbackQuery callback = mock(CallbackQuery.class);
        when(callback.getMessage()).thenReturn(message);
        when(callback.getFrom()).thenReturn(user);
        return callback;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
