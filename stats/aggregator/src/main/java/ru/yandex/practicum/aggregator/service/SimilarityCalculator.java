package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.avro.ActionTypeAvro;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityCalculator {
    private final SimilarityPublisher pub;
    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    private final Map<String, Double> minWeightsSums = new ConcurrentHashMap<>();

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    public void processUserAction(long userId, long eventId, ActionTypeAvro actionType, Instant actionTimestamp) {
        double newWeight = getActionWeight(actionType);

        Map<Long, Double> userWeights = userEventWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Double oldWeight = userWeights.getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            return;
        }

        userWeights.put(userId, newWeight);
        double delta = newWeight - oldWeight;
        eventWeightSums.merge(eventId, delta, Double::sum);

        for (Map.Entry<Long, Map<Long, Double>> entry : userEventWeights.entrySet()) {
            long eventB = entry.getKey();
            if (eventB == eventId) continue;

            Map<Long, Double> otherWeights = entry.getValue();
            if (!otherWeights.containsKey(userId)) {
                continue;
            }

            Double otherWeight = otherWeights.get(userId);
            Double pairMinSum = updatePairMinSum(eventId, eventB, oldWeight, newWeight, otherWeight);
            Double similarity = calculatePairSimilarity(eventId, eventB, pairMinSum);

            updateSimilarityPair(eventId, eventB, similarity, actionTimestamp).ifPresent(pub::publish);
        }
    }

    private double getActionWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }

    private Optional<EventSimilarityAvro> updateSimilarityPair(long eventA, long eventB, Double sim, Instant timestamp) {
        return Optional.of(createSimilarityAvro(eventA, eventB, sim, timestamp));
    }

    private Double updatePairMinSum(long eventA, long eventB, double oldWA, double newWA, double weightB) {
        String pairKey = buildPairKey(eventA, eventB);

        Double oldMin = Math.min(oldWA, weightB);
        Double newMin = Math.min(newWA, weightB);

        if (oldMin.equals(newMin)) {
            return minWeightsSums.getOrDefault(pairKey, 0.0);
        }

        Double currentSum = minWeightsSums.getOrDefault(pairKey, 0.0);
        Double updatedSum = currentSum - oldMin + newMin;
        minWeightsSums.put(pairKey, updatedSum);

        return updatedSum;
    }

    private Double calculatePairSimilarity(Long eventA, Long eventB, Double min) {
        if (min == 0.0) {
            return 0.0;
        }

        Double sumA = eventWeightSums.getOrDefault(eventA, 0.0);
        Double sumB = eventWeightSums.getOrDefault(eventB, 0.0);

        if (sumA == 0.0 || sumB == 0.0) {
            return 0.0;
        }

        return min / (Math.sqrt(sumA) * Math.sqrt(sumB));
    }

    private String buildPairKey(long id1, long id2) {
        long first = Math.min(id1, id2);
        long second = Math.max(id1, id2);
        return first + "_" + second;
    }

    private EventSimilarityAvro createSimilarityAvro(long a, long b, double score,Instant timestamp) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(Math.min(a, b))
                .setEventB(Math.max(a, b))
                .setScore(score)
                .setTimestamp(timestamp)
                .build();
    }
}