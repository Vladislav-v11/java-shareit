package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.dto.CreateItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestResponse;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestService itemRequestService;

    @Test
    @DisplayName("Создание запроса: успешный ответ")
    void create_returnsRequest() throws Exception {
        when(itemRequestService.create(any(CreateItemRequest.class), eq(1L))).thenReturn(ItemRequestResponse.builder()
                .id(1L)
                .description("need drill")
                .created(LocalDateTime.now())
                .items(List.of())
                .build());

        CreateItemRequest request = new CreateItemRequest();
        request.setDescription("need drill");

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("need drill"));
    }

    @Test
    @DisplayName("Получение своих запросов: успешный ответ")
    void getOwn_returnsListOfRequests() throws Exception {
        when(itemRequestService.getOwnRequests(1L)).thenReturn(List.of(requestResponse()));

        mockMvc.perform(get("/requests").header("X-Sharer-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("need drill"));
    }

    @Test
    @DisplayName("Получение чужих запросов: успешный ответ")
    void getAllOther_returnsListOfRequests() throws Exception {
        when(itemRequestService.getAllOtherRequests(1L)).thenReturn(List.of(requestResponse()));

        mockMvc.perform(get("/requests/all").header("X-Sharer-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("Получение запроса по ID: успешный ответ")
    void getById_returnsRequest() throws Exception {
        when(itemRequestService.getById(2L)).thenReturn(ItemRequestResponse.builder()
                .id(2L)
                .description("need drill")
                .created(LocalDateTime.now())
                .items(List.of(ItemRequestResponse.ItemSummary.builder()
                        .id(10L)
                        .name("Drill")
                        .ownerId(3L)
                        .build()))
                .build());

        mockMvc.perform(get("/requests/2").header("X-Sharer-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Drill"));
    }

    @Test
    @DisplayName("Получение запроса: возврат 404 при отсутствии")
    void getById_returns404_whenRequestMissing() throws Exception {
        when(itemRequestService.getById(2L)).thenThrow(new NotFoundException("Request not found"));

        mockMvc.perform(get("/requests/2").header("X-Sharer-User-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    private ItemRequestResponse requestResponse() {
        return ItemRequestResponse.builder()
                .id(1L)
                .description("need drill")
                .created(LocalDateTime.now())
                .items(List.of())
                .build();
    }
}
