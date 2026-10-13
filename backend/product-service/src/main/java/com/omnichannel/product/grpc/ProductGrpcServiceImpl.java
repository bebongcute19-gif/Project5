
package com.omnichannel.product.grpc;

import com.omnichannel.product.entity.Product;
import com.omnichannel.product.entity.ProductVariant;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.repository.ProductVariantRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class ProductGrpcServiceImpl
        extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    @Override
    public void getProductVariants(
            VariantRequest request,
            StreamObserver<VariantResponse> responseObserver) {

        try {
            VariantResponse.Builder response = VariantResponse.newBuilder();

            for (String skuCode : request.getSkuCodesList()) {

                ProductVariant variant = variantRepository
                        .findBySkuCode(skuCode)
                        .orElse(null);

                if (variant == null) {
                    continue;
                }

                Product product = productRepository
                        .findById(variant.getProductId())
                        .orElse(null);

                BigDecimal price = variant.getSalePrice() != null
                        ? variant.getSalePrice()
                        : variant.getRegularPrice();

                boolean active = Boolean.TRUE.equals(variant.getActive())
                        && product != null
                        && "ACTIVE".equalsIgnoreCase(product.getStatus());

                VariantInfo info = VariantInfo.newBuilder()
                        .setSkuCode(variant.getSkuCode())
                        .setProductId(variant.getProductId().toString())
                        .setProductName(
                                product != null ? product.getName() : "")
                        .setPrice(price.doubleValue())
                        .setIsActive(active)
                        .setCurrency(variant.getCurrency() != null
                                ? variant.getCurrency() : "VND")
                        .build();

                response.addVariants(info);
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to fetch product variants")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}
