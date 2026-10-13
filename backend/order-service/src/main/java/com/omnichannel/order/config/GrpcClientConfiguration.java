
package com.omnichannel.order.config;

import com.omnichannel.product.grpc.ProductGrpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel productServiceChannel(
            @Value("${product.grpc.host}") String host,
            @Value("${product.grpc.port}") int port) {

        return ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @Bean
    public ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productGrpcStub(
            ManagedChannel productServiceChannel) {

        return ProductGrpcServiceGrpc
                .newBlockingStub(productServiceChannel);
    }
}
