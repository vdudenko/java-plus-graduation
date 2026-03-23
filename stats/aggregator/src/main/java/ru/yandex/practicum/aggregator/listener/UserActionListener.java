package ru.yandex.practicum.aggregator.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.service.SimilarityCalculator;
import ru.yandex.practicum.aggregator.service.SimilarityPublisher;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.stats.avro.UserActionAvro;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {
    private final SimilarityCalculator calc;
    private final SimilarityPublisher pub;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void listen(ConsumerRecord<String, UserActionAvro> rec) {
        try {
            UserActionAvro action = rec.value();

            // ✅ ProcessUserAction обновляет состояние И возвращает список обновлённых сходств
            List<EventSimilarityAvro> updated = calc.processUserAction(
                    action.getUserId(),
                    action.getEventId(),
                    action.getActionType()
            );

            // ✅ Отправляем только те, что реально изменились
            pub.publish(updated);

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
        }
    }
}