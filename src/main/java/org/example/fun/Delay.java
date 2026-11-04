package org.example.fun;

/** Пауза между сообщениями игры; в тестах подменяется пустой реализацией. */
@FunctionalInterface
public interface Delay {
    void pause(long millis);
}
