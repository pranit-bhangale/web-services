package io.github.grpc;

import io.github.grpc.service.UserClientService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GrpcClientApplication implements CommandLineRunner {

    private final UserClientService service;

    public GrpcClientApplication(UserClientService service) {
        this.service = service;
    }

    static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        IO.println("gRPC Client Response: "
                + this.service.getUserResponse(1));

        IO.println("Starting gRPC Client Stream...");

        this.service.subscribe(2);
    }
}
