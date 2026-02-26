package ru.yandex.practicum.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.main.dto.event.LocationDto;
import ru.yandex.practicum.main.entity.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationDto toDto(Location location);

    @Mapping(target = "id", source = "id")
    Location toEntity(LocationDto locationDto);
}