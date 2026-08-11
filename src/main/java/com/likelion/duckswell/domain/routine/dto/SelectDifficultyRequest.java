package com.likelion.duckswell.domain.routine.dto;

import com.likelion.duckswell.domain.routine.entity.RoutineDifficulty;
import jakarta.validation.constraints.NotNull;

public record SelectDifficultyRequest(@NotNull RoutineDifficulty difficulty) {
}
