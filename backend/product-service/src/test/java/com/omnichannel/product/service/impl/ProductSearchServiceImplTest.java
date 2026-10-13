package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.response.ProductResponse;
import com.omnichannel.product.entity.Brand;
import com.omnichannel.product.entity.Category;
import com.omnichannel.product.entity.Product;
import com.omnichannel.product.entity.ProductVariant;
import com.omnichannel.product.exception.ResourceNotFoundException;
import com.omnichannel.product.repository.BrandRepository;
import com.omnichannel.product.repository.CategoryRepository;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private ProductSearchServiceImpl productSearchService;

    private UUID productId;
    private UUID categoryId;
    private UUID brandId;
    private Product testProduct;
    private ProductVariant testVariant;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        brandId = UUID.randomUUID();

        testProduct = Product.builder()
                .id(productId)
                .name("Smartphone Pro 15")
                .slug("smartphone-pro-15")
                .description("Flagship mobile smartphone with OLED display")
                .categoryId(categoryId)
                .brandId(brandId)
                .status("ACTIVE")
                .images(List.of("https://example.com/img1.jpg"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testVariant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .skuCode("SP-PRO-15-256")
                .variantName("256GB Black")
                .regularPrice(new BigDecimal("25000000"))
                .salePrice(new BigDecimal("23000000"))
                .currency("VND")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Search with keyword returns matching products via PostgreSQL specification")
    void testSearchWithKeyword() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(variantRepository.findByProductIdIn(List.of(productId))).thenReturn(List.of(testVariant));

        Page<ProductResponse> result = productSearchService.searchProducts(
                "Smartphone", null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        ProductResponse response = result.getContent().get(0);
        assertEquals("Smartphone Pro 15", response.getName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(1, response.getVariants().size());
        assertEquals("SP-PRO-15-256", response.getVariants().get(0).getSkuCode());

        verify(productRepository).findAll(any(Specification.class), eq(pageable));
        verify(variantRepository).findByProductIdIn(List.of(productId));
    }

    @Test
    @DisplayName("Search with category and brand filters")
    void testSearchWithCategoryAndBrand() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(variantRepository.findByProductIdIn(List.of(productId))).thenReturn(List.of(testVariant));

        Page<ProductResponse> result = productSearchService.searchProducts(
                null, categoryId.toString(), brandId.toString(), null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(productId.toString(), result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("Search with price range filter")
    void testSearchWithPriceRange() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(variantRepository.findByProductIdIn(List.of(productId))).thenReturn(List.of(testVariant));

        Page<ProductResponse> result = productSearchService.searchProducts(
                null, null, null, 20000000.0, 30000000.0, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Smartphone Pro 15", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("Search with multiple combined filters including custom status")
    void testSearchWithCombinedFiltersAndStatus() {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(variantRepository.findByProductIdIn(List.of(productId))).thenReturn(List.of(testVariant));

        Page<ProductResponse> result = productSearchService.searchProducts(
                "Phone", categoryId.toString(), brandId.toString(), 10000000.0, 50000000.0, "ACTIVE", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testProduct.getSlug(), result.getContent().get(0).getSlug());
    }

    @Test
    @DisplayName("Search with empty results returns empty page")
    void testSearchEmptyResults() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

        Page<ProductResponse> result = productSearchService.searchProducts(
                "NonexistentProduct", null, null, null, null, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(variantRepository, never()).findByProductIdIn(any());
    }

    @Test
    @DisplayName("indexProduct successfully verifies existing product in PostgreSQL")
    void testIndexProductSuccess() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        assertDoesNotThrow(() -> productSearchService.indexProduct(productId.toString()));
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("indexProduct throws ResourceNotFoundException when product does not exist")
    void testIndexProductNotFound() {
        UUID randomId = UUID.randomUUID();
        when(productRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                productSearchService.indexProduct(randomId.toString()));
    }

    @Test
    @DisplayName("reindexAllProducts executes without error using PostgreSQL")
    void testReindexAllProducts() {
        when(productRepository.count(any(Specification.class))).thenReturn(50L);

        assertDoesNotThrow(() -> productSearchService.reindexAllProducts());
        verify(productRepository).count(any(Specification.class));
    }

    @Test
    @DisplayName("reindexAllProductsWithBatching executes successfully")
    void testReindexAllProductsWithBatching() {
        when(productRepository.count(any(Specification.class))).thenReturn(50L);

        assertDoesNotThrow(() -> productSearchService.reindexAllProductsWithBatching(25));
        verify(productRepository).count(any(Specification.class));
    }

    @Test
    @DisplayName("removeProductFromIndex executes without error")
    void testRemoveProductFromIndex() {
        assertDoesNotThrow(() -> productSearchService.removeProductFromIndex(productId.toString()));
    }
}
