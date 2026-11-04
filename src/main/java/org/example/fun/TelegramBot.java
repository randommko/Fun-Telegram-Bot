package org.example.fun;

import org.example.fun.Config.BotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final String botToken;
    private final UserChatManager userChatManager;
    private final CommandDispatcher commandDispatcher;
    private final CallbackDispatcher callbackDispatcher;
    private final ThreadPoolExecutor executor;

    public TelegramBot(BotProperties properties, DefaultBotOptions options,
                       UserChatManager userChatManager,
                       CommandDispatcher commandDispatcher,
                       CallbackDispatcher callbackDispatcher) {
        super(options);
        this.botToken = properties.token();
        this.userChatManager = userChatManager;
        this.commandDispatcher = commandDispatcher;
        this.callbackDispatcher = callbackDispatcher;
        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    @Override
    public String getBotUsername() {
        return "Fun-Telegram-Bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        CompletableFuture.runAsync(() -> processUpdate(update), executor);
        logger.debug("Активных потоков: {}", executor.getActiveCount());
    }

    private void processUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery callback = update.getCallbackQuery();
            userChatManager.checkUser(callback.getFrom());
            if (callback.getMessage() != null)
                userChatManager.checkChat(callback.getMessage().getChat());
            callbackDispatcher.dispatch(callback);
            return;
        }

        Message message = update.getMessage();
        if (message == null || message.getFrom() == null || message.getChat() == null) {
            logger.debug("Обновление без пользователя или чата: {}", update.getUpdateId());
            return;
        }

        userChatManager.checkUser(message.getFrom());
        userChatManager.checkChat(message.getChat());

        if (message.getText() == null) {
            logger.debug("Не-текстовое сообщение в чате {}", message.getChatId());
            return;
        }

        commandDispatcher.dispatch(update);
    }
}
