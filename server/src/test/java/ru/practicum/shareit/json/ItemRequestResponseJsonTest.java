package ru.practicum.shareit.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.shareit.request.dto.ItemRequestResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestResponseJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializesCreatedAndItems() throws Exception {
        ItemRequestResponse response = ItemRequestResponse.builder()
                .id(1L)
                .description("need drill")
                .created(LocalDateTime.of(2026, 1, 15, 10, 30, 45))
                .items(List.of(ItemRequestResponse.ItemSummary.builder()
                        .id(2L)
                        .name("Drill")
                        .ownerId(3L)
                        .build()))
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"description\":\"need drill\"");
        assertThat(json).contains("\"created\":\"2026-01-15T10:30:45\"");
        assertThat(json).contains("\"items\":[{\"id\":2");
        assertThat(json).contains("\"ownerId\":3");
    }
}