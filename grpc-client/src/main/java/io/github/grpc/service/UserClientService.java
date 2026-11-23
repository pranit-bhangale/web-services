package io.github.grpc.service;

import io.github.grpc.dto.UserBatchRequest;
import io.github.grpc.dto.UserBatchResponse;
import io.github.grpc.dto.UserRequest;
import io.github.grpc.dto.UserResponse;
import io.github.grpc.dto.UserValidationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserClientService {

    private final UserValidationServiceGrpc.UserValidationServiceBlockingStub validation;

    private final UserValidationServiceGrpc.UserValidationServiceStub asyncValidation;

    public UserClientService(GrpcChannelFactory channelFactory) {

        ManagedChannel channel = channelFactory.createChannel("user-service");

        // Unary blocking stub
        this.validation = UserValidationServiceGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(10, TimeUnit.SECONDS);

        // Asynchronous stub
        this.asyncValidation = UserValidationServiceGrpc
                .newStub(channel)
                .withDeadlineAfter(60, TimeUnit.SECONDS);
    }

    // Unary RPC
    public UserResponse getUserResponse(int id) {
        UserRequest request = UserRequest.newBuilder()
                .setId(id)
                .build();

        return this.validation.isUserExists(request);
    }

    // Server-side streaming
    public void subscribe(int id) {

        UserRequest request = UserRequest.newBuilder()
                .setId(id)
                .build();

        asyncValidation.subscribeUser(request, new StreamObserver<>() {

            @Override
            public void onNext(UserResponse response) {
                System.out.println("Received: " + response.getValue());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Streaming error:");
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Streaming completed");
            }
        });
    }

    // Client-side streaming
    public void validateUsers() {
        StreamObserver<UserBatchRequest> requestObserver = asyncValidation.validateUsers(new StreamObserver<>() {
            @Override
            public void onNext(UserBatchResponse response) {
                System.out.println("Server response: " + response.getMessage());
                System.out.println("Status: " + response.getStatus());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Client streaming error:");
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Client streaming completed");
            }
        });
        requestObserver.onNext(UserBatchRequest.newBuilder().setId(101).build());
        requestObserver.onNext(UserBatchRequest.newBuilder().setId(102).build());
        requestObserver.onNext(UserBatchRequest.newBuilder().setId(103).build());
        requestObserver.onCompleted();
    }

    public void validateUsersBidirectional() {
        StreamObserver<UserBatchResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(UserBatchResponse response) {
                System.out.println("Received from server: " + response.getMessage());
                System.out.println("User exists: " + response.getStatus());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Bidirectional streaming error:");
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Bidirectional streaming completed");
            }
        };
        StreamObserver<UserBatchRequest> requestObserver =
                asyncValidation.validateUsersBidirectional(responseObserver);
        requestObserver.onNext(UserBatchRequest.newBuilder().setId(1).build());
        requestObserver.onNext(UserBatchRequest.newBuilder().setId(10).build());
        requestObserver.onNext(UserBatchRequest.newBuilder().setId(2).build());
        requestObserver.onNext(UserBatchRequest.newBuilder().setId(20).build());
        requestObserver.onCompleted();
    }
}
