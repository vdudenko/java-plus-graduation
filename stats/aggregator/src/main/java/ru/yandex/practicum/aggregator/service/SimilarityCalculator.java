package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.ActionTypeAvro;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SimilarityCalculator {
    // eventId → userId → weight (максимальный вес действия пользователя для события)
    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();

    // eventId → сумма всех весов пользователей для этого события
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();

    // first event → (second event → сумма минимальных весов по общим пользователям)
    // Ключи упорядочены: first < second
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    public List<EventSimilarityAvro> processUserAction(long userId, long eventId, ActionTypeAvro actionType) {
        double newWeight = getActionWeight(actionType);

        Map<Long, Double> userWeights = userEventWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Double oldWeight = userWeights.getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            return null;
        }

        userWeights.put(userId, newWeight);
        double delta = newWeight - oldWeight;
        eventWeightSums.merge(eventId, delta, Double::sum);

        List<EventSimilarityAvro> similarities = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : userEventWeights.entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == eventId) continue;

            Double weightInOther = entry.getValue().get(userId);
//            if (weightInOther == null) continue;

            updateSimilarityPair(eventId, otherEventId, oldWeight, newWeight, weightInOther)
                    .ifPresent(similarities::add);
        }

        sendSimilarities(similarities);
        return similarities;
    }

    private double getActionWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }

    private Optional<EventSimilarityAvro> updateSimilarityPair(long eventA, long eventB,
                                                               double oldWA, double newWA, double weightB) {
        double oldMin = Math.min(oldWA, weightB);
        double newMin = Math.min(newWA, weightB);
        double deltaMin = newMin - oldMin;

        if (deltaMin == 0.0) {
            return Optional.empty();
        }

        double currentMinSum = updateMinWeightsSum(eventA, eventB, deltaMin);

        double normA = Math.sqrt(eventWeightSums.getOrDefault(eventA, 0.0));
        double normB = Math.sqrt(eventWeightSums.getOrDefault(eventB, 0.0));

        if (normA == 0 || normB == 0) return Optional.empty();

        double score = currentMinSum / (normA * normB);

        score = Math.round(score * 100.0) / 100.0;

        return Optional.of(createSimilarityAvro(eventA, eventB, score));
    }

    private double updateMinWeightsSum(long a, long b, double delta) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);
        return minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
    }

    private EventSimilarityAvro createSimilarityAvro(long a, long b, double score) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(Math.min(a, b))
                .setEventB(Math.max(a, b))
                .setScore(score)
                .setTimestamp(Instant.now())
                .build();
    }

    private void sendSimilarities(List<EventSimilarityAvro> list) {
    }

    public List<EventSimilarityAvro> getUpdatedSimilarities(long triggered, ActionTypeAvro actionType) {
        return Collections.emptyList();
    }
}