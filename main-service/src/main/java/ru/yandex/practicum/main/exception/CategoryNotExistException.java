package ru.yandex.practicum.main.exception;

public class CategoryNotExistException extends RuntimeException {
    public CategoryNotExistException(String message) {
        super(message);
    }
}
