package ru.yandex.practicum.stats.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.proto.*;
import java.util.Iterator;

@Component
public class RecommendationsClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub recommendationsStub;

    public Iterator<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        return recommendationsStub.getRecommendationsForUser(request);
    }

    public Iterator<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        return recommendationsStub.getSimilarEvents(request);
    }

    public Iterator<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        return recommendationsStub.getInteractionsCount(request);
    }
}