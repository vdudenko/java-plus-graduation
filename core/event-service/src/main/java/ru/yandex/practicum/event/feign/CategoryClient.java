package ru.yandex.practicum.event.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "category-service")
public interface CategoryClient {
    @GetMapping("/admin/categories/{categoryId}/exists")
    boolean existsById(@PathVariable Long categoryId);
}
