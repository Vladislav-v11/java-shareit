package ru.practicum.shareit.item.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ItemResponse {
    private long id;
    private String name;
    private String description;
    private Boolean available;
    private BookingSummary lastBooking;
    private BookingSummary nextBooking;
    private List<CommentResponse> comments;

    @Data
    @Builder
    public static class BookingSummary {
        private long id;
        private long bookerId;
        private LocalDateTime start;
        private LocalDateTime end;
    }
}
