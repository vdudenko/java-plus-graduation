package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityPublisher {
    private static final String TOPIC = "stats.events-similarity.v1";
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final SimilarityCalculator calc;

    public void publish(long triggered) {
        for (Long other : calc.getEventRatingSums().keySet()) {
            if (other.equals(triggered)) continue;
            double sim = calc.calculateSimilarity(triggered, other);
            long e1 = Math.min(triggered, other), e2 = Math.max(triggered, other);
            EventSimilarityAvro msg = EventSimilarityAvro.newBuilder()
                    .setEventA(e1).setEventB(e2).setScore(sim).setTimestamp(Instant.now()).build();

            kafkaTemplate.send(new ProducerRecord<>(TOPIC, String.valueOf(e1), msg));
        }
    }
}