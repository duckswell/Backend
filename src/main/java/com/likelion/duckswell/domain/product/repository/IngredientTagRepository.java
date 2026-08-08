package com.likelion.duckswell.domain.product.repository;

import com.likelion.duckswell.domain.product.entity.IngredientTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientTagRepository extends JpaRepository<IngredientTag, Long> {

    List<IngredientTag> findByIngredientId(Long ingredientId);
}
