package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.request.CreateProductVariantRequest;
import com.omnichannel.product.dto.request.UpdateProductVariantRequest;
import com.omnichannel.product.dto.response.ProductVariantResponse;
import com.omnichannel.product.entity.ProductVariant;
import com.omnichannel.product.exception.BadRequestException;
import com.omnichannel.product.exception.DuplicateResourceException;
import com.omnichannel.product.exception.ResourceNotFoundException;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private ProductVariantServiceImpl productVariantService;

    private UUID productId;
    private UUID variantId;
    private ProductVariant testVariant;
    private CreateProductVariantRequest createRequest;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        testVariant = ProductVariant.builder()
                .id(variantId)
                .productId(productId)
                .skuCode("SKU-001")
                .variantName("Variant 1")
                .regularPrice(new BigDecimal("100.00"))
                .salePrice(new BigDecimal("90.00"))
                .currency("VND")
                .active(true)
                .createdAt(Instant.now())
                .build();

        createRequest = CreateProductVariantRequest.builder()
                .skuCode("SKU-NEW")
                .variantName("New Variant")
                .regularPrice(new BigDecimal("150.00"))
                .salePrice(new BigDecimal("130.00"))
                .currency("VND")
                .active(true)
                .build();
    }

    @Test
    void testCreateVariant_Success() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.existsBySkuCode("SKU-NEW")).thenReturn(false);
        
        ProductVariant savedVariant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .skuCode("SKU-NEW")
                .variantName("New Variant")
                .regularPrice(new BigDecimal("150.00"))
                .salePrice(new BigDecimal("130.00"))
                .currency("VND")
                .active(true)
                .createdAt(Instant.now())
                .build();
        
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(savedVariant);

        ProductVariantResponse response = productVariantService.createVariant(productId.toString(), createRequest);

        assertNotNull(response);
        assertEquals("SKU-NEW", response.getSkuCode());
        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).existsBySkuCode("SKU-NEW");
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    void testCreateVariant_DuplicateSKU() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.existsBySkuCode("SKU-NEW")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> 
            productVariantService.createVariant(productId.toString(), createRequest)
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).existsBySkuCode("SKU-NEW");
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void testCreateVariant_ProductNotFound() {
        when(productRepository.existsById(productId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
            productVariantService.createVariant(productId.toString(), createRequest)
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void testCreateVariant_InvalidPrice() {
        when(productRepository.existsById(productId)).thenReturn(true);

        CreateProductVariantRequest badRequest = CreateProductVariantRequest.builder()
                .skuCode("SKU-NEW")
                .variantName("Bad Variant")
                .regularPrice(new BigDecimal("-10"))
                .currency("VND")
                .build();

        assertThrows(BadRequestException.class, () ->
            productVariantService.createVariant(productId.toString(), badRequest)
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void testCreateVariant_DataIntegrityViolation() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.existsBySkuCode("SKU-NEW")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate SKU"));

        assertThrows(DuplicateResourceException.class, () ->
            productVariantService.createVariant(productId.toString(), createRequest)
        );
    }

    @Test
    void testGetVariantById_Success() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));

        ProductVariantResponse response = productVariantService.getVariantById(productId.toString(), variantId.toString());

        assertNotNull(response);
        assertEquals("SKU-001", response.getSkuCode());
        assertEquals(productId.toString(), response.getProductId());
        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).findById(variantId);
    }

    @Test
    void testGetVariantById_VariantNotFound() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            productVariantService.getVariantById(productId.toString(), variantId.toString())
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).findById(variantId);
    }

    @Test
    void testGetVariantById_VariantDoesNotBelongToProduct() {
        UUID otherProductId = UUID.randomUUID();
        ProductVariant variantFromOtherProduct = ProductVariant.builder()
                .id(variantId)
                .productId(otherProductId)
                .skuCode("SKU-001")
                .variantName("Variant 1")
                .regularPrice(new BigDecimal("100.00"))
                .currency("VND")
                .active(true)
                .build();

        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variantFromOtherProduct));

        assertThrows(BadRequestException.class, () ->
            productVariantService.getVariantById(productId.toString(), variantId.toString())
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).findById(variantId);
    }

    @Test
    void testListVariants_Success() {
        ProductVariant variant2 = ProductVariant.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .skuCode("SKU-002")
                .variantName("Variant 2")
                .regularPrice(new BigDecimal("200.00"))
                .currency("VND")
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.findByProductId(productId))
                .thenReturn(List.of(testVariant, variant2));

        List<ProductVariantResponse> responses = productVariantService.listVariants(productId.toString());

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).findByProductId(productId);
    }

    @Test
    void testListVariants_ProductNotFound() {
        when(productRepository.existsById(productId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
            productVariantService.listVariants(productId.toString())
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, never()).findByProductId(any());
    }

    @Test
    void testUpdateVariant_Success() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(testVariant);

        UpdateProductVariantRequest updateRequest = UpdateProductVariantRequest.builder()
                .variantName("Updated Variant")
                .regularPrice(new BigDecimal("200.00"))
                .build();

        ProductVariantResponse response = productVariantService.updateVariant(
                productId.toString(), variantId.toString(), updateRequest);

        assertNotNull(response);
        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).findById(variantId);
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    void testUpdateVariant_InvalidPrice() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));

        UpdateProductVariantRequest updateRequest = UpdateProductVariantRequest.builder()
                .regularPrice(new BigDecimal("-50"))
                .build();

        assertThrows(BadRequestException.class, () ->
            productVariantService.updateVariant(productId.toString(), variantId.toString(), updateRequest)
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).findById(variantId);
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void testDeleteVariant_Success() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(testVariant);

        productVariantService.deleteVariant(productId.toString(), variantId.toString());

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).findById(variantId);
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    void testDeleteVariant_VariantNotFound() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            productVariantService.deleteVariant(productId.toString(), variantId.toString())
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, times(1)).findById(variantId);
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    void testCreateVariant_ConcurrentDuplicateSKU() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productVariantRepository.existsBySkuCode("SKU-NEW")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThrows(DuplicateResourceException.class, () ->
            productVariantService.createVariant(productId.toString(), createRequest)
        );
    }

    @Test
    void testCreateVariant_InvalidSalePrice() {
        when(productRepository.existsById(productId)).thenReturn(true);

        CreateProductVariantRequest badRequest = CreateProductVariantRequest.builder()
                .skuCode("SKU-NEW")
                .variantName("Bad Variant")
                .regularPrice(new BigDecimal("100.00"))
                .salePrice(new BigDecimal("-20"))
                .currency("VND")
                .build();

        assertThrows(BadRequestException.class, () ->
            productVariantService.createVariant(productId.toString(), badRequest)
        );

        verify(productRepository, times(1)).existsById(productId);
        verify(productVariantRepository, never()).save(any());
    }
}
