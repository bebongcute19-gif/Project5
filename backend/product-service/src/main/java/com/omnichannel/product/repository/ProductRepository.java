package com.omnichannel.product.repository;

import com.omnichannel.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByBrandId(UUID brandId);

    Page<Product> findByStatus(String status, Pageable pageable);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(UUID categoryId, String status, Pageable pageable);

    Page<Product> findByBrandId(UUID brandId, Pageable pageable);

    Page<Product> findByBrandIdAndStatus(UUID brandId, String status, Pageable pageable);

    Page<Product> findByCategoryIdAndBrandId(UUID categoryId, UUID brandId, Pageable pageable);

    Page<Product> findByCategoryIdAndBrandIdAndStatus(UUID categoryId, UUID brandId, String status, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN ProductVariant pv ON p.id = pv.productId WHERE pv.skuCode = :skuCode")
    Optional<Product> findBySkuCode(@Param("skuCode") String skuCode);

    @Query("SELECT DISTINCT p FROM Product p JOIN ProductVariant pv ON p.id = pv.productId WHERE pv.skuCode IN :skuCodes")
    List<Product> findBySkuCodes(@Param("skuCodes") Collection<String> skuCodes);
}

