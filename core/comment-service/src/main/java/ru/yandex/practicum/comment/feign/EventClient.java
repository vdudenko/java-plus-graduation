package ru.yandex.practicum.comment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.interaction.dto.event.EventFullDto;

import java.util.List;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/admin/events/{eventId}/exists")
    boolean exists(@PathVariable Long eventId);

    @GetMapping("/events/top/byComment")
    List<EventFullDto> getTopEvents(@RequestParam(name = "count", defaultValue = "5") int count);
}
