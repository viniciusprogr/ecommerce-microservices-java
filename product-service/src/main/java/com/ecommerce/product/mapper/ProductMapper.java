package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.CategoryResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import org.springframework.stereotype.Component;

/**
 * Conversoes entre entidades do catalogo e DTOs.
 *
 * <p>A categoria e recebida ja carregada porque o mapper nao deve acessar o banco:
 * buscar dados e responsabilidade do service.</p>
 */
@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request, Category category) {
        return Product.builder()
                .sku(request.sku())
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .category(category)
                .build();
    }

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                category.getId(),
                category.getName(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
