package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.request.CreateProductRequest;
import com.omnichannel.product.dto.request.ProductVariantDTO;
import com.omnichannel.product.dto.request.UpdateProductRequest;
import com.omnichannel.product.dto.response.ProductResponse;
import com.omnichannel.product.entity.Brand;
import com.omnichannel.product.entity.Category;
import com.omnichannel.product.entity.Product;
import com.omnichannel.product.entity.ProductVariant;
import com.omnichannel.product.exception.BadRequestException;
import com.omnichannel.product.exception.DuplicateResourceException;
import com.omnichannel.product.exception.ResourceNotFoundException;
import com.omnichannel.product.repository.BrandRepository;
import com.omnichannel.product.repository.CategoryRepository;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.repository.ProductVariantRepository;
import com.omnichannel.product.service.ProductEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductEventPublisher productEventPublisher;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category validCategory;
    private Brand validBrand;
    private Product testProduct;
    private CreateProductRequest createRequest;
    private ProductVariantDTO variantDTO;
    private UUID categoryId;
    private UUID brandId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        brandId = UUID.randomUUID();
        productId = UUID.randomUUID();

        validCategory = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .slug("electronics")
                .active(true)
                .createdAt(Instant.now())
                .build();

        validBrand = Brand.builder()
                .id(brandId)
                .name("Samsung")
                .slug("samsung")
                .active(true)
                .createdAt(Instant.now())
                .build();

        variantDTO = ProductVariantDTO.builder()
                .skuCode("SKU-001")
                .variantName("Variant 1")
                .regularPrice(1000.0)
                .salePrice(900.0)
                .currency("VND")
                .active(true)
                .build();

        createRequest = CreateProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .categoryId(categoryId.toString())
                .brandId(brandId.toString())
                .status("DRAFT")
                .images(new ArrayList<>())
                .variants(List.of(variantDTO))
                .build();

        testProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .description("Test Description")
                .categoryId(categoryId)
                .brandId(brandId)
                .status("ACTIVE")
                .images(List.of("image1.jpg"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testCreateProductSuccess() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(validBrand));
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        
        Product savedProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .description("Test Description")
                .categoryId(categoryId)
                .brandId(brandId)
                .status("DRAFT")
                .images(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(null);
        when(productVariantRepository.findByProductId(productId)).thenReturn(new ArrayList<>());

        // Act
        ProductResponse response = productService.createProduct(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Test Product", response.getName());
        assertEquals("test-product", response.getSlug());
        assertEquals("DRAFT", response.getStatus());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testCreateProductWithDuplicateSlugThrowsException() {
        // Arrange
        when(productRepository.existsBySlug(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            productService.createProduct(createRequest);
        });
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testCreateProductWithInactiveCategoryThrowsException() {
        // Arrange
        Category inactiveCategory = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .slug("electronics")
                .active(false)
                .createdAt(Instant.now())
                .build();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(inactiveCategory));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            productService.createProduct(createRequest);
        });
    }

    @Test
    void testCreateProductWithInactiveBrandThrowsException() {
        // Arrange
        Brand inactiveBrand = Brand.builder()
                .id(brandId)
                .name("Samsung")
                .slug("samsung")
                .active(false)
                .createdAt(Instant.now())
                .build();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(inactiveBrand));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            productService.createProduct(createRequest);
        });
    }

    @Test
    void testCreateProductWithNonExistentCategoryThrowsException() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.createProduct(createRequest);
        });
    }

    @Test
    void testCreateProductWithNonExistentBrandThrowsException() {
        // Arrange
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.createProduct(createRequest);
        });
    }

    @Test
    void testGetProductByIdSuccess() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productVariantRepository.findByProductId(productId)).thenReturn(new ArrayList<>());

        // Act
        ProductResponse response = productService.getProductById(productId.toString());

        // Assert
        assertNotNull(response);
        assertEquals("Test Product", response.getName());
        assertEquals("test-product", response.getSlug());
    }

    @Test
    void testGetProductByIdNotFoundThrowsException() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById(productId.toString());
        });
    }

    @Test
    void testGetProductBySlugSuccess() {
        // Arrange
        when(productRepository.findBySlug("test-product")).thenReturn(Optional.of(testProduct));
        when(productVariantRepository.findByProductId(productId)).thenReturn(new ArrayList<>());

        // Act
        ProductResponse response = productService.getProductBySlug("test-product");

        // Assert
        assertNotNull(response);
        assertEquals("Test Product", response.getName());
        assertEquals("test-product", response.getSlug());
    }

    @Test
    void testGetProductBySlugNotFoundThrowsException() {
        // Arrange
        when(productRepository.findBySlug("test-product")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductBySlug("test-product");
        });
    }

    @Test
    void testGetProductByIdPublicSuccessForActiveProduct() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productVariantRepository.findByProductId(productId)).thenReturn(new ArrayList<>());

        // Act
        ProductResponse response = productService.getProductByIdPublic(productId.toString());

        // Assert
        assertNotNull(response);
        assertEquals("Test Product", response.getName());
    }

    @Test
    void testGetProductByIdPublicThrowsExceptionForInactiveProduct() {
        // Arrange
        Product inactiveProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .description("Test Description")
                .categoryId(categoryId)
                .brandId(brandId)
                .status("DRAFT")
                .images(List.of("image1.jpg"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(inactiveProduct));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductByIdPublic(productId.toString());
        });
    }

    @Test
    void testUpdateProductSuccess() {
        // Arrange
        UUID newCategoryId = UUID.randomUUID();
        UUID newBrandId = UUID.randomUUID();

        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Updated Product")
                .description("Updated Description")
                .categoryId(newCategoryId.toString())
                .brandId(newBrandId.toString())
                .status("ACTIVE")
                .images(List.of("image1.jpg", "image2.jpg"))
                .build();

        Product existingProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .description("Test Description")
                .categoryId(categoryId)
                .brandId(brandId)
                .status("DRAFT")
                .images(List.of("image1.jpg"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category newCategory = Category.builder()
                .id(newCategoryId)
                .name("Books")
                .slug("books")
                .active(true)
                .createdAt(Instant.now())
                .build();

        Brand newBrand = Brand.builder()
                .id(newBrandId)
                .name("Penguin")
                .slug("penguin")
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
        when(brandRepository.findById(newBrandId)).thenReturn(Optional.of(newBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setUpdatedAt(Instant.now());
            return product;
        });
        when(productVariantRepository.findByProductId(productId)).thenReturn(List.of(
            ProductVariant.builder()
                    .id(UUID.randomUUID())
                    .productId(productId)
                    .skuCode("SKU-001")
                    .regularPrice(new BigDecimal("1000"))
                    .active(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build()
        ));

        // Act
        ProductResponse response = productService.updateProduct(productId.toString(), updateRequest);

        // Assert
        assertNotNull(response);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProductWithDuplicateSlugThrowsException() {
        // Arrange
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .slug("another-slug")
                .name("Updated Product")
                .description("Updated Description")
                .categoryId(categoryId.toString())
                .brandId(brandId.toString())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.existsBySlugAndIdNot("another-slug", productId)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            productService.updateProduct(productId.toString(), updateRequest);
        });
    }

    @Test
    void testUpdateProductNotFoundThrowsException() {
        // Arrange
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Updated Product")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.updateProduct(productId.toString(), updateRequest);
        });
    }

    @Test
    void testDeleteProductSoftDeletesProduct() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        productService.deleteProduct(productId.toString());

        // Assert
        verify(productRepository, times(1)).save(argThat(product ->
                product.getStatus().equals("ARCHIVED")
        ));
    }

    @Test
    void testDeleteProductNotFoundThrowsException() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.deleteProduct(productId.toString());
        });
    }

    @Test
    void testListProductsWithAllFilters() {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByCategoryIdAndBrandIdAndStatus(categoryId, brandId, "ACTIVE", pageable))
                .thenReturn(productPage);
        when(productVariantRepository.findByProductId(productId)).thenReturn(new ArrayList<>());

        // Act
        Page<ProductResponse> response = productService.listProducts(categoryId.toString(), brandId.toString(), "ACTIVE", pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(productRepository, times(1)).findByCategoryIdAndBrandIdAndStatus(categoryId, brandId, "ACTIVE", pageable);
    }

    @Test
    void testListProductsPublicOnlyReturnsActiveProducts() {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findByStatus("ACTIVE", pageable))
                .thenReturn(productPage);
        when(productVariantRepository.findByProductId(productId)).thenReturn(new ArrayList<>());

        // Act
        Page<ProductResponse> response = productService.listProductsPublic(null, null, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(productRepository, times(1)).findByStatus("ACTIVE", pageable);
    }

    @Test
    void testCreateProductAutoGeneratesSlug() {
        // Arrange
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product Name")
                .slug(null)
                .description("Test Description")
                .categoryId(categoryId.toString())
                .brandId(brandId.toString())
                .status("DRAFT")
                .images(new ArrayList<>())
                .variants(List.of(variantDTO))
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(validBrand));
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(null);
        when(productVariantRepository.findByProductId(productId)).thenReturn(new ArrayList<>());

        // Act
        ProductResponse response = productService.createProduct(request);

        // Assert
        assertNotNull(response);
        verify(productRepository, times(1)).save(argThat(product ->
                product.getSlug() != null && !product.getSlug().isEmpty()
        ));
    }

    @Test
    void testCreateActiveProductWithoutImagesThrowsException() {
        // Arrange
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .categoryId(categoryId.toString())
                .brandId(brandId.toString())
                .status("ACTIVE")
                .images(new ArrayList<>())
                .variants(List.of(variantDTO))
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(validBrand));
        when(productRepository.existsBySlug(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            productService.createProduct(request);
        });
    }

    @Test
    void testCreateActiveProductWithoutVariantsThrowsException() {
        // Arrange
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .categoryId(categoryId.toString())
                .brandId(brandId.toString())
                .status("ACTIVE")
                .images(List.of("image.jpg"))
                .variants(new ArrayList<>())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(validBrand));
        when(productRepository.existsBySlug(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            productService.createProduct(request);
        });
    }

    @Test
    void testInvalidStatusThrowsException() {
        // Arrange
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .categoryId(categoryId.toString())
                .brandId(brandId.toString())
                .status("INVALID_STATUS")
                .images(List.of("image.jpg"))
                .variants(List.of(variantDTO))
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(validCategory));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(validBrand));
        when(productRepository.existsBySlug(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            productService.createProduct(request);
        });
    }
}
