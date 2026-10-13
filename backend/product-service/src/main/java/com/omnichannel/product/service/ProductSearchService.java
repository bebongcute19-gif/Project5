package com.omnichannel.product.service;

import com.omnichannel.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductSearchService {

    /**
     * Search products with full-text search and filters
     *
     * @param keyword Search keyword for name and description
     * @param categoryId Filter by category
     * @param brandId Filter by brand
     * @param minPrice Minimum price (from variant)
     * @param maxPrice Maximum price (from variant)
     * @param pageable Pagination and sorting
     * @return Page of products matching criteria (ACTIVE only for public)
     */
    Page<ProductResponse> searchProducts(
            String keyword,
            String categoryId,
            String brandId,
            Double minPrice,
            Double maxPrice,
            Pageable pageable);

    /**
     * Search products with full-text search, filters, and custom status
     */
    Page<ProductResponse> searchProducts(
            String keyword,
            String categoryId,
            String brandId,
            Double minPrice,
            Double maxPrice,
            String status,
            Pageable pageable);

    /**
     * Index a product document for search
     *
     * @param productId Product ID to index
     */
    void indexProduct(String productId);

    /**
     * Remove a product from search index
     *
     * @param productId Product ID to remove
     */
    void removeProductFromIndex(String productId);

    /**
     * Reindex all active products (admin only)
     */
    void reindexAllProducts();

    /**
     * Reindex all active products with batch processing
     *
     * @param batchSize Number of products to process per batch
     */
    void reindexAllProductsWithBatching(int batchSize);
}
