package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.response.ProductResponse;
import com.omnichannel.product.dto.response.ProductVariantResponse;
import com.omnichannel.product.entity.Brand;
import com.omnichannel.product.entity.Category;
import com.omnichannel.product.entity.Product;
import com.omnichannel.product.entity.ProductVariant;
import com.omnichannel.product.entity.search.ProductSearchDocument;
import com.omnichannel.product.exception.ResourceNotFoundException;
import com.omnichannel.product.repository.BrandRepository;
import com.omnichannel.product.repository.CategoryRepository;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.repository.ProductVariantRepository;
import com.omnichannel.product.repository.search.ProductSearchRepository;
import com.omnichannel.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductSearchRepository searchRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_BATCH_SIZE = 100;

    @Override
    public Page<ProductResponse> searchProducts(
            String keyword,
            String categoryId,
            String brandId,
            Double minPrice,
            Double maxPrice,
            Pageable pageable) {

        try {
            // First, search in Elasticsearch for ACTIVE products only
            Page<ProductSearchDocument> esResults = performElasticsearchSearch(
                    keyword, categoryId, brandId, minPrice, maxPrice, pageable);

            // Convert ES documents back to ProductResponse using PostgreSQL as source of truth
            List<ProductResponse> productResponses = esResults.getContent().stream()
                    .map(doc -> {
                        try {
                            UUID productId = UUID.fromString(doc.getProductId());
                            return productRepository.findById(productId)
                                    .filter(p -> STATUS_ACTIVE.equals(p.getStatus()))
                                    .map(this::mapToResponse)
                                    .orElse(null);
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid product ID in search result: {}", doc.getProductId());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new PageImpl<>(productResponses, pageable, esResults.getTotalElements());
        } catch (Exception e) {
            log.error("Error searching products in Elasticsearch, falling back to PostgreSQL", e);
            return fallbackPostgresSearch(keyword, categoryId, brandId, minPrice, maxPrice, pageable);
        }
    }

    private Page<ProductSearchDocument> performElasticsearchSearch(
            String keyword,
            String categoryId,
            String brandId,
            Double minPrice,
            Double maxPrice,
            Pageable pageable) {

        // Simple approach: fetch and filter ACTIVE products
        // For production, use ElasticsearchRepository with custom queries via QueryBuilders
        if (StringUtils.hasText(categoryId) && StringUtils.hasText(brandId)) {
            return searchRepository.findByCategoryIdAndBrandIdAndStatus(categoryId, brandId, STATUS_ACTIVE, pageable);
        } else if (StringUtils.hasText(categoryId)) {
            return searchRepository.findByCategoryIdAndStatus(categoryId, STATUS_ACTIVE, pageable);
        } else if (StringUtils.hasText(brandId)) {
            return searchRepository.findByBrandIdAndStatus(brandId, STATUS_ACTIVE, pageable);
        } else {
            return searchRepository.findByStatus(STATUS_ACTIVE, pageable);
        }
    }

    private Page<ProductResponse> fallbackPostgresSearch(
            String keyword,
            String categoryId,
            String brandId,
            Double minPrice,
            Double maxPrice,
            Pageable pageable) {

        // Fallback to PostgreSQL full-text search if Elasticsearch is unavailable
        UUID catId = StringUtils.hasText(categoryId) ? UUID.fromString(categoryId) : null;
        UUID brnId = StringUtils.hasText(brandId) ? UUID.fromString(brandId) : null;

        Page<Product> productPage;
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
    @Transactional
    public void indexProduct(String productId) {
        try {
            UUID id = UUID.fromString(productId);
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

            ProductSearchDocument doc = buildSearchDocument(product);
            searchRepository.save(doc);
            log.info("Product indexed successfully: {}", productId);
        } catch (Exception e) {
            log.error("Error indexing product: {}", productId, e);
            throw new RuntimeException("Failed to index product", e);
        }
    }

    @Override
    @Transactional
    public void removeProductFromIndex(String productId) {
        try {
            searchRepository.deleteByProductId(productId);
            log.info("Product removed from index: {}", productId);
        } catch (Exception e) {
            log.error("Error removing product from index: {}", productId, e);
        }
    }

    @Override
    @Transactional
    public void reindexAllProducts() {
        reindexAllProductsWithBatching(DEFAULT_BATCH_SIZE);
    }

    @Override
    @Transactional
    public void reindexAllProductsWithBatching(int batchSize) {
        try {
            log.info("Starting full reindex of products with batch size: {}", batchSize);
            
            // Delete all existing documents first
            searchRepository.deleteAll();
            log.info("Cleared existing index");

            // Fetch and index products in batches
            int page = 0;
            boolean hasMore = true;
            
            while (hasMore) {
                List<Product> batch = productRepository.findAll()
                        .stream()
                        .filter(p -> STATUS_ACTIVE.equals(p.getStatus()))
                        .skip((long) page * batchSize)
                        .limit(batchSize)
                        .collect(Collectors.toList());

                if (batch.isEmpty()) {
                    hasMore = false;
                } else {
                    List<ProductSearchDocument> docs = batch.stream()
                            .map(this::buildSearchDocument)
                            .collect(Collectors.toList());
                    
                    searchRepository.saveAll(docs);
                    log.info("Indexed batch: {} products (page: {})", batch.size(), page);
                    page++;
                }
            }

            log.info("Reindex completed successfully");
        } catch (Exception e) {
            log.error("Error during reindex", e);
            throw new RuntimeException("Failed to reindex products", e);
        }
    }

    private ProductSearchDocument buildSearchDocument(Product product) {
        List<ProductVariant> variants = variantRepository.findByProductId(product.getId());
        
        Double minPrice = null;
        Double maxPrice = null;
        
        if (!variants.isEmpty()) {
            List<BigDecimal> prices = new ArrayList<>();
            for (ProductVariant v : variants) {
                if (v.getSalePrice() != null && v.getSalePrice().compareTo(BigDecimal.ZERO) > 0) {
                    prices.add(v.getSalePrice());
                } else {
                    prices.add(v.getRegularPrice());
                }
            }
            if (!prices.isEmpty()) {
                minPrice = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO).doubleValue();
                maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO).doubleValue();
            }
        }

        String categoryName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName)
                .orElse("");

        String brandName = brandRepository.findById(product.getBrandId())
                .map(Brand::getName)
                .orElse("");

        return ProductSearchDocument.builder()
                .id(product.getId().toString())
                .productId(product.getId().toString())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .categoryId(product.getCategoryId().toString())
                .categoryName(categoryName)
                .brandId(product.getBrandId().toString())
                .brandName(brandName)
                .status(product.getStatus())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductVariant> variants = variantRepository.findByProductId(product.getId());
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
}
