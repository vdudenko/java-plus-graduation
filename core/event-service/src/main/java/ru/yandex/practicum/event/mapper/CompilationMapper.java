package ru.yandex.practicum.event.mapper;

import org.mapstruct.*;
import ru.yandex.practicum.event.entity.Compilation;
import ru.yandex.practicum.event.entity.Event;
import ru.yandex.practicum.interaction.dto.compilation.CompilationDto;
import ru.yandex.practicum.interaction.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.interaction.dto.compilation.UpdateCompilationRequest;

import java.util.List;

@Mapper(componentModel = "spring", uses = EventMapper.class)
public interface CompilationMapper {

    CompilationDto toCompilationDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", source = "newCompilationDto.title")
    @Mapping(target = "pinned", source = "newCompilationDto.pinned")
    @Mapping(target = "events", source = "events")
    Compilation toCompilation(NewCompilationDto newCompilationDto, List<Event> events);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "events", ignore = true)
    void updateCompilationFields(UpdateCompilationRequest updateCompilationRequest,
                                 @MappingTarget Compilation compilation);
}
