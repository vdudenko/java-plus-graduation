package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.stats.proto.ActionTypeProto;
import ru.yandex.practicum.stats.proto.UserActionProto;
import ru.yandex.practicum.stats.avro.UserActionAvro;
import ru.yandex.practicum.stats.avro.ActionTypeAvro;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectorService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${collector.kafka.topic:stats.user-actions.v1}")
    private String topic;

    public void collectUserAction(UserActionProto request) {
        Instant instant = Instant.ofEpochSecond(
                request.getTimestamp().getSeconds(),
                request.getTimestamp().getNanos()
        );

        UserActionAvro avroRecord = UserActionAvro.newBuilder()
                .setUserId(request.getUserId())
                .setEventId(request.getEventId())
                .setActionType(convertActionType(request.getActionType()))
                .setTimestamp(instant)
                .build();

        kafkaTemplate.send(topic, avroRecord).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("The message was successfully sent to Kafka: {}", result.getRecordMetadata());
            } else {
                log.error("Error sending a message to Kafka for a user {}", avroRecord.getUserId(), ex);
            }
        });;
    }

    private ActionTypeAvro convertActionType(ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> {
                log.error("Unknown type of action: {}", protoType);
                throw new IllegalArgumentException("Unsupported type of action: " + protoType);
            }
        };
    }
}