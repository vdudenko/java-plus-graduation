package ru.yandex.practicum.comment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.Map;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/admin/users/{userId}/exists")
    boolean existsById(@PathVariable Long userId);

    @GetMapping("/admin/users/names")
    Map<Long, String> getNamesByIds(@RequestParam("ids") Collection<Long> ids);
}
