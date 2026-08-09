package com.likelion.duckswell.domain.product.dto;

import com.likelion.duckswell.domain.product.entity.Product;
import com.likelion.duckswell.domain.product.entity.ProductCategory;

public record ProductResponse(
        Long id,
        String name,
        String brand,
        ProductCategory category,
        String imageUrl,
        String linkUrl
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getBrand(), product.getCategory(), product.getImageUrl(), product.getLinkUrl());
    }
}
