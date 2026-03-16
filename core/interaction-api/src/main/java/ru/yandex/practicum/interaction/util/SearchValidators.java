package ru.yandex.practicum.interaction.util;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.interaction.enums.EventState;
import ru.yandex.practicum.interaction.enums.SortValue;

import java.util.List;

@UtilityClass
public class SearchValidators {
    public static boolean hasUsers(List<Long> users) {
        return users != null && !users.isEmpty();
    }

    public static boolean hasCategories(List<Long> categories) {
        return categories != null && !categories.isEmpty();
    }

    public static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    public static boolean hasStates(List<EventState> states) {
        return states != null;
    }

    public static boolean shouldSort(SortValue sort) {
        return sort != null;
    }

    public static boolean isOnlyAvailable(Boolean onlyAvailable) {
        return Boolean.TRUE.equals(onlyAvailable);
    }
}
