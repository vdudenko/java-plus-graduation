package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityPublisher {
    private static final String TOPIC = "stats.events-similarity.v1";

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final SimilarityCalculator calc;

    // Храним последние отправленные значения
    private final Map<Map.Entry<Long, Long>, Double> lastSent = new ConcurrentHashMap<>();

    public void publish(long triggered) {
        for (Long other : calc.getEventRatingSums().keySet()) {
            if (other.equals(triggered)) continue;

            long e1 = Math.min(triggered, other);
            long e2 = Math.max(triggered, other);
            double newSim = calc.calculateSimilarity(triggered, other);

            Map.Entry<Long, Long> key = Map.entry(e1, e2);
            Double oldSim = lastSent.get(key);

            // ✅ Отправляем только если значение изменилось
            if (oldSim == null || Math.abs(newSim - oldSim) > 0.0001) {
                EventSimilarityAvro msg = EventSimilarityAvro.newBuilder()
                        .setEventA(e1)
                        .setEventB(e2)
                        .setScore(newSim)
                        .setTimestamp(Instant.now())
                        .build();

                kafkaTemplate.send(TOPIC, String.valueOf(e1), msg)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                lastSent.put(key, newSim);  // ✅ Запоминаем отправленное
                                log.debug("✅ Sent similarity update: e1={}, e2={}, old={}, new={}",
                                        e1, e2, oldSim, newSim);
                            }
                        });
            } else {
                log.debug("⏭️ Skipping unchanged similarity: e1={}, e2={}, score={}", e1, e2, newSim);
            }
        }
    }
}