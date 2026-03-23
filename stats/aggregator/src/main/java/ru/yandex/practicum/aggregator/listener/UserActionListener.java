package ru.yandex.practicum.aggregator.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.service.SimilarityCalculator;
import ru.yandex.practicum.aggregator.service.SimilarityPublisher;
import ru.yandex.practicum.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {
    private final SimilarityCalculator calc;
    private final SimilarityPublisher pub;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void listen(UserActionAvro rec) {
        try {
            double w = switch (rec.getActionType()) {
                case VIEW -> 0.4;
                case REGISTER -> 0.8;
                case LIKE -> 1.0;
            };
            calc.processUserAction(rec.getUserId(), rec.getEventId(), w);
            pub.publish(rec.getEventId());
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }
}