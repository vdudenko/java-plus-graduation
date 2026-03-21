package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AggregatorService {
    private final SimilarityCalculator calc;
    private final SimilarityPublisher pub;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<Long, Set<Long>> userEventMap = new ConcurrentHashMap<>();
    private final Map<Map.Entry<Long, Long>, Integer> cooccurrenceMap = new ConcurrentHashMap<>();

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void listen(UserActionAvro action) {
        log.info("Received action: userId={}, eventId={}", action.getUserId(), action.getEventId());

        double weight = switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };

        calc.processUserAction(action.getUserId(), action.getEventId(), weight);
        pub.publish(action.getEventId());
    }

    private void calculateSimilarities(Long userId) {
        Set<Long> events = userEventMap.get(userId);
        if (events == null || events.size() < 2) return;

        List<Long> eventsList = new ArrayList<>(events);
        for (int i = 0; i < eventsList.size(); i++) {
            for (int j = i + 1; j < eventsList.size(); j++) {
                Long event1 = eventsList.get(i);
                Long event2 = eventsList.get(j);
                cooccurrenceMap.merge(Map.entry(event1, event2), 1, Integer::sum);
                cooccurrenceMap.merge(Map.entry(event2, event1), 1, Integer::sum);
            }
        }
        sendSimilarities();
    }

    private void sendSimilarities() {
        cooccurrenceMap.forEach((key, count) -> {
            EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                    .setEventA(key.getKey())
                    .setEventB(key.getValue())
                    .setScore(count.doubleValue())
                    .setTimestamp(Instant.now())
                    .build();
            kafkaTemplate.send("stats.events-similarity.v1", key.getKey().toString(), similarity);
        });
        cooccurrenceMap.clear();
    }
}