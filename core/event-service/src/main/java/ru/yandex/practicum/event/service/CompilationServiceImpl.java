package ru.yandex.practicum.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.event.entity.Compilation;
import ru.yandex.practicum.event.entity.Event;
import ru.yandex.practicum.event.mapper.CompilationMapper;
import ru.yandex.practicum.event.repository.CompilationRepository;
import ru.yandex.practicum.interaction.dto.compilation.CompilationDto;
import ru.yandex.practicum.interaction.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.interaction.dto.compilation.UpdateCompilationRequest;
import ru.yandex.practicum.interaction.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventServiceImpl eventServiceImpl;
    private final CompilationMapper compilationMapper;

    @Override
    public List<CompilationDto> getCompilations(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").descending());
        Page<Compilation> compilations = compilationRepository.findAll(pageable);
        return compilations.getContent()
                .stream()
                .map(compilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository
                .findById(compId).orElseThrow(() -> new NotFoundException("подборка не найдена"));
        return compilationMapper.toCompilationDto(compilation);
    }

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        List<Event> events = new ArrayList<>();
        if (newCompilationDto.hasEvents()) {
            events = newCompilationDto.getEvents().stream()
                    .map(eventServiceImpl::getEventById)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        if (!newCompilationDto.hasPinned()) {
            newCompilationDto.setPinned(false);
        }
        Compilation compilation = compilationMapper.toCompilation(newCompilationDto, events);
        Compilation newCompilation = compilationRepository.save(compilation);
        return compilationMapper.toCompilationDto(newCompilation);
    }

    @Override
    public void deleteCompilationById(Long compId) {
        compilationRepository.deleteById(compId);
    }

    @Override
    public CompilationDto updateCompilationById(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("подборка не найдена"));

        compilationMapper.updateCompilationFields(updateCompilationRequest, compilation);

        if (updateCompilationRequest.hasEvents()) {
            List<Event> events = getEventsFromIds(updateCompilationRequest.getEvents());
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        return compilationMapper.toCompilationDto(updatedCompilation);
    }

    private List<Event> getEventsFromIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }
        return eventIds.stream()
                .map(eventServiceImpl::getEventById)
                .collect(Collectors.toCollection(ArrayList::new));
    }

}