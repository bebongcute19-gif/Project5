package com.omnichannel.product.service.impl;

import com.omnichannel.product.dto.request.CategoryRequest;
import com.omnichannel.product.dto.response.CategoryResponse;
import com.omnichannel.product.entity.Category;
import com.omnichannel.product.exception.BadRequestException;
import com.omnichannel.product.exception.DuplicateResourceException;
import com.omnichannel.product.exception.ResourceNotFoundException;
import com.omnichannel.product.repository.CategoryRepository;
import com.omnichannel.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private UUID parseUUID(String id, String fieldName) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(fieldName + " must be a valid UUID format");
        }
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getName());
        } else {
            slug = generateSlug(slug);
        }

        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category with slug '" + slug + "' already exists");
        }

        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            UUID parentId = parseUUID(request.getParentId(), "parentId");
            if (!categoryRepository.existsById(parentId)) {
                throw new ResourceNotFoundException("Parent category not found with id: " + request.getParentId());
            }
        }

        UUID parentId = request.getParentId() != null && !request.getParentId().isBlank() 
            ? parseUUID(request.getParentId(), "parentId") 
            : null;

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .parentId(parentId)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Override
    public CategoryResponse getCategoryById(String id) {
        UUID categoryId = parseUUID(id, "id");
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    @Override
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return mapToResponse(category);
    }

    @Override
    public Page<CategoryResponse> listCategories(Boolean active, Pageable pageable) {
        Page<Category> categoryPage;
        if (active != null) {
            categoryPage = categoryRepository.findByActive(active, pageable);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }
        return categoryPage.map(this::mapToResponse);
    }

    @Override
    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        UUID categoryId = parseUUID(id, "id");
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String targetSlug = request.getSlug();
        if (targetSlug == null || targetSlug.isBlank()) {
            targetSlug = generateSlug(request.getName());
        } else {
            targetSlug = generateSlug(targetSlug);
        }

        if (categoryRepository.existsBySlugAndIdNot(targetSlug, categoryId)) {
            throw new DuplicateResourceException("Category with slug '" + targetSlug + "' already exists");
        }

        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            UUID newParentId = parseUUID(request.getParentId(), "parentId");
            if (newParentId.equals(categoryId)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            if (!categoryRepository.existsById(newParentId)) {
                throw new ResourceNotFoundException("Parent category not found with id: " + request.getParentId());
            }
            category.setParentId(newParentId);
        } else {
            category.setParentId(null);
        }

        category.setName(request.getName());
        category.setSlug(targetSlug);
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(String id) {
        UUID categoryId = parseUUID(id, "id");
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }

        List<Category> children = categoryRepository.findByParentId(categoryId);
        if (!children.isEmpty()) {
            throw new BadRequestException("Cannot delete category that has subcategories");
        }

        categoryRepository.deleteById(categoryId);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId().toString())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParentId() != null ? category.getParentId().toString() : null)
                .active(category.getActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private String generateSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
