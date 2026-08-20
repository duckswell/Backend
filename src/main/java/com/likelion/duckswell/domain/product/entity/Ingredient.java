package com.likelion.duckswell.domain.product.entity;

import com.likelion.duckswell.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ingredient")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IngredientCategory category;

    @Column(nullable = false, length = 500)
    private String description;

    public Ingredient(String name, IngredientCategory category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }
}
