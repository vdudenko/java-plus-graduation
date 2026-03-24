package ru.yandex.practicum.aggregator.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.service.SimilarityCalculator;
import ru.yandex.practicum.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {
    private final SimilarityCalculator calc;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void listen(ConsumerRecord<String, UserActionAvro> rec) {
        UserActionAvro action = rec.value();
        calc.processUserAction(
            action.getUserId(),
            action.getEventId(),
            action.getActionType(),
            action.getTimestamp()
        );
    }
}