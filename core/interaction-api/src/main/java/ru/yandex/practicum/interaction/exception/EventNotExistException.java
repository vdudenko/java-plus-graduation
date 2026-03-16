package ru.yandex.practicum.interaction.exception;

public class EventNotExistException extends RuntimeException {
    public EventNotExistException(String message) {
        super(message);
    }
}
