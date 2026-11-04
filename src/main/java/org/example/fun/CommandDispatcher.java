package org.example.fun;

import org.example.fun.CockSize.CockSizeGame;
import org.example.fun.PidorGame.PidorGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Только команды Fun: чужие команды и сообщения без слэша игнорируются. */
@Component
public class CommandDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(CommandDispatcher.class);

    private final MessageSender messageSender;
    private final CockSizeGame cockSizeGame;
    private final PidorGame pidorGame;
    private final KeyboardBuilder keyboardBuilder;

    public enum Command {
        BOT_INFO("/bot_info"),
        COCKSIZE("/cocksize"),
        PIDOR_REG("/pidor_reg"),
        PIDOR_STATS("/pidor_stats"),
        PIDOR_START("/pidor_start"),
        HOROSCOPE("/horoscope_today");

        private final String command;

        Command(String command) {
            this.command = command;
        }

        public String command() {
            return command;
        }

        public static Command fromString(String cmd) {
            String clean = cmd.toLowerCase().trim();
            for (Command command : values()) {
                if (command.command.equals(clean))
                    return command;
            }
            return null;
        }

        public static List<String> all() {
            return java.util.Arrays.stream(values()).map(Command::command).toList();
        }
    }

    private final Map<Command, Consumer<Message>> handlers = Map.of(
            Command.BOT_INFO, this::handleBotInfo,
            Command.COCKSIZE, this::handleCockSize,
            Command.PIDOR_REG, this::handlePidorReg,
            Command.PIDOR_STATS, this::handlePidorStats,
            Command.PIDOR_START, this::handlePidorStart,
            Command.HOROSCOPE, this::handleHoroscope
    );

    public CommandDispatcher(MessageSender messageSender, CockSizeGame cockSizeGame,
                             PidorGame pidorGame, KeyboardBuilder keyboardBuilder) {
        this.messageSender = messageSender;
        this.cockSizeGame = cockSizeGame;
        this.pidorGame = pidorGame;
        this.keyboardBuilder = keyboardBuilder;
    }

    public void dispatch(Update update) {
        Message message = update.getMessage();
        if (message == null || message.getText() == null)
            return;

        String text = message.getText();
        if (!text.startsWith("/"))
            return;

        String commandText = text.split(" ", 2)[0];
        int mention = commandText.indexOf('@');
        if (mention > 0)
            commandText = commandText.substring(0, mention);

        Command command = Command.fromString(commandText);
        if (command == null) {
            logger.debug("Команда не принадлежит Fun: {}", commandText);
            return;
        }

        handlers.get(command).accept(message);
    }

    private void handleBotInfo(Message message) {
        messageSender.sendMessage(message.getChatId(), """
                Бот для развлечений! Команды:
                /cocksize - Измерить достоинство
                /pidor_start - Выбрать пидора дня
                /horoscope_today - Гороскоп
                /pidor_reg - Вступить в ряды пидоров
                /pidor_stats - Статистика пидоров
                """);
    }

    private void handleCockSize(Message message) {
        cockSizeGame.sendTodayCockSize(message);
    }

    private void handlePidorReg(Message message) {
        pidorGame.registerPlayer(message.getChatId(), message.getFrom().getId());
    }

    private void handlePidorStats(Message message) {
        pidorGame.sendPidorStats(message.getChatId());
    }

    private void handlePidorStart(Message message) {
        pidorGame.startPidorGame(message.getChatId(), message.getFrom().getId());
    }

    private void handleHoroscope(Message message) {
        keyboardBuilder.sendHoroscopeKeyboard(messageSender, message.getChatId());
    }
}
