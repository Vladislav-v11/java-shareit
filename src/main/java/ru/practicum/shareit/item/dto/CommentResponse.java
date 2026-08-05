package ru.practicum.shareit.item.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {
    private final long id;
    private final String text;
    private String authorName;
    private final LocalDateTime created;
}