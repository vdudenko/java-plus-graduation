package ru.yandex.practicum.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.main.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.main.entity.ParticipationRequest;
import ru.yandex.practicum.main.util.DateFormatter;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    @Mapping(target = "created", expression = "java(formatDate(request.getCreated()))")
    @Mapping(target = "requester", source = "requester.id")
    @Mapping(target = "event", source = "event.id")
    ParticipationRequestDto toDto(ParticipationRequest request);

    default String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DateFormatter.format(dateTime);
    }
}
