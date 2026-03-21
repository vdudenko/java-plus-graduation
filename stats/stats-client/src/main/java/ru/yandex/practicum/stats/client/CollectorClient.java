package ru.yandex.practicum.stats.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.stats.proto.UserActionControllerGrpc;
import ru.yandex.practicum.stats.proto.UserActionProto;
import com.google.protobuf.Empty;

@Component
public class CollectorClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public Empty sendUserAction(UserActionProto request) {
        return collectorStub.collectUserAction(request);
    }
}