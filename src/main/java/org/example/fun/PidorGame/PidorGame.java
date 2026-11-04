package org.example.fun.PidorGame;

import com.vdurmont.emoji.EmojiParser;
import org.example.fun.Delay;
import org.example.fun.MessageSender;
import org.example.fun.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PidorGame {
    private static final Logger logger = LoggerFactory.getLogger(PidorGame.class);
    private static final long MESSAGE_PAUSE_MS = 2000;

    private final PidorGameRepository repo;
    private final UsersService usersService;
    private final MessageSender sender;
    private final Delay delay;

    // Флаги «в процессе» по чатам: защита от параллельного запуска в одном чате.
    private static final ConcurrentHashMap<Long, AtomicBoolean> processingFlags = new ConcurrentHashMap<>();

    public PidorGame(PidorGameRepository repo, UsersService usersService,
                     MessageSender sender, Delay delay) {
        this.repo = repo;
        this.usersService = usersService;
        this.sender = sender;
        this.delay = delay;
    }

    public void registerPlayer(Long chatID, Long userID) {
        String userName = usersService.getUserNameByID(userID);
        Integer changed = repo.registerPlayer(chatID, userID);
        if (changed != 0)
            sender.sendMessage(chatID, "Игрок " + userName + " зарегистрирован!");
        else
            sender.sendMessage(chatID, "Игрок " + userName + " был зарегистрирован ранее ");
    }

    public void sendPidorStats(Long chatID) {
        Map<String, Integer> stats = repo.getPidorStats(chatID);

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(stats.entrySet());
        sorted.sort((first, second) -> second.getValue().compareTo(first.getValue()));

        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sorted)
            sortedMap.put(entry.getKey(), entry.getValue());

        if (sortedMap.isEmpty()) {
            sender.sendMessage(chatID, EmojiParser.parseToUnicode(":rainbow_flag: Побед пока нет."));
            return;
        }

        StringBuilder message = new StringBuilder(EmojiParser.parseToUnicode(":rainbow_flag: Статистика игр:\n"));
        sortedMap.forEach((userName, score) -> {
            String name = userName.startsWith("@") ? userName.substring(1) : userName;
            message.append(EmojiParser.parseToUnicode(":rainbow_flag:"))
                    .append(name)
                    .append(": ").append(score).append(" побед\n");
        });
        sender.sendMessage(chatID, message.toString());
    }

    public void startPidorGame(Long chatID, Long userID) {
        logger.info("Запущена игра в чате: {}", chatID);

        AtomicBoolean processing = processingFlags.computeIfAbsent(chatID, key -> new AtomicBoolean(false));
        if (!processing.compareAndSet(false, true)) {
            logger.info("Поиск победителя уже запущен в чате {}", chatID);
            sender.sendMessage(chatID, EmojiParser.parseToUnicode(":rainbow_flag: Поиск победителя уже запущен! Подождите завершения."));
            return;
        }

        try {
            Long winnerID = repo.getTodayWinner(chatID);
            if (winnerID != null) {
                logger.info("Победитель на текущую дату уже выбран");
                sender.sendMessage(chatID,
                        EmojiParser.parseToUnicode(":rainbow_flag: Сегодня уже выбрали. Победитель: "
                                + usersService.getUserNameByID(winnerID)));
                return;
            }

            Set<Long> chatPlayers = repo.getPidorGamePlayers(chatID);
            if (chatPlayers.isEmpty()) {
                logger.info("В чате {} нет зарегистрированных игроков", chatID);
                sender.sendMessage(chatID, "Нет зарегистрированных игроков.");
                return;
            }

            if (!chatPlayers.contains(userID)) {
                logger.info("Игру пытается запустить незарегистрированный игрок");
                sender.sendMessage(chatID, """
                        Сегодня победителя ещё не выбирали.
                        Игру может начать только зарегистрированный игрок.
                        Зарегистрируйтесь командой /pidor_reg""");
                return;
            }

            if (chatPlayers.size() < 2) {
                logger.info("В чате {} недостаточно игроков: {}", chatID, chatPlayers.size());
                sender.sendMessage(chatID, "Для игры необходимо хотя бы два игрока. Зарегистрируйтесь командой /pidor_reg");
                return;
            }

            try {
                for (String response : repo.getRandomResponses()) {
                    sender.sendMessage(chatID, response);
                    delay.pause(MESSAGE_PAUSE_MS);
                }
            } catch (Exception e) {
                sender.sendMessage(chatID, "Ищем победителя запасным вариантом...");
                logger.error("Ошибка при получении сообщений игры: ", e);
            }

            winnerID = new ArrayList<>(chatPlayers).get(new Random().nextInt(chatPlayers.size()));
            repo.setPidorWinner(chatID, winnerID);
            sender.sendMessage(chatID, EmojiParser.parseToUnicode(":rainbow_flag:")
                    + " " + repo.getWinnerResponse() + usersService.getUserNameByID(winnerID) + "!");
        } finally {
            processing.set(false);
        }
    }
}
