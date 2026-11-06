package org.example.fun.Config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BotPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    void bindsValidConfiguration() {
        runner.withPropertyValues(validProperties())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    BotProperties properties = context.getBean(BotProperties.class);
                    assertThat(properties.db().url()).isEqualTo("jdbc:postgresql://db-host:5432/fun_db");
                    assertThat(properties.ownerChatId()).isEqualTo(270459384L);
                    assertThat(properties.hasOwner()).isTrue();
                    assertThat(context.getBean(FunProperties.class).resourcesPathOrDefault())
                            .isEqualTo("/opt/fun/resources");
                });
    }

    @Test
    void bindsFromEnvironmentVariableNames() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("BOT_TOKEN", "123:token");
        values.put("BOT_DB_URL", "jdbc:postgresql://db-host:5432/fun_db");
        values.put("BOT_DB_USER", "fun_user");
        values.put("BOT_DB_PASSWORD", "secret");
        values.put("BOT_OWNER_CHAT_ID", "270459384");
        values.put("FUN_RESOURCES_PATH", "/opt/fun/resources");

        StandardEnvironment environment = new StandardEnvironment();
        // Источник должен называться systemEnvironment: только тогда Spring сопоставляет
        // bot.db.url с BOT_DB_URL. Настоящее окружение убираем, иначе тест зависит от машины.
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, values));

        BotProperties bot = Binder.get(environment)
                .bind("bot", Bindable.of(BotProperties.class))
                .orElseThrow(IllegalStateException::new);
        FunProperties fun = Binder.get(environment)
                .bind("fun", Bindable.of(FunProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertThat(bot.token()).isEqualTo("123:token");
        assertThat(bot.db().user()).isEqualTo("fun_user");
        assertThat(bot.ownerChatId()).isEqualTo(270459384L);
        assertThat(fun.resourcesPath()).isEqualTo("/opt/fun/resources");
    }

    @Test
    void hasNoAiOrAdminFields() {
        assertThat(Arrays.stream(BotProperties.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("token", "db", "proxy", "ownerChatId");
        assertThat(Arrays.stream(FunProperties.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("resourcesPath");
    }

    @Test
    void rejectsBlankToken() {
        String[] properties = validProperties();
        properties[0] = "bot.token=";

        runner.withPropertyValues(properties)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsProxyPortOutOfRange() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("bot.proxy.host=127.0.0.1", "bot.proxy.port=70000")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void acceptsEmptyProxyPortWhenHostIsAbsent() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("bot.proxy.port=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(BotProperties.class).hasProxy()).isFalse();
                });
    }

    @Test
    void acceptsMissingOwner() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("bot.owner-chat-id=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(BotProperties.class).hasOwner()).isFalse();
                });
    }

    @Test
    void usesDefaultResourcesPathWhenAbsent() {
        runner.withPropertyValues(validProperties())
                .withPropertyValues("fun.resources-path=")
                .run(context -> assertThat(context.getBean(FunProperties.class).resourcesPathOrDefault())
                        .isEqualTo(FunProperties.DEFAULT_RESOURCES_PATH));
    }

    private String[] validProperties() {
        return new String[]{
                "bot.token=123:token",
                "bot.db.url=jdbc:postgresql://db-host:5432/fun_db",
                "bot.db.user=fun_user",
                "bot.db.password=secret",
                "bot.owner-chat-id=270459384",
                "fun.resources-path=/opt/fun/resources"
        };
    }

    @Configuration
    @EnableConfigurationProperties({BotProperties.class, FunProperties.class})
    static class PropertiesConfig {
    }
}
