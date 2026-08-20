package ru.practicum.shareit.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.CreateItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestResponse;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemRequestServiceImplIntegrationTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @PersistenceContext
    private EntityManager entityManager;

    private User requester;
    private User owner;

    @BeforeEach
    void setUp() {
        requester = saveUser("req", "req@test.ru");
        owner = saveUser("owner", "owner@test.ru");
    }

    @Test
    @DisplayName("Создание запроса на вещь")
    void create_savesRequestForExistingUser() {
        CreateItemRequest request = new CreateItemRequest();
        request.setDescription("Need a drill");

        ItemRequestResponse response = itemRequestService.create(request, requester.getId());

        assertThat(response.getId()).isPositive();
        assertThat(response.getDescription()).isEqualTo("Need a drill");
        assertThat(response.getCreated()).isNotNull();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Ошибка если пользователь не найден")
    void create_throwsWhenUserMissing() {
        CreateItemRequest request = new CreateItemRequest();
        request.setDescription("Need a drill");

        assertThatThrownBy(() -> itemRequestService.create(request, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Получение своих запросов с вещами и сортировкой")
    void getOwnRequests_returnsOwnRequestsNewestFirstWithItems() {
        ItemRequest older = saveRequest(requester, "older", LocalDateTime.now().minusHours(2));
        saveRequest(requester, "newer", LocalDateTime.now());
        saveItem(owner, older);

        List<ItemRequestResponse> responses = itemRequestService.getOwnRequests(requester.getId());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDescription()).isEqualTo("newer");
        assertThat(responses.get(1).getDescription()).isEqualTo("older");
        assertThat(responses.get(1).getItems()).hasSize(1);
        ItemRequestResponse.ItemSummary summary = responses.get(1).getItems().get(0);
        assertThat(summary.getName()).isEqualTo("Drill");
        assertThat(summary.getOwnerId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("Получение только чужих запросов")
    void getAllOtherRequests_returnsOnlyRequestsOfOthers() {
        saveRequest(requester, "mine", LocalDateTime.now().minusHours(1));
        saveRequest(owner, "older other", LocalDateTime.now().minusHours(3));
        saveRequest(owner, "newer other", LocalDateTime.now().minusHours(2));

        List<ItemRequestResponse> responses = itemRequestService.getAllOtherRequests(requester.getId());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDescription()).isEqualTo("newer other");
        assertThat(responses.get(1).getDescription()).isEqualTo("older other");
    }

    @Test
    @DisplayName("Получение запроса с вещами по ID")
    void getById_returnsRequestWithItems() {
        ItemRequest request = saveRequest(requester, "need drill", LocalDateTime.now());
        saveItem(owner, request);

        ItemRequestResponse response = itemRequestService.getById(request.getId());

        assertThat(response.getDescription()).isEqualTo("need drill");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isPositive();
    }

    @Test
    @DisplayName("Ошибка если запрос не найден")
    void getById_throwsWhenRequestMissing() {
        assertThatThrownBy(() -> itemRequestService.getById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    private User saveUser(String name, String email) {
        User user = User.builder().name(name).email(email).build();
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private ItemRequest saveRequest(User requestor, String description, LocalDateTime created) {
        ItemRequest request = ItemRequest.builder()
                .description(description)
                .requestor(requestor)
                .created(created)
                .build();
        entityManager.persist(request);
        entityManager.flush();
        return request;
    }

    private Item saveItem(User owner, ItemRequest request) {
        Item item = Item.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .owner(owner)
                .request(request)
                .build();
        entityManager.persist(item);
        entityManager.flush();
        return item;
    }
}