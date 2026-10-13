package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.response.ProductResponse;
import com.omnichannel.product.dto.response.ProductVariantResponse;
import com.omnichannel.product.entity.Product;
import com.omnichannel.product.entity.ProductVariant;
import com.omnichannel.product.exception.ResourceNotFoundException;
import com.omnichannel.product.repository.BrandRepository;
import com.omnichannel.product.repository.CategoryRepository;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.repository.ProductVariantRepository;
import com.omnichannel.product.service.ProductSearchService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    private static final String STATUS_ACTIVE = "ACTIVE";

    @Override
    public Page<ProductResponse> searchProducts(
            String keyword,
            String categoryId,
            String brandId,
            Double minPrice,
            Double maxPrice,
            Pageable pageable) {
        return searchProducts(keyword, categoryId, brandId, minPrice, maxPrice, STATUS_ACTIVE, pageable);
    }

    @Override
    public Page<ProductResponse> searchProducts(
            String keyword,
            String categoryId,
            String brandId,
            Double minPrice,
            Double maxPrice,
            String status,
            Pageable pageable) {

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filter by product status (defaults to ACTIVE for public catalog)
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }

            // 2. Full-text keyword search across name, description, and slug
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate slugMatch = cb.like(cb.lower(root.get("slug")), pattern);
                predicates.add(cb.or(nameMatch, descMatch, slugMatch));
            }

            // 3. Category filter
            if (StringUtils.hasText(categoryId)) {
                try {
                    UUID catUuid = UUID.fromString(categoryId.trim());
                    predicates.add(cb.equal(root.get("categoryId"), catUuid));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid categoryId format: {}", categoryId);
                }
            }

            // 4. Brand filter
            if (StringUtils.hasText(brandId)) {
                try {
                    UUID brandUuid = UUID.fromString(brandId.trim());
                    predicates.add(cb.equal(root.get("brandId"), brandUuid));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid brandId format: {}", brandId);
                }
            }

            // 5. Price range filter across active product variants
            if (minPrice != null || maxPrice != null) {
                Subquery<UUID> variantSubquery = query.subquery(UUID.class);
                Root<ProductVariant> variantRoot = variantSubquery.from(ProductVariant.class);
                variantSubquery.select(variantRoot.get("productId"));

                List<Predicate> variantPredicates = new ArrayList<>();
                variantPredicates.add(cb.isTrue(variantRoot.get("active")));

                // Effective price: salePrice if present and > 0, otherwise regularPrice
                Expression<BigDecimal> effectivePrice = cb.<BigDecimal>selectCase()
                        .when(cb.and(cb.isNotNull(variantRoot.get("salePrice")),
                                     cb.greaterThan(variantRoot.get("salePrice"), BigDecimal.ZERO)),
                              variantRoot.get("salePrice"))
                        .otherwise(variantRoot.get("regularPrice"));

                if (minPrice != null) {
                    variantPredicates.add(cb.greaterThanOrEqualTo(effectivePrice, BigDecimal.valueOf(minPrice)));
                }
                if (maxPrice != null) {
                    variantPredicates.add(cb.lessThanOrEqualTo(effectivePrice, BigDecimal.valueOf(maxPrice)));
                }

                variantSubquery.where(variantPredicates.toArray(new Predicate[0]));
                predicates.add(root.get("id").in(variantSubquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        if (productPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Batch fetch variants to avoid N+1 queries
        List<UUID> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        Map<UUID, List<ProductVariant>> variantsByProductId = variantRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        List<ProductResponse> responses = productPage.getContent().stream()
                .map(product -> mapToResponse(product, variantsByProductId.getOrDefault(product.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, productPage.getTotalElements());
    }

    @Override
    @Transactional
    public void indexProduct(String productId) {
        UUID id = UUID.fromString(productId);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        log.info("Product verified in PostgreSQL search index: id={}, status={}", product.getId(), product.getStatus());
    }

    @Override
    @Transactional
    public void removeProductFromIndex(String productId) {
        log.info("Product search removal managed natively in PostgreSQL: productId={}", productId);
    }

    @Override
    @Transactional
    public void reindexAllProducts() {
        long activeCount = productRepository.count((root, query, cb) -> cb.equal(root.get("status"), STATUS_ACTIVE));
        log.info("PostgreSQL native search active; verified {} active products in database.", activeCount);
    }

    @Override
    @Transactional
    public void reindexAllProductsWithBatching(int batchSize) {
        long activeCount = productRepository.count((root, query, cb) -> cb.equal(root.get("status"), STATUS_ACTIVE));
        log.info("PostgreSQL native search batch verification complete (batchSize={}). Total active products: {}", batchSize, activeCount);
    }

    private ProductResponse mapToResponse(Product product, List<ProductVariant> variants) {
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
