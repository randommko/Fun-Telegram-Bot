package org.example.fun;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeyboardBuilderTest {

    private final KeyboardBuilder keyboardBuilder = new KeyboardBuilder();

    @Test
    void buildsTwelveSignsThreePerRow() {
        InlineKeyboardMarkup markup = keyboardBuilder.buildHoroscopeKeyboard();
        List<List<InlineKeyboardButton>> rows = markup.getKeyboard();

        assertThat(rows).hasSize(4);
        assertThat(rows).allMatch(row -> row.size() == 3);
        assertThat(rows.stream().flatMap(List::stream).map(InlineKeyboardButton::getCallbackData))
                .contains("aries_button_pressed", "pisces_button_pressed");
    }

    @Test
    void recognisesZodiacByCallbackData() {
        assertThat(KeyboardBuilder.Zodiac.fromCallback("aries_button_pressed"))
                .isEqualTo(KeyboardBuilder.Zodiac.ARIES);
    }

    @Test
    void ignoresForeignCallbackData() {
        assertThat(KeyboardBuilder.Zodiac.fromCallback(null)).isNull();
        assertThat(KeyboardBuilder.Zodiac.fromCallback("avg_cock_size_button_pressed")).isNull();
    }
}
