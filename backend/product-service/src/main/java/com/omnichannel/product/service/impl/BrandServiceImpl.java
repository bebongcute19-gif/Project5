package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.request.BrandRequest;
import com.omnichannel.product.dto.response.BrandResponse;
import com.omnichannel.product.entity.Brand;
import com.omnichannel.product.exception.BadRequestException;
import com.omnichannel.product.exception.DuplicateResourceException;
import com.omnichannel.product.exception.ResourceNotFoundException;
import com.omnichannel.product.repository.BrandRepository;
import com.omnichannel.product.repository.ProductRepository;
import com.omnichannel.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

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

    private BrandResponse mapToResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId().toString())
                .name(brand.getName())
                .slug(brand.getSlug())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .active(brand.getActive())
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("Brand name is required");
        }
        String slug = StringUtils.hasText(request.getSlug()) ? request.getSlug() : generateSlug(request.getName());
        if (brandRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Brand with slug '" + slug + "' already exists");
        }
        Brand brand = Brand.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        Brand saved = brandRepository.save(brand);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(String id) {
        UUID brandId = parseUUID(id, "id");
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        return mapToResponse(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with slug: " + slug));
        return mapToResponse(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrandResponse> listBrands(Boolean active, Pageable pageable) {
        // Public API only returns active brands unless caller explicitly sets active flag
        Boolean filterActive = (active != null) ? active : true;
        Page<Brand> page = brandRepository.findByActive(filterActive, pageable);
        List<BrandResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(String id, BrandRequest request) {
        UUID brandId = parseUUID(id, "id");
        Brand existing = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        if (StringUtils.hasText(request.getName())) {
            existing.setName(request.getName());
        }
        if (StringUtils.hasText(request.getDescription())) {
            existing.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getLogoUrl())) {
            existing.setLogoUrl(request.getLogoUrl());
        }
        if (request.getActive() != null) {
            existing.setActive(request.getActive());
        }
        // Slug handling
        String newSlug = StringUtils.hasText(request.getSlug()) ? request.getSlug() : generateSlug(existing.getName());
        if (!newSlug.equals(existing.getSlug()) && brandRepository.existsBySlugAndIdNot(newSlug, brandId)) {
            throw new DuplicateResourceException("Brand with slug '" + newSlug + "' already exists");
        }
        existing.setSlug(newSlug);
        Brand saved = brandRepository.save(existing);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteBrand(String id) {
        UUID brandId = parseUUID(id, "id");
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        // Prevent deletion if any product references this brand
        if (productRepository.existsByBrandId(brandId)) {
            throw new BadRequestException("Cannot delete brand; it is referenced by existing products");
        }
        brandRepository.delete(brand);
    }
}
