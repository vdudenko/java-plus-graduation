package ru.practicum.stats.server.hit.service;

import ru.yandex.practicum.stats.dto.GetStatsDto;
import ru.yandex.practicum.stats.dto.HitDto;
import ru.yandex.practicum.stats.dto.CreateHitDto;
import ru.yandex.practicum.stats.dto.ViewStats;

import java.util.List;

public interface HitService {
    HitDto create(CreateHitDto hit);

    List<ViewStats> getStats(GetStatsDto getStatsDto);

}
