package io.github.grpc.service;

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

        // Asynchronous server-streaming stub
        this.asyncValidation = UserValidationServiceGrpc
                .newStub(channel)
                .withDeadlineAfter(60, TimeUnit.SECONDS);
    }

    public UserResponse getUserResponse(int id) {
        UserRequest request = UserRequest.newBuilder().setId(id).build();
        return this.validation.isUserExists(request);
    }

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
}
