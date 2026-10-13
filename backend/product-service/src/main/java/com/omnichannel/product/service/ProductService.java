package com.omnichannel.product.service;

import com.omnichannel.product.dto.request.CreateProductRequest;
import com.omnichannel.product.dto.request.UpdateProductRequest;
import com.omnichannel.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    
    ProductResponse createProduct(CreateProductRequest request);
    
    ProductResponse getProductById(String id);
    
    ProductResponse getProductBySlug(String slug);
    
    ProductResponse getProductByIdPublic(String id);
    
    Page<ProductResponse> listProducts(String categoryId, String brandId, String status, Pageable pageable);
    
    Page<ProductResponse> listProductsPublic(String categoryId, String brandId, Pageable pageable);
    
    ProductResponse updateProduct(String id, UpdateProductRequest request);
    
    void deleteProduct(String id);
}
