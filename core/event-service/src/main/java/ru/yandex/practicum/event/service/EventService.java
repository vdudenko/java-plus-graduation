package ru.yandex.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.yandex.practicum.event.entity.Event;
import ru.yandex.practicum.interaction.dto.event.*;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> getEvents(Long userId, int from, int size);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserDto updateEventUserDto);

    EventFullDto getEventByUser(Long userId, Long eventId);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminDto updateEventAdminDto);

    EventFullDto getEvent(Long id, HttpServletRequest request);

    List<EventFullDto> getEventsWithParamsByAdmin(AdminEventSearchRequest request);

    List<EventFullDto> getEventsWithParamsByUser(PublicEventSearchRequest request, HttpServletRequest httpRequest);

    Event getEventById(Long eventId);

    List<EventFullDto> getTopEvent(int count);

    boolean exists(Long eventId);

    boolean existsByCategoryId(Long categoryId);

    void updateEventConfirmedRequests(Long eventId, Long confirmedCount);

    EventFullDto getEventWithoutStatistic(Long eventId);
}