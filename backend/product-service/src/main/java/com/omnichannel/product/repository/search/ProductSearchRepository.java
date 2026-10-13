package com.omnichannel.product.repository.search;

import com.omnichannel.product.entity.search.ProductSearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String> {

    Optional<ProductSearchDocument> findByProductId(String productId);

    Page<ProductSearchDocument> findByName(String name, Pageable pageable);

    Page<ProductSearchDocument> findByCategoryId(String categoryId, Pageable pageable);

    Page<ProductSearchDocument> findByBrandId(String brandId, Pageable pageable);

    Page<ProductSearchDocument> findByStatus(String status, Pageable pageable);

    // Multiple filters
    Page<ProductSearchDocument> findByCategoryIdAndStatus(String categoryId, String status, Pageable pageable);

    Page<ProductSearchDocument> findByBrandIdAndStatus(String brandId, String status, Pageable pageable);

    Page<ProductSearchDocument> findByCategoryIdAndBrandIdAndStatus(String categoryId, String brandId, String status, Pageable pageable);

    long deleteByProductId(String productId);
}

