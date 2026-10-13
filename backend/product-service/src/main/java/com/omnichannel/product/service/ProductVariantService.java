package com.omnichannel.product.service;

import com.omnichannel.product.dto.request.CreateProductVariantRequest;
import com.omnichannel.product.dto.request.UpdateProductVariantRequest;
import com.omnichannel.product.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {
    
    ProductVariantResponse createVariant(String productId, CreateProductVariantRequest request);
    
    ProductVariantResponse getVariantById(String productId, String variantId);
    
    List<ProductVariantResponse> listVariants(String productId);
    
    ProductVariantResponse updateVariant(String productId, String variantId, UpdateProductVariantRequest request);
    
    void deleteVariant(String productId, String variantId);
}
