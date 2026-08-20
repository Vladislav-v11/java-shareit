package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    @DisplayName("Создание вещи: успешный ответ")
    void create_returnsItem() throws Exception {
        when(itemService.create(any(CreateItemRequest.class), eq(1L))).thenReturn(ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .requestId(5L)
                .build());

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateItemRequest.builder()
                                .name("Hammer")
                                .description("iron hammer")
                                .available(true)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Hammer"));
    }

    @Test
    @DisplayName("Обновление вещи: успешный ответ")
    void update_returnsUpdatedItem() throws Exception {
        when(itemService.update(eq(1L), any(UpdateItemRequest.class), eq(2L))).thenReturn(ItemResponse.builder()
                .id(1L)
                .name("Sledgehammer")
                .description("heavy")
                .available(false)
                .build());

        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateItemRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sledgehammer"));
    }

    @Test
    @DisplayName("Получение вещи по ID: успешный ответ")
    void getItem_returnsItem() throws Exception {
        when(itemService.findById(1L, 3L)).thenReturn(ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .build());

        mockMvc.perform(get("/items/1").header("X-Sharer-User-Id", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hammer"));
    }

    @Test
    @DisplayName("Получение списка вещей владельца: успешный ответ")
    void getAll_returnsListOfItems() throws Exception {
        when(itemService.findAllOwnerItems(4L)).thenReturn(List.of(ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .build()));

        mockMvc.perform(get("/items").header("X-Sharer-User-Id", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Hammer"));
    }

    @Test
    @DisplayName("Поиск вещей по тексту: успешный ответ")
    void search_returnsFoundItems() throws Exception {
        when(itemService.search("hammer")).thenReturn(List.of(ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .build()));

        mockMvc.perform(get("/items/search").param("text", "hammer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("Добавление комментария: успешный ответ")
    void addComment_returnsComment() throws Exception {
        when(itemService.addComment(eq(1L), any(CreateCommentRequest.class), eq(2L)))
                .thenReturn(CommentResponse.builder()
                        .id(1L)
                        .text("great")
                        .authorName("John")
                        .build());
        CreateCommentRequest request = new CreateCommentRequest();
        request.setText("great");

        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("great"));
    }

    @Test
    @DisplayName("Обновление чужой вещи: возврат 403")
    void update_returns403_whenNotOwner() throws Exception {
        when(itemService.update(eq(1L), any(UpdateItemRequest.class), eq(2L)))
                .thenThrow(new ForbiddenException("Only owner can edit"));

        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Получение вещи: возврат 404 при отсутствии")
    void getItem_returns404_whenItemMissing() throws Exception {
        when(itemService.findById(1L, 3L)).thenThrow(new NotFoundException("Item not found"));

        mockMvc.perform(get("/items/1").header("X-Sharer-User-Id", 3))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Создание вещи: возврат 404 при отсутствии запроса")
    void create_returns404_whenRequestMissing() throws Exception {
        when(itemService.create(any(CreateItemRequest.class), eq(1L)))
                .thenThrow(new NotFoundException("Item request not found"));

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Добавление комментария: возврат 400 если пользователь не бронировал")
    void addComment_returns400_whenUserNotBooked() throws Exception {
        when(itemService.addComment(eq(1L), any(CreateCommentRequest.class), eq(2L)))
                .thenThrow(new IllegalArgumentException("User has not booked this item"));

        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}