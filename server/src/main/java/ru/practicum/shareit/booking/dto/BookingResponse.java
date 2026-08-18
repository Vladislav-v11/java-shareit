package ru.practicum.shareit.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private long id;
    private LocalDateTime start;
    private LocalDateTime end;
    private ItemInfo item;
    private BookerInfo booker;
    private BookingStatus status;

    @Data
    @Builder
    public static class ItemInfo {
        private long id;
        private String name;
    }

    @Data
    @Builder
    public static class BookerInfo {
        private long id;
        private String name;
    }
}
