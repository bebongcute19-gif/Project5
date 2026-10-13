package com.omnichannel.order.service;

import com.omnichannel.order.exception.BadRequestException;
import com.omnichannel.product.grpc.ProductGrpcServiceGrpc;
import com.omnichannel.product.grpc.VariantInfo;
import com.omnichannel.product.grpc.VariantRequest;
import com.omnichannel.product.grpc.VariantResponse;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(ProductGrpcClient.class);

    private final ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productGrpcStub;

    public Map<String, VariantInfo> getVariantsInfo(List<String> skuCodes) {
        try {
            if (skuCodes == null || skuCodes.isEmpty()) {
                throw new BadRequestException("SKU codes list cannot be empty");
            }

            VariantRequest request = VariantRequest.newBuilder()
                    .addAllSkuCodes(skuCodes)
                    .build();

            VariantResponse response = productGrpcStub.getProductVariants(request);
            
            return response.getVariantsList().stream()
                    .collect(Collectors.toMap(VariantInfo::getSkuCode, variant -> variant));
        } catch (StatusRuntimeException ex) {
            log.error("gRPC call to Product Service failed: {}", ex.getMessage());
            throw new BadRequestException("Failed to fetch product information from Product Service");
        } catch (Exception ex) {
            log.error("Unexpected error calling Product Service", ex);
            throw new BadRequestException("Unexpected error while fetching product information");
        }
    }

    public VariantInfo getVariantInfo(String skuCode) {
        try {
            if (skuCode == null || skuCode.isBlank()) {
                throw new BadRequestException("SKU code cannot be empty");
            }

            VariantRequest request = VariantRequest.newBuilder()
                    .addSkuCodes(skuCode)
                    .build();

            VariantResponse response = productGrpcStub.getProductVariants(request);
            
            if (response.getVariantsList().isEmpty()) {
                throw new BadRequestException("Product variant not found for SKU: " + skuCode);
            }

            return response.getVariants(0);
        } catch (StatusRuntimeException ex) {
            log.error("gRPC call to Product Service failed: {}", ex.getMessage());
            throw new BadRequestException("Failed to fetch product information from Product Service");
        } catch (Exception ex) {
            log.error("Unexpected error calling Product Service", ex);
            throw new BadRequestException("Unexpected error while fetching product information");
        }
    }
}
