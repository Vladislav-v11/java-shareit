package ru.practicum.shareit.booking.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {
    private long itemId;
    private LocalDateTime start;
    private LocalDateTime end;
}
