package org.example.fun.CockSize;

import com.vdurmont.emoji.EmojiParser;
import org.example.fun.DTO.AVGCockSizeDTO;
import org.example.fun.Users.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service
public class CockSizeService {
    private static final Logger logger = LoggerFactory.getLogger(CockSizeService.class);
    private final CockSizeRepository repo;
    private final UsersService usersService;

    public CockSizeService(CockSizeRepository repo, UsersService usersService) {
        this.repo = repo;
        this.usersService = usersService;
    }

    /** Сегодняшний замер, а если его нет — новый с сохранением. */
    public int todayOrNew(Long userID) {
        int existing = repo.getTodayPlayerCockSize(userID);
        if (existing != -1)
            return existing;
        return measureCockSize(userID);
    }

    public Integer measureCockSize(Long userID) {
        int newRandomSize = getCockSize(userID);
        repo.setCockSizeWinner(userID, newRandomSize);
        return newRandomSize;
    }

    public Integer findTodayCockSize(Long userID) {
        return repo.getTodayPlayerCockSize(userID);
    }

    /** Набор с повторами: частые значения выпадают чаще, последние замеры остаются в наборе. */
    List<Integer> sizePool(Long userID) {
        List<Integer> sizes = new ArrayList<>();

        sizes.add(0);
        sizes.add(1);
        sizes.add(2);
        sizes.add(3);
        for (int i = 0; i < 2; i++) sizes.add(5);
        for (int i = 0; i < 2; i++) sizes.add(6);
        for (int i = 0; i < 2; i++) sizes.add(7);
        for (int i = 0; i < 2; i++) sizes.add(8);
        for (int i = 0; i < 5; i++) sizes.add(10);
        for (int i = 0; i < 5; i++) sizes.add(11);
        for (int i = 0; i < 5; i++) sizes.add(13);
        for (int i = 0; i < 4; i++) sizes.add(15);
        for (int i = 0; i < 4; i++) sizes.add(16);
        for (int i = 0; i < 3; i++) sizes.add(18);
        for (int i = 0; i < 2; i++) sizes.add(20);
        for (int i = 0; i < 2; i++) sizes.add(24);
        sizes.add(40);
        sizes.add(45);
        sizes.add(49);

        List<Integer> lastSizes = repo.getPlayerCockSizeByDays(userID, 3);
        logger.debug("Прошлые замеры игрока: {}", lastSizes);
        for (int size : lastSizes) {
            sizes.removeIf(n -> Objects.equals(n, size));
            sizes.add(size);
        }

        return sizes;
    }

    private int getCockSize(Long userID) {
        List<Integer> sizes = sizePool(userID);
        Random random = new Random();
        return sizes.get(random.nextInt(sizes.size()));
    }

    public String phraseSelection(int size, String username) {
        if (size >= 0 && size <= 5) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :-1:");
        } else if (size >= 6 && size <= 10) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :handshake:");
        } else if (size >= 11 && size <= 20) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :tada:");
        } else if (size >= 21 && size <= 30) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :balloon::balloon:");
        } else if (size >= 31 && size <= 40) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :palm_tree:");
        } else if (size >= 41 && size <= 50) {
            return EmojiParser.parseToUnicode("Cocksize of " + username + " is " + size + "cm :sparkling_heart::sparkling_heart::sparkling_heart:");
        }
        return EmojiParser.parseToUnicode("NO FUCKING WAY! Cocksize of " + username + " is " + size + "cm ");
    }

    public String getCockSizeImageName(Integer size) {
        return repo.getCockSizeImageName(size);
    }

    public String getUserNameByID(Long userID) {
        return usersService.getUserNameByID(userID);
    }

    public AVGCockSizeDTO getAVGCockSize(Long userID) {
        return repo.getAVGCockSize(userID);
    }
}
