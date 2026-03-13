package ru.yandex.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.event.service.EventService;
import ru.yandex.practicum.interaction.dto.event.AdminEventSearchRequest;
import ru.yandex.practicum.interaction.dto.event.EventFullDto;
import ru.yandex.practicum.interaction.dto.event.UpdateEventAdminDto;
import ru.yandex.practicum.interaction.enums.EventState;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> getEvents(
            @RequestParam(name = "users", required = false) List<Long> users,
            @RequestParam(name = "states", required = false) List<EventState> states,
            @RequestParam(name = "categories", required = false) List<Long> categories,
            @RequestParam(name = "rangeStart", required = false) String rangeStart,
            @RequestParam(name = "rangeEnd", required = false) String rangeEnd,
            @RequestParam(name = "from", required = false, defaultValue = "0") Integer from,
            @RequestParam(name = "size", required = false, defaultValue = "10") Integer size) {

        return eventService.getEventsWithParamsByAdmin(
                AdminEventSearchRequest.fromParams(
                        users, states, categories, rangeStart, rangeEnd, from, size));
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable(name = "eventId") Long eventId,
                                    @Valid @RequestBody UpdateEventAdminDto updateEventAdminDto) {
        return eventService.updateEvent(eventId, updateEventAdminDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventWithoutStatistic(@PathVariable(name = "eventId") Long eventId) {
        return eventService.getEventWithoutStatistic(eventId);
    }

    @PutMapping("/{eventId}/confirmed/{confirmedCount}")
    public void updateEventConfirmedRequests(@PathVariable Long eventId,
                                    @PathVariable Long confirmedCount) {
        eventService.updateEventConfirmedRequests(eventId, confirmedCount);
    }

    @GetMapping("/{eventId}/exists")
    public boolean exists(@PathVariable Long eventId) {
        return eventService.exists(eventId);
    }

    @GetMapping("/{categoryId}/existsByCategory")
    public boolean existsByCategoryId(@PathVariable Long categoryId) {
        return eventService.existsByCategoryId(categoryId);
    }
}
