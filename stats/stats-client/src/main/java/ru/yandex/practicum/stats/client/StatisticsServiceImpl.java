package ru.yandex.practicum.stats.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.retry.RetryCallback;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.stats.dto.CreateHitDto;
import ru.yandex.practicum.stats.dto.ViewStats;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.yandex.practicum.stats.dto.util.DateFormatter.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private static final String STATS_SERVICE_ID = "stats-server";
    private static final int MAX_ATTEMPTS = 5;
    private static final long BACKOFF_PERIOD = 2000L;

    private final RetryTemplate retryTemplate = createRetryTemplate();

    private RetryTemplate createRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(BACKOFF_PERIOD);
        template.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_ATTEMPTS);
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    private ServiceInstance getInstance() {
        return retryTemplate.execute((RetryCallback<ServiceInstance, IllegalStateException>) context -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);
            if (instances == null || instances.isEmpty()) {
                throw new IllegalStateException(
                        "Сервис '" + STATS_SERVICE_ID + "' не зарегистрирован в Eureka после " +
                                (context.getRetryCount() + 1) + " попыток"
                );
            }
            ServiceInstance instance = instances.getFirst();
            log.debug("Найден экземпляр stats-server: {}:{} (instanceId={})",
                    instance.getHost(), instance.getPort(), instance.getInstanceId());
            return instance;
        });
    }

    private URI makeUri(String path) {
        ServiceInstance instance = getInstance();
        return UriComponentsBuilder
                .fromHttpUrl("http://" + instance.getHost() + ":" + instance.getPort())
                .path(path)
                .build()
                .toUri();
    }

    @Override
    public List<ViewStats> getStats(String start, String end, List<String> uris) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(makeUri("/stats"))
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("unique", false);

        if (uris != null && !uris.isEmpty()) {
            uris.forEach(uri -> builder.queryParam("uris", uri));
        }

        URI uri = builder.build(true).toUri();

        try {
            ViewStats[] response = restTemplate.getForObject(uri, ViewStats[].class);
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Ошибка при получении статистики из stats-server: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void sendStats(List<Long> eventIds, HttpServletRequest request) {
        CreateHitDto hit = CreateHitDto.builder()
                .app("ewm-main")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();

        URI uri = makeUri("/hit");
        try {
            restTemplate.postForEntity(uri, hit, Void.class);
            log.debug("Хит отправлен в stats-server: {}", hit);
        } catch (Exception e) {
            log.error("Ошибка при отправке хита в stats-server: {}", e.getMessage(), e);
        }
    }

    @Override
    public Map<Long, Long> getEventsViews(List<Long> eventIds, HttpServletRequest request, boolean sendHit) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        if (sendHit && request != null) {
            sendStats(eventIds, request);
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStats> stats = getStats(format(start), format(end), uris);

        Map<Long, Long> viewsMap = new HashMap<>();
        eventIds.forEach(id -> viewsMap.put(id, 0L));

        stats.forEach(stat -> {
            Long eventId = extractEventIdFromUri(stat.getUri());
            if (eventId != null && viewsMap.containsKey(eventId)) {
                viewsMap.put(eventId, stat.getHits());
            }
        });
        return viewsMap;
    }

    private Long extractEventIdFromUri(String uri) {
        if (uri == null || !uri.startsWith("/events/")) {
            return null;
        }
        try {
            String idStr = uri.substring("/events/".length());
            return Long.parseLong(idStr);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }
}
