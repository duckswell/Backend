package com.likelion.duckswell.domain.dashboard.client.llm;

import java.util.List;

public record LlmChecklistResult(
        List<ChecklistItemDraft> items
) {
    public record ChecklistItemDraft(String title, String description) {
    }
}
