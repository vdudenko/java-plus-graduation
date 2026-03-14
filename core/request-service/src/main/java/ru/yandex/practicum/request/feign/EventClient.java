package ru.yandex.practicum.request.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.event.EventFullDto;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/admin/events/{eventId}/exists")
    boolean existsByCategoryId(@PathVariable Long eventId);

    @GetMapping("/admin/events/{id}")
    EventFullDto getEventWithoutStatistic(@PathVariable Long id);

    @PutMapping("/admin/events/{eventId}/confirmed/{confirmedCount}")
    void updateEventConfirmedRequests(@PathVariable Long eventId, @PathVariable Long confirmedCount);
}
