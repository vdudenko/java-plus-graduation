package ru.yandex.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        if (old != null && rating <= old) {
            return;
        }

        double delta = rating - (old != null ? old : 0.0);
        eventRatingSums.compute(eventId, (k, v) -> (v == null ? 0.0 : v) + delta);

        for (Long other : eventUserRatings.keySet()) {
            if (other.equals(eventId)) {
                continue;
            }
            if (!eventUserRatings.get(other).containsKey(userId)) {
                continue;
            }
            updatePair(eventId, other);
        }
    }

    private void updatePair(long a, long b) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);
        double sum = calculateMinSum(first, second);
        minRatingSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>()).put(second, sum);
    }

    private double calculateMinSum(long e1, long e2) {
        Map<Long, Double> r1 = eventUserRatings.getOrDefault(e1, Map.of());
        Map<Long, Double> r2 = eventUserRatings.getOrDefault(e2, Map.of());
        double sum = 0;
        for (Long u : r1.keySet()) {
            if (r2.containsKey(u)) {
                sum += Math.min(r1.get(u), r2.get(u));
            }
        }
        return sum;
    }

    public double calculateSimilarity(long e1, long e2) {
        Map<Long, Double> r1 = eventUserRatings.getOrDefault(e1, Map.of());
        Map<Long, Double> r2 = eventUserRatings.getOrDefault(e2, Map.of());

        double dot = 0, norm1 = 0, norm2 = 0;
        Set<Long> allUsers = new HashSet<>(r1.keySet());
        allUsers.addAll(r2.keySet());

        for (Long u : allUsers) {
            double v1 = r1.getOrDefault(u, 0.0);
            double v2 = r2.getOrDefault(u, 0.0);
            dot += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        return (norm1 > 0 && norm2 > 0) ? dot / (Math.sqrt(norm1) * Math.sqrt(norm2)) : 0.0;
    }

    public Map<Long, Double> getEventRatingSums() {
        return new ConcurrentHashMap<>(eventRatingSums);
    }

    public Set<Long> getUsersForEvent(long eventId) {
        return eventUserRatings.getOrDefault(eventId, Map.of()).keySet();
    }
}