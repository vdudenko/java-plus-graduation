package ru.yandex.practicum.event.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "request-service")
public interface RequestClient {
    @GetMapping("/users/{userId}/events/{eventId}/exists")
    boolean checkUserParticipated(@PathVariable Long userId, @PathVariable Long eventId);
}
