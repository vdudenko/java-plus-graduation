package ru.yandex.practicum.stats.server.hit;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.stats.dto.CreateHitDto;
import ru.yandex.practicum.stats.dto.GetStatsDto;
import ru.yandex.practicum.stats.dto.HitDto;
import ru.yandex.practicum.stats.dto.ViewStats;
import ru.yandex.practicum.stats.server.hit.service.HitService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class HitController {
    private final HitService hitService;

    @PostMapping(path = "/users")
    @ResponseStatus(HttpStatus.CREATED)
    public HitDto createUser(@RequestBody @Valid CreateHitDto hitCreateDto) {
        return hitService.create(hitCreateDto);
    }

    @PostMapping(path = "/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public HitDto create(@RequestBody @Valid CreateHitDto hitCreateDto) {
        return hitService.create(hitCreateDto);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(@RequestParam String start,
                                    @RequestParam String end,
                                    @RequestParam(required = false) List<String> uris,
                                    @RequestParam(defaultValue = "false") Boolean unique) {

        LocalDateTime startDate = parseFlexibleDateTime(start);
        LocalDateTime endDate = parseFlexibleDateTime(end);

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date is before start date");
        }

        GetStatsDto getStatsDto = GetStatsDto.builder()
                .start(startDate)
                .end(endDate)
                .uris(uris)
                .unique(unique)
                .build();

        return hitService.getStats(getStatsDto);
    }

    private LocalDateTime parseFlexibleDateTime(String dateTime) {
        if (dateTime == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTime);
        } catch (DateTimeParseException e) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTime, formatter);
        }
    }

}
