package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityPublisher {
    private static final String TOPIC = "stats.events-similarity.v1";
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    public void publish(List<EventSimilarityAvro> similarities) {
        for (EventSimilarityAvro sim : similarities) {
            try {
                kafkaTemplate.send(TOPIC, String.valueOf(sim.getEventA()), sim)
                        .get(10, TimeUnit.SECONDS);
                log.debug("Sent: e1={}, e2={}, sim={}",
                        sim.getEventA(), sim.getEventB(), sim.getScore());
            } catch (Exception e) {
                log.warn("First send failed: e1={}, e2={}, retrying...",
                        sim.getEventA(), sim.getEventB());
                try {
                    kafkaTemplate.send(TOPIC, String.valueOf(sim.getEventA()), sim)
                            .get(10, TimeUnit.SECONDS);
                    log.debug("Retry succeeded: e1={}, e2={}", sim.getEventA(), sim.getEventB());
                } catch (Exception retryEx) {
                    log.error("Failed to send after retry: e1={}, e2={}, error={}",
                            sim.getEventA(), sim.getEventB(), retryEx.getMessage());
                }
            }
        }
    }
}