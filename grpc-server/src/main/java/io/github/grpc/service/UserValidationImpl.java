package io.github.grpc.service;

import io.github.grpc.dto.UserRequest;
import io.github.grpc.dto.UserResponse;
import io.github.grpc.dto.UserValidationServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.Map;

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
}
