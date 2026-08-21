package ru.practicum.shareit.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.shareit.item.dto.CommentResponse;
import ru.practicum.shareit.item.dto.ItemResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemResponseJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializesBookingsAndComments() throws Exception {
        ItemResponse response = ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .requestId(7L)
                .lastBooking(ItemResponse.BookingSummary.builder()
                        .id(1L)
                        .bookerId(2L)
                        .start(LocalDateTime.of(2026, 1, 10, 9, 0, 45))
                        .end(LocalDateTime.of(2026, 1, 11, 9, 0, 45))
                        .build())
                .comments(List.of(CommentResponse.builder()
                        .id(1L)
                        .text("great")
                        .authorName("John")
                        .created(LocalDateTime.of(2026, 1, 9, 12, 0, 45))
                        .build()))
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"requestId\":7");
        assertThat(json).contains("\"lastBooking\":{\"id\":1");
        assertThat(json).contains("\"start\":\"2026-01-10T09:00:45\"");
        assertThat(json).contains("\"comments\":[{\"id\":1");
        assertThat(json).contains("\"authorName\":\"John\"");
    }
}