package io.github.grpc.service;

import io.github.grpc.dto.UserRequest;
import io.github.grpc.dto.UserResponse;
import io.github.grpc.dto.UserValidationServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@GrpcService
public class UserValidationImpl extends UserValidationServiceGrpc.UserValidationServiceImplBase {

    private static final Map<Integer, String> USERS = Map.of(
            1, "John",
            2, "David",
            3, "Smith",
            4, "Rahul"
    );

    @Override
    public void isUserExists(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        int id = request.getId();
        boolean exists = USERS.containsKey(id);
        UserResponse build = UserResponse.newBuilder()
                .setStatus(exists)
                .build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }

    @Override
    public void subscribeUser(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        ServerCallStreamObserver<UserResponse> observer = (ServerCallStreamObserver<UserResponse>) responseObserver;
        int id = request.getId();
        Thread.ofVirtual().start(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    if (observer.isCancelled()) {
                        System.out.println("Client cancelled: " + id);
                        return;
                    }
                    UserResponse response = UserResponse.newBuilder()
                            .setStatus(true)
                            .setValue(new Random().nextInt(200))
                            .build();
                    observer.onNext(response);
                    System.out.println("Sent update: " + i);
                    TimeUnit.SECONDS.sleep(1);
                }
                if (!observer.isCancelled()) {
                    observer.onCompleted();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!observer.isCancelled()) {
                    observer.onError(Status.CANCELLED.withDescription("Streaming interrupted").asRuntimeException());
                }
            }
        });
    }
}
