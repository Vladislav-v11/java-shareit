package ru.practicum.shareit.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingResponseJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializesDatesInIsoFormat() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(1L)
                .start(LocalDateTime.of(2026, 1, 15, 10, 30, 45))
                .end(LocalDateTime.of(2026, 1, 16, 12, 0, 5))
                .item(BookingResponse.ItemInfo.builder().id(2L).name("Hammer").build())
                .booker(BookingResponse.BookerInfo.builder().id(3L).name("John").build())
                .status(BookingStatus.WAITING)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"start\":\"2026-01-15T10:30:45\"");
        assertThat(json).contains("\"end\":\"2026-01-16T12:00:05\"");
        assertThat(json).contains("\"status\":\"WAITING\"");
        assertThat(json).contains("\"name\":\"Hammer\"");
    }
}