package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.request.CreateProductRequest;
import com.omnichannel.product.dto.request.UpdateProductRequest;
import com.omnichannel.product.dto.response.ProductResponse;
import com.omnichannel.product.dto.response.ProductVariantResponse;
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
import com.omnichannel.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductEventPublisher eventPublisher;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private String generateSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase();
    }

    private UUID parseUUID(String id, String fieldName) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(fieldName + " must be a valid UUID format");
        }
    }

    private void validateCategoryExists(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        if (!category.getActive()) {
            throw new BadRequestException("Category with id '" + categoryId + "' is not active");
        }
    }

    private void validateBrandExists(UUID brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + brandId));
        if (!brand.getActive()) {
            throw new BadRequestException("Brand with id '" + brandId + "' is not active");
        }
    }

    private void validateProductDataForActivation(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        if (!StringUtils.hasText(product.getName())) {
            throw new BadRequestException("Product name is required to activate product");
        }
        if (!StringUtils.hasText(product.getDescription())) {
            throw new BadRequestException("Product description is required to activate product");
        }
        
        long variantCount = productVariantRepository.findByProductId(productId).size();
        if (variantCount == 0) {
            throw new BadRequestException("Product must have at least one variant to activate");
        }
        
        if (CollectionUtils.isEmpty(product.getImages())) {
            throw new BadRequestException("Product must have at least one image to activate");
        }
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());
        List<ProductVariantResponse> variantResponses = variants.stream()
                .map(v -> ProductVariantResponse.builder()
                        .id(v.getId().toString())
                        .productId(v.getProductId().toString())
                        .skuCode(v.getSkuCode())
                        .variantName(v.getVariantName())
                        .regularPrice(v.getRegularPrice())
                        .salePrice(v.getSalePrice())
                        .currency(v.getCurrency())
                        .attributes(v.getAttributes())
                        .active(v.getActive())
                        .createdAt(v.getCreatedAt())
                        .updatedAt(v.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        
        return ProductResponse.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .categoryId(product.getCategoryId().toString())
                .brandId(product.getBrandId().toString())
                .status(product.getStatus())
                .images(product.getImages())
                .variants(variantResponses)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        // Convert string IDs to UUIDs
        UUID categoryId = parseUUID(request.getCategoryId(), "categoryId");
        UUID brandId = parseUUID(request.getBrandId(), "brandId");

        // Generate or normalize slug
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getName());
        } else {
            slug = generateSlug(slug);
        }

        // Check slug uniqueness
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Product with slug '" + slug + "' already exists");
        }

        // Validate category exists and is active
        validateCategoryExists(categoryId);

        // Validate brand exists and is active
        validateBrandExists(brandId);

        // Validate status
        String status = request.getStatus() != null ? request.getStatus() : STATUS_DRAFT;
        if (!isValidStatus(status)) {
            throw new BadRequestException("Invalid product status. Must be one of: DRAFT, ACTIVE, INACTIVE, ARCHIVED");
        }

        // If status is ACTIVE, validate all required data
        if (STATUS_ACTIVE.equals(status)) {
            if (CollectionUtils.isEmpty(request.getImages())) {
                throw new BadRequestException("Product must have at least one image to be ACTIVE");
            }
            if (CollectionUtils.isEmpty(request.getVariants())) {
                throw new BadRequestException("Product must have at least one variant to be ACTIVE");
            }
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .categoryId(categoryId)
                .brandId(brandId)
                .status(status)
                .images(request.getImages())
                .build();

        Product savedProduct = productRepository.save(product);
        
        // Create and save variants
        if (!CollectionUtils.isEmpty(request.getVariants())) {
            request.getVariants().forEach(dto -> {
                ProductVariant variant = ProductVariant.builder()
                        .productId(savedProduct.getId())
                        .skuCode(dto.getSkuCode())
                        .variantName(dto.getVariantName())
                        .regularPrice(new BigDecimal(dto.getRegularPrice().toString()))
                        .salePrice(dto.getSalePrice() != null ? new BigDecimal(dto.getSalePrice().toString()) : null)
                        .currency(dto.getCurrency() != null ? dto.getCurrency() : "VND")
                        .attributes(dto.getAttributes())
                        .active(dto.getActive() != null ? dto.getActive() : true)
                        .build();
                productVariantRepository.save(variant);
            });
        }

        // Publish event to product search event outbox (PostgreSQL-native search)
        eventPublisher.publishProductCreated(savedProduct.getId());

        log.info("Product created with id: {}, slug: {}", savedProduct.getId(), savedProduct.getSlug());
        return mapToResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(String id) {
        UUID productId = parseUUID(id, "id");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByIdPublic(String id) {
        UUID productId = parseUUID(id, "id");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (!STATUS_ACTIVE.equals(product.getStatus())) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(String categoryId, String brandId, String status, Pageable pageable) {
        Page<Product> productPage;
        
        UUID catId = StringUtils.hasText(categoryId) ? parseUUID(categoryId, "categoryId") : null;
        UUID brnId = StringUtils.hasText(brandId) ? parseUUID(brandId, "brandId") : null;

        if (catId != null && brnId != null && StringUtils.hasText(status)) {
            productPage = productRepository.findByCategoryIdAndBrandIdAndStatus(catId, brnId, status, pageable);
        } else if (catId != null && brnId != null) {
            productPage = productRepository.findByCategoryIdAndBrandId(catId, brnId, pageable);
        } else if (catId != null && StringUtils.hasText(status)) {
            productPage = productRepository.findByCategoryIdAndStatus(catId, status, pageable);
        } else if (brnId != null && StringUtils.hasText(status)) {
            productPage = productRepository.findByBrandIdAndStatus(brnId, status, pageable);
        } else if (catId != null) {
            productPage = productRepository.findByCategoryId(catId, pageable);
        } else if (brnId != null) {
            productPage = productRepository.findByBrandId(brnId, pageable);
        } else if (StringUtils.hasText(status)) {
            productPage = productRepository.findByStatus(status, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        
        return productPage.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProductsPublic(String categoryId, String brandId, Pageable pageable) {
        Page<Product> productPage;
        
        UUID catId = StringUtils.hasText(categoryId) ? parseUUID(categoryId, "categoryId") : null;
        UUID brnId = StringUtils.hasText(brandId) ? parseUUID(brandId, "brandId") : null;

        if (catId != null && brnId != null) {
            productPage = productRepository.findByCategoryIdAndBrandIdAndStatus(catId, brnId, STATUS_ACTIVE, pageable);
        } else if (catId != null) {
            productPage = productRepository.findByCategoryIdAndStatus(catId, STATUS_ACTIVE, pageable);
        } else if (brnId != null) {
            productPage = productRepository.findByBrandIdAndStatus(brnId, STATUS_ACTIVE, pageable);
        } else {
            productPage = productRepository.findByStatus(STATUS_ACTIVE, pageable);
        }
        
        return productPage.map(this::mapToResponse);
    }

    @Override
    public ProductResponse updateProduct(String id, UpdateProductRequest request) {
        UUID productId = parseUUID(id, "id");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Check if new slug is provided and is different from current
        if (StringUtils.hasText(request.getSlug())) {
            String newSlug = generateSlug(request.getSlug());
            if (!newSlug.equals(product.getSlug()) && productRepository.existsBySlugAndIdNot(newSlug, productId)) {
                throw new DuplicateResourceException("Product with slug '" + newSlug + "' already exists");
            }
            product.setSlug(newSlug);
        }

        // Update fields if provided
        if (StringUtils.hasText(request.getName())) {
            product.setName(request.getName());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (StringUtils.hasText(request.getCategoryId())) {
            UUID newCategoryId = parseUUID(request.getCategoryId(), "categoryId");
            if (!newCategoryId.equals(product.getCategoryId())) {
                validateCategoryExists(newCategoryId);
            }
            product.setCategoryId(newCategoryId);
        }

        if (StringUtils.hasText(request.getBrandId())) {
            UUID newBrandId = parseUUID(request.getBrandId(), "brandId");
            if (!newBrandId.equals(product.getBrandId())) {
                validateBrandExists(newBrandId);
            }
            product.setBrandId(newBrandId);
        }

        if (StringUtils.hasText(request.getStatus())) {
            if (!isValidStatus(request.getStatus())) {
                throw new BadRequestException("Invalid product status. Must be one of: DRAFT, ACTIVE, INACTIVE, ARCHIVED");
            }
            
            // If changing to ACTIVE, validate all required data
            if (STATUS_ACTIVE.equals(request.getStatus()) && !STATUS_ACTIVE.equals(product.getStatus())) {
                validateProductDataForActivation(productId);
            }
            
            // Publish status change event
            String oldStatus = product.getStatus();
            product.setStatus(request.getStatus());
            if (!oldStatus.equals(request.getStatus())) {
                eventPublisher.publishProductStatusChanged(productId, oldStatus, request.getStatus());
            }
        }

        if (!CollectionUtils.isEmpty(request.getImages())) {
            product.setImages(request.getImages());
        }

        Product updatedProduct = productRepository.save(product);
        
        // Publish update event (only if fields other than status were updated)
        if (hasNonStatusChanges(request)) {
            eventPublisher.publishProductUpdated(updatedProduct.getId());
        }
        
        log.info("Product updated with id: {}", id);
        return mapToResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(String id) {
        UUID productId = parseUUID(id, "id");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Soft delete: change status to ARCHIVED
        String oldStatus = product.getStatus();
        product.setStatus(STATUS_ARCHIVED);
        productRepository.save(product);
        
        // Publish deletion event
        eventPublisher.publishProductDeleted(productId);
        eventPublisher.publishProductStatusChanged(productId, oldStatus, STATUS_ARCHIVED);
        
        log.info("Product soft deleted with id: {}", id);
    }

    private boolean isValidStatus(String status) {
        return STATUS_DRAFT.equals(status) || STATUS_ACTIVE.equals(status) 
                || STATUS_INACTIVE.equals(status) || STATUS_ARCHIVED.equals(status);
    }

    private boolean hasNonStatusChanges(UpdateProductRequest request) {
        return StringUtils.hasText(request.getName())
                || request.getDescription() != null
                || StringUtils.hasText(request.getSlug())
                || StringUtils.hasText(request.getCategoryId())
                || StringUtils.hasText(request.getBrandId())
                || !CollectionUtils.isEmpty(request.getImages());
    }
}
