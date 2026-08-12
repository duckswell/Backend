package com.likelion.duckswell.domain.dashboard.dto;

import com.likelion.duckswell.domain.dashboard.entity.ChecklistItem;

public record ChecklistItemResponse(
        Long id,
        String title,
        String description,
        boolean checked
) {
    public static ChecklistItemResponse from(ChecklistItem checklistItem) {
        return new ChecklistItemResponse(
                checklistItem.getId(),
                checklistItem.getTitle(),
                checklistItem.getDescription(),
                checklistItem.isChecked()
        );
    }
}
