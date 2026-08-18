package ru.practicum.shareit.request.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ItemRequestResponse {
    private long id;
    private String description;
    private LocalDateTime created;
    private List<ItemSummary> items;

    @Data
    @Builder
    public static class ItemSummary {
        private long id;
        private String name;
        private long ownerId;
    }
}
