package com.omnichannel.product.service;

import com.omnichannel.product.dto.request.CategoryRequest;
import com.omnichannel.product.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse getCategoryById(String id);
    CategoryResponse getCategoryBySlug(String slug);
    Page<CategoryResponse> listCategories(Boolean active, Pageable pageable);
    CategoryResponse updateCategory(String id, CategoryRequest request);
    void deleteCategory(String id);
}
