package org.example.fun.Users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class UsersService {
    private static final Logger logger = LoggerFactory.getLogger(UsersService.class);
    private final UsersRepository repo;

    public UsersService(UsersRepository repo) {
        this.repo = repo;
    }

    public void addUser(User user) {
        repo.insertUserInDB(user);
    }

    public void updateUser(User user) {
        repo.updateUserInDB(user);
    }

    public boolean checkUser(User user) {
        return repo.getUserByID(user.getId()) != null;
    }

    /** Ник, а если его нет — имя. Запись создаётся до обработки команды. */
    public String getUserNameByID(Long userID) {
        User user = repo.getUserByID(userID);
        if (user == null) {
            logger.warn("Пользователь {} не найден в shared.tg_users", userID);
            return "неизвестный";
        }

        String userName = user.getUserName();
        if (userName != null && !userName.isBlank())
            return userName;
        return user.getFirstName();
    }
}
