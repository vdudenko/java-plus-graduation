package ru.yandex.practicum.category.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/admin/events/{categoryId}/existsByCategory")
    boolean existsByCategoryId(@PathVariable Long categoryId);
}
