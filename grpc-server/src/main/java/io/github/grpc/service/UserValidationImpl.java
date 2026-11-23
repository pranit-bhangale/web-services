package io.github.grpc.service;

import io.github.grpc.dto.UserBatchRequest;
import io.github.grpc.dto.UserBatchResponse;
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
import java.util.concurrent.atomic.AtomicInteger;

@GrpcService
public class UserValidationImpl extends UserValidationServiceGrpc.UserValidationServiceImplBase {

    private static final Map<Integer, String> USERS = Map.of(
            1, "John",
            2, "David",
            3, "Smith",
            4, "Rahul"
    );

    // Unary RPC
    @Override
    public void isUserExists(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        int id = request.getId();
        boolean exists = USERS.containsKey(id);
        UserResponse response = UserResponse.newBuilder()
                .setStatus(exists)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Server-side streaming
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
                if (!observer.isCancelled()) observer.onCompleted();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!observer.isCancelled()) {
                    observer.onError(Status.CANCELLED
                            .withDescription("Streaming interrupted")
                            .asRuntimeException());
                }
            }
        });
    }

    // Client-side streaming
    @Override
    public StreamObserver<UserBatchRequest> validateUsers(StreamObserver<UserBatchResponse> responseObserver) {
        AtomicInteger totalUsers = new AtomicInteger(0);
        AtomicInteger existingUsers = new AtomicInteger(0);
        return new StreamObserver<>() {
            @Override
            public void onNext(UserBatchRequest request) {
                int id = request.getId();
                totalUsers.incrementAndGet();
                if (USERS.containsKey(id)) {
                    existingUsers.incrementAndGet();
                    System.out.println("User exists: " + USERS.get(id) + ", ID: " + id);
                } else {
                    System.out.println("User not found, ID: " + id);
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Client streaming failed: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                UserBatchResponse response = UserBatchResponse.newBuilder()
                        .setStatus(existingUsers.get() == totalUsers.get())
                        .setMessage("Total users received: " + totalUsers.get() + ", Existing users: " + existingUsers.get())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                System.out.println("Client streaming completed");
            }
        };
    }

    @Override
    public StreamObserver<UserBatchRequest> validateUsersBidirectional(StreamObserver<UserBatchResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(UserBatchRequest request) {
                int id = request.getId();
                boolean exists = USERS.containsKey(id);
                String message = exists ? "User found: " + USERS.get(id) : "User not found: " + id;
                System.out.println("Received from client: " + id);
                UserBatchResponse response = UserBatchResponse.newBuilder()
                        .setStatus(exists)
                        .setMessage(message)
                        .build();
                responseObserver.onNext(response);
                System.out.println("Response sent: " + message);
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Bidirectional streaming error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Client finished sending messages");
                responseObserver.onCompleted();
            }
        };
    }
}
