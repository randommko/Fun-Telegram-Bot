package org.example.fun.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки Fun, не относящиеся к Telegram: путь к картинкам замера.
 * Переменная FUN_RESOURCES_PATH раскладывается в fun.resources-path.
 */
@ConfigurationProperties(prefix = "fun")
public record FunProperties(String resourcesPath) {

    public static final String DEFAULT_RESOURCES_PATH = "/bin/tg_bot/resources";

    public String resourcesPathOrDefault() {
        return resourcesPath == null || resourcesPath.isBlank()
                ? DEFAULT_RESOURCES_PATH
                : resourcesPath.trim();
    }
}
