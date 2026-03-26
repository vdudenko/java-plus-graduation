package ru.yandex.practicum.stats.client;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.proto.ActionTypeProto;
import ru.yandex.practicum.stats.proto.UserActionControllerGrpc;
import ru.yandex.practicum.stats.proto.UserActionProto;

import java.time.LocalDateTime;

@Component
public class CollectorClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {
        UserActionProto action = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC)))
                .build();

        collectorStub.collectUserAction(action);
    }
}