package com.omnichannel.product.repository;

import com.omnichannel.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findBySkuCode(String skuCode);

    boolean existsBySkuCode(String skuCode);

    boolean existsBySkuCodeAndIdNot(String skuCode, UUID id);

    List<ProductVariant> findByProductId(UUID productId);

    List<ProductVariant> findByProductIdAndActive(UUID productId, Boolean active);

    long deleteByProductId(UUID productId);
}
