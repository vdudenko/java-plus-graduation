package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SimilarityCalculator {
    private final Map<Long, Map<Long, Double>> eventUserRatings = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventRatingSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minRatingSums = new ConcurrentHashMap<>();

    public void processUserAction(long userId, long eventId, double rating) {
        Map<Long, Double> userRatings = eventUserRatings.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Double old = userRatings.put(userId, rating);
        if (old != null && rating <= old) return;

        double delta = rating - (old != null ? old : 0.0);
        eventRatingSums.compute(eventId, (k, v) -> (v == null ? 0.0 : v) + delta);

        for (Long other : eventUserRatings.keySet()) {
            if (other.equals(eventId)) continue;
            if (!eventUserRatings.get(other).containsKey(userId)) continue;
            updatePair(eventId, other);
        }
    }

    private void updatePair(long a, long b) {
        long first = Math.min(a, b), second = Math.max(a, b);
        double sum = calculateMinSum(first, second);
        minRatingSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>()).put(second, sum);
    }

    private double calculateMinSum(long e1, long e2) {
        Map<Long, Double> r1 = eventUserRatings.getOrDefault(e1, Map.of());
        Map<Long, Double> r2 = eventUserRatings.getOrDefault(e2, Map.of());
        double sum = 0;
        for (Long u : r1.keySet()) if (r2.containsKey(u)) sum += Math.min(r1.get(u), r2.get(u));
        return sum;
    }

    public double calculateSimilarity(long e1, long e2) {
        long first = Math.min(e1, e2), second = Math.max(e1, e2);
        double sMin = minRatingSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
        double s1 = eventRatingSums.getOrDefault(e1, 0.0), s2 = eventRatingSums.getOrDefault(e2, 0.0);
        return (s1 > 0 && s2 > 0) ? sMin / Math.sqrt(s1 * s2) : 0.0;
    }

    public Map<Long, Double> getEventRatingSums() { return new ConcurrentHashMap<>(eventRatingSums); }
}