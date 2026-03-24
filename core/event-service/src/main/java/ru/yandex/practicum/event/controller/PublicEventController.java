package ru.yandex.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.event.service.EventService;
import ru.yandex.practicum.interaction.dto.event.EventFullDto;
import ru.yandex.practicum.interaction.dto.event.PublicEventSearchRequest;
import ru.yandex.practicum.interaction.enums.SortValue;
import ru.yandex.practicum.stats.client.RecommendationsClient;
import ru.yandex.practicum.stats.client.CollectorClient;
import ru.yandex.practicum.stats.proto.UserPredictionsRequestProto;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController {
    private final EventService eventService;
    private final RecommendationsClient recommendationsClient;
    private final CollectorClient collectorClient;

    @GetMapping("/events")
    public List<EventFullDto> getEvents(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "categories", required = false) List<Long> categories,
            @RequestParam(name = "paid", required = false) Boolean paid,
            @RequestParam(name = "rangeStart", required = false) String rangeStart,
            @RequestParam(name = "rangeEnd", required = false) String rangeEnd,
            @RequestParam(name = "onlyAvailable", required = false) Boolean onlyAvailable,
            @RequestParam(name = "sort", required = false) SortValue sort,
            @RequestParam(name = "from", required = false, defaultValue = "0") Integer from,
            @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
            HttpServletRequest request) {

        PublicEventSearchRequest searchRequest = PublicEventSearchRequest.fromParams(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        return eventService.getEventsWithParamsByUser(searchRequest, request);
    }

    @GetMapping("/events/{id}")
    public EventFullDto getEvent(@PathVariable Long id,
                                 @RequestHeader("X-EWM-USER-ID") long userId,
                                 HttpServletRequest request) {
        return eventService.getEvent(id, userId, request);
    }

    @GetMapping("/events/top/byComment")
    public List<EventFullDto> getTopEvents(@RequestParam(name = "count", defaultValue = "5") int count) {
        return eventService.getTopEvent(count);
    }

    @GetMapping("/events/recommendations")
    public ResponseEntity<List<EventFullDto>> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") Integer maxResults) {

        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        var recs = recommendationsClient.getRecommendationsForUser(request);
        List<EventFullDto> result = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(recs, Spliterator.ORDERED), false)
                .map(r -> {
                    EventFullDto dto = new EventFullDto();
                    dto.setId(r.getEventId());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/events/{eventId}/like")
    public ResponseEntity<Void> likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {

        eventService.addLike(userId, eventId);
        return ResponseEntity.noContent().build();
    }
}
