package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.request.CreateProductVariantRequest;
import com.omnichannel.product.dto.request.UpdateProductVariantRequest;
import com.omnichannel.product.dto.response.ProductVariantResponse;
import com.omnichannel.product.entity.Product;
import com.omnichannel.product.entity.ProductVariant;
import com.omnichannel.product.exception.BadRequestException;
import com.omnichannel.product.exception.DuplicateResourceException;
import com.omnichannel.product.exception.ResourceNotFoundException;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.repository.ProductVariantRepository;
import com.omnichannel.product.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    private void validateProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
    }

    private ProductVariantResponse mapToResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId().toString())
                .productId(variant.getProductId().toString())
                .skuCode(variant.getSkuCode())
                .variantName(variant.getVariantName())
                .regularPrice(variant.getRegularPrice())
                .salePrice(variant.getSalePrice())
                .currency(variant.getCurrency())
                .attributes(variant.getAttributes())
                .active(variant.getActive())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    private void validatePrice(ProductVariant variant) {
        if (variant.getRegularPrice() == null || variant.getRegularPrice().signum() < 0) {
            throw new BadRequestException("Regular price must be greater than or equal to 0");
        }
        if (variant.getSalePrice() != null && variant.getSalePrice().signum() < 0) {
            throw new BadRequestException("Sale price must be greater than or equal to 0");
        }
    }

    @Override
    public ProductVariantResponse createVariant(String productId, CreateProductVariantRequest request) {
        UUID prodId = UUID.fromString(productId);
        validateProduct(prodId);

        // Validate prices
        if (request.getRegularPrice().signum() < 0) {
            throw new BadRequestException("Regular price must be greater than or equal to 0");
        }
        if (request.getSalePrice() != null && request.getSalePrice().signum() < 0) {
            throw new BadRequestException("Sale price must be greater than or equal to 0");
        }

        // Check for duplicate SKU
        if (productVariantRepository.existsBySkuCode(request.getSkuCode())) {
            throw new DuplicateResourceException("A variant with SKU code '" + request.getSkuCode() + "' already exists");
        }

        ProductVariant variant = ProductVariant.builder()
                .productId(prodId)
                .skuCode(request.getSkuCode())
                .variantName(request.getVariantName())
                .regularPrice(request.getRegularPrice())
                .salePrice(request.getSalePrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .attributes(request.getAttributes())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        try {
            ProductVariant savedVariant = productVariantRepository.save(variant);
            log.info("Variant created with id: {}, SKU: {} for product: {}", savedVariant.getId(), savedVariant.getSkuCode(), prodId);
            return mapToResponse(savedVariant);
        } catch (DataIntegrityViolationException e) {
            // Handle concurrent SKU creation
            throw new DuplicateResourceException("A variant with SKU code '" + request.getSkuCode() + "' already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(String productId, String variantId) {
        UUID prodId = UUID.fromString(productId);
        UUID varId = UUID.fromString(variantId);
        
        validateProduct(prodId);

        ProductVariant variant = productVariantRepository.findById(varId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!variant.getProductId().equals(prodId)) {
            throw new BadRequestException("Variant does not belong to product with id: " + productId);
        }

        return mapToResponse(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> listVariants(String productId) {
        UUID prodId = UUID.fromString(productId);
        validateProduct(prodId);

        return productVariantRepository.findByProductId(prodId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductVariantResponse updateVariant(String productId, String variantId, UpdateProductVariantRequest request) {
        UUID prodId = UUID.fromString(productId);
        UUID varId = UUID.fromString(variantId);

        validateProduct(prodId);

        ProductVariant variant = productVariantRepository.findById(varId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!variant.getProductId().equals(prodId)) {
            throw new BadRequestException("Variant does not belong to product with id: " + productId);
        }

        // Validate prices if provided
        if (request.getRegularPrice() != null) {
            if (request.getRegularPrice().signum() < 0) {
                throw new BadRequestException("Regular price must be greater than or equal to 0");
            }
            variant.setRegularPrice(request.getRegularPrice());
        }

        if (request.getSalePrice() != null) {
            if (request.getSalePrice().signum() < 0) {
                throw new BadRequestException("Sale price must be greater than or equal to 0");
            }
            variant.setSalePrice(request.getSalePrice());
        }

        if (request.getVariantName() != null) {
            variant.setVariantName(request.getVariantName());
        }

        if (request.getCurrency() != null) {
            variant.setCurrency(request.getCurrency());
        }

        if (request.getAttributes() != null) {
            variant.setAttributes(request.getAttributes());
        }

        if (request.getActive() != null) {
            variant.setActive(request.getActive());
        }

        ProductVariant updatedVariant = productVariantRepository.save(variant);
        log.info("Variant updated with id: {}", varId);
        return mapToResponse(updatedVariant);
    }

    @Override
    public void deleteVariant(String productId, String variantId) {
        UUID prodId = UUID.fromString(productId);
        UUID varId = UUID.fromString(variantId);

        validateProduct(prodId);

        ProductVariant variant = productVariantRepository.findById(varId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));

        if (!variant.getProductId().equals(prodId)) {
            throw new BadRequestException("Variant does not belong to product with id: " + productId);
        }

        // Soft delete: deactivate the variant
        variant.setActive(false);
        productVariantRepository.save(variant);
        log.info("Variant deactivated with id: {}", varId);
    }
}
