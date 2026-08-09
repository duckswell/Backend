package com.likelion.duckswell.domain.product.service;

import com.likelion.duckswell.domain.product.dto.ProductResponse;
import com.likelion.duckswell.domain.product.entity.Product;
import com.likelion.duckswell.domain.product.entity.ProductCategory;
import com.likelion.duckswell.domain.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getProductsByIngredient(Long ingredientId, ProductCategory productCategory) {
        List<Product> products = productCategory == null
                ? productRepository.findByIngredientIdOrderByCreatedAtDesc(ingredientId)
                : productRepository.findByIngredientIdAndCategoryOrderByCreatedAtDesc(ingredientId, productCategory);
        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }
}
