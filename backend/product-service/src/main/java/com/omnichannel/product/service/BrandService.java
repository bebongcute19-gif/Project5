package com.omnichannel.product.service;

import com.omnichannel.product.dto.request.BrandRequest;
import com.omnichannel.product.dto.response.BrandResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandService {
    BrandResponse createBrand(BrandRequest request);
    BrandResponse getBrandById(String id);
    BrandResponse getBrandBySlug(String slug);
    Page<BrandResponse> listBrands(Boolean active, Pageable pageable);
    BrandResponse updateBrand(String id, BrandRequest request);
    void deleteBrand(String id);
}
