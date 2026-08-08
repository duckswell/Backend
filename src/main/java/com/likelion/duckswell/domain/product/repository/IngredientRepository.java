package com.likelion.duckswell.domain.product.repository;

import com.likelion.duckswell.domain.product.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
