package org.example.fun.CockSize;

import com.vdurmont.emoji.EmojiParser;
import org.example.fun.Config.FunProperties;
import org.example.fun.DTO.AVGCockSizeDTO;
import org.example.fun.MessageSender;
import org.example.fun.Users.UsersService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CockSizeGame {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CockSizeService service;
    private final UsersService usersService;
    private final MessageSender sender;
    private final FunProperties properties;

    public CockSizeGame(CockSizeService service, UsersService usersService,
                        MessageSender sender, FunProperties properties) {
        this.service = service;
        this.usersService = usersService;
        this.sender = sender;
        this.properties = properties;
    }

    public void sendTodayCockSize(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        int size = service.todayOrNew(userId);
        String caption = service.phraseSelection(size, service.getUserNameByID(userId));

        String imageName = service.getCockSizeImageName(size);
        if (imageName == null || imageName.isBlank()) {
            sender.sendMessage(chatId, caption);
            return;
        }

        File image = new File(properties.resourcesPathOrDefault(), imageName);
        if (!sender.sendImage(chatId, caption, image, createInlineKeyboard()))
            sender.sendMessage(chatId, caption);
    }

    public void sendAVGCockSize(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        Long chatId = callbackQuery.getMessage().getChatId();
        String userName = usersService.getUserNameByID(userId);
        AVGCockSizeDTO result = service.getAVGCockSize(userId);

        if (result == null) {
            sender.sendMessage(chatId, userName + " ни разу не измерял свой член!");
            return;
        }

        String average = String.format("%.2f", result.AVGSize());
        String text = "C " + result.firstMeasurementDate().format(DATE)
                + " по " + result.lastMeasurementDate().format(DATE)
                + " " + userName + " сделал " + result.measurementCount() + " замеров.\n"
                + EmojiParser.parseToUnicode("В среднем твой болт\uD83C\uDF46: " + average + "cm");
        sender.sendMessage(chatId, text);
    }

    private InlineKeyboardMarkup createInlineKeyboard() {
        InlineKeyboardButton average = new InlineKeyboardButton();
        average.setText(EmojiParser.parseToUnicode("\uD83D\uDCA6\uD83C\uDF46\uD83D\uDCA6  Статистика  \uD83D\uDCA6\uD83C\uDF46\uD83D\uDCA6"));
        average.setCallbackData("avg_cock_size_button_pressed");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(average)));
        return markup;
    }
}
