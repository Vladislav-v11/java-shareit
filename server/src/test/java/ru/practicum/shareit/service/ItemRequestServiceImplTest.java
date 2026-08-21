package ru.practicum.shareit.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.dto.CreateItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestResponse;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.service.impl.ItemRequestServiceImpl;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl itemRequestService;

    private User requester;
    private User owner;

    @BeforeEach
    void setUp() {
        requester = User.builder().id(1L).name("req").email("req@test.ru").build();
        owner = User.builder().id(2L).name("owner").email("owner@test.ru").build();
    }

    @Test
    @DisplayName("Создание запроса: успешное сохранение для существующего пользователя")
    void create_savesRequestForExistingUser() {
        ItemRequest saved = request(1L, "Need a drill", LocalDateTime.now());
        CreateItemRequest createRequest = new CreateItemRequest();
        createRequest.setDescription("Need a drill");
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(saved);

        ItemRequestResponse response = itemRequestService.create(createRequest, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDescription()).isEqualTo("Need a drill");
        assertThat(response.getItems()).isEmpty();
        verify(itemRequestRepository).save(any(ItemRequest.class));
    }

    @Test
    @DisplayName("Создание запроса: ошибка, если пользователь не найден")
    void create_throwsWhenUserMissing() {
        CreateItemRequest createRequest = new CreateItemRequest();
        createRequest.setDescription("Need a drill");
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemRequestService.create(createRequest, 9L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Получение своих запросов: пустой список, если запросов нет")
    void getOwnRequests_returnsEmptyWhenNone() {
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(1L)).thenReturn(List.of());

        List<ItemRequestResponse> responses = itemRequestService.getOwnRequests(1L);

        assertThat(responses).isEmpty();
        verify(itemRepository, never()).findByRequestIdIn(anyList());
    }

    @Test
    @DisplayName("Получение своих запросов: успешный возврат списка с вещами")
    void getOwnRequests_returnsRequestsWithItems() {
        ItemRequest older = request(2L, "older", LocalDateTime.now().minusHours(2));
        ItemRequest newer = request(3L, "newer", LocalDateTime.now());
        Item item = itemFor(older);
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(1L)).thenReturn(List.of(newer, older));
        when(itemRepository.findByRequestIdIn(anyList())).thenReturn(List.of(item));

        List<ItemRequestResponse> responses = itemRequestService.getOwnRequests(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDescription()).isEqualTo("newer");
        assertThat(responses.get(1).getDescription()).isEqualTo("older");
        assertThat(responses.get(1).getItems()).hasSize(1);
        assertThat(responses.get(1).getItems().get(0).getName()).isEqualTo("Drill");
        assertThat(responses.get(1).getItems().get(0).getOwnerId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("Получение чужих запросов: успешный возврат списка")
    void getAllOtherRequests_returnsOtherRequests() {
        ItemRequest other = request(2L, "other request", LocalDateTime.now());
        when(itemRequestRepository.findAllByRequestorIdNotOrderByCreatedDesc(1L)).thenReturn(List.of(other));

        List<ItemRequestResponse> responses = itemRequestService.getAllOtherRequests(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getDescription()).isEqualTo("other request");
    }

    @Test
    @DisplayName("Получение запроса по ID: успешный возврат с вещами")
    void getById_returnsRequestWithItems() {
        ItemRequest request = request(1L, "need drill", LocalDateTime.now());
        Item item = itemFor(request);
        when(itemRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findByRequestIdIn(List.of(1L))).thenReturn(List.of(item));

        ItemRequestResponse response = itemRequestService.getById(1L);

        assertThat(response.getDescription()).isEqualTo("need drill");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isEqualTo(item.getId());
    }

    @Test
    @DisplayName("Получение запроса по ID: ошибка, если запрос не найден")
    void getById_throwsWhenRequestMissing() {
        when(itemRequestRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemRequestService.getById(9L))
                .isInstanceOf(NotFoundException.class);
    }

    private ItemRequest request(long id, String description, LocalDateTime created) {
        return ItemRequest.builder()
                .id(id)
                .description(description)
                .requestor(requester)
                .created(created)
                .build();
    }

    private Item itemFor(ItemRequest request) {
        return Item.builder()
                .id(10L)
                .name("Drill")
                .description("power drill")
                .available(true)
                .owner(owner)
                .request(request)
                .build();
    }
}