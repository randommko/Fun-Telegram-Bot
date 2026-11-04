package org.example.fun.Config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Конфигурация Fun-бота из переменных BOT_*.
 * Полей AI и админки здесь нет: бот работает независимо от них.
 */
@Validated
@ConfigurationProperties(prefix = "bot")
public record BotProperties(
        @NotBlank String token,
        @Valid @NotNull Db db,
        @Valid Proxy proxy,
        Long ownerChatId) {

    /** Прокси опционален: без BOT_PROXY_* Spring не создаёт вложенную запись вовсе. */
    public boolean hasProxy() {
        return proxy != null && proxy.hasHost();
    }

    /** Получатель уведомления о запуске; пустое значение выключает уведомление. */
    public boolean hasOwner() {
        return ownerChatId != null;
    }

    public record Db(@NotBlank String url, @NotBlank String user, @NotBlank String password) {
    }

    public record Proxy(String type, String host, Integer port, String user, String pass) {

        public boolean hasHost() {
            return host != null && !host.isBlank();
        }

        @AssertTrue(message = "BOT_PROXY_HOST задан, а BOT_PROXY_PORT пуст или вне диапазона 1..65535")
        public boolean isPortUsable() {
            return !hasHost() || (port != null && port > 0 && port < 65536);
        }
    }
}
