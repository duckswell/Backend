package com.likelion.duckswell.domain.product.repository;

import com.likelion.duckswell.domain.product.entity.Product;
import com.likelion.duckswell.domain.product.entity.ProductCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByIngredientIdOrderByCreatedAtDesc(Long ingredientId);

    List<Product> findByCategory(ProductCategory category);
}
