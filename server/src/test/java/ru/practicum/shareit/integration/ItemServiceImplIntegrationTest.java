package ru.practicum.shareit.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemServiceImplIntegrationTest {

    @Autowired
    private ItemService itemService;

    @PersistenceContext
    private EntityManager entityManager;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = saveUser("owner", "owner@test.ru");
        booker = saveUser("booker", "booker@test.ru");
        item = saveItem("Hammer", "iron hammer", true, owner);
    }

    @Test
    @DisplayName("Получение вещей владельца с бронированиями и комментариями")
    void findAllOwnerItems_returnsItemsWithBookingsAndComments() {
        LocalDateTime now = LocalDateTime.now();
        saveBooking(item, booker, now.minusDays(2), now.minusDays(1), BookingStatus.APPROVED);
        saveBooking(item, booker, now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED);
        saveComment(item, booker, "good tool", now.minusDays(1));

        List<ItemResponse> responses = itemService.findAllOwnerItems(owner.getId());

        assertThat(responses).hasSize(1);
        ItemResponse response = responses.get(0);
        assertThat(response.getLastBooking()).isNotNull();
        assertThat(response.getLastBooking().getBookerId()).isEqualTo(booker.getId());
        assertThat(response.getNextBooking()).isNotNull();
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().get(0).getText()).isEqualTo("good tool");
    }

    @Test
    @DisplayName("Пустой список если у владельца нет вещей")
    void findAllOwnerItems_returnsEmptyWhenNoItems() {
        User emptyOwner = saveUser("empty", "empty@test.ru");

        List<ItemResponse> responses = itemService.findAllOwnerItems(emptyOwner.getId());

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("Создание вещи с привязкой к запросу")
    void create_withRequestId_bindsRequest() {
        User requester = saveUser("req", "req@test.ru");
        ItemRequest request = saveRequest(requester, "need drill", LocalDateTime.now());

        ItemResponse response = itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .requestId(request.getId())
                .build(), owner.getId());

        assertThat(response.getRequestId()).isEqualTo(request.getId());
        Item saved = entityManager.find(Item.class, response.getId());
        assertThat(saved.getRequest().getId()).isEqualTo(request.getId());
    }

    @Test
    @DisplayName("Создание обычной вещи без запроса")
    void create_withoutRequestId_savesPlainItem() {
        ItemResponse response = itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .build(), owner.getId());

        assertThat(response.getRequestId()).isNull();
    }

    @Test
    @DisplayName("Ошибка при несуществующем ID запроса")
    void create_withUnknownRequestId_throws() {
        assertThatThrownBy(() -> itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .requestId(999L)
                .build(), owner.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Ошибка если пользователь не найден")
    void create_throwsWhenUserMissing() {
        assertThatThrownBy(() -> itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .build(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Обновление только переданных полей вещи")
    void update_changesOnlyProvidedFields() {
        UpdateItemRequest update = new UpdateItemRequest();
        update.setName("Sledgehammer");
        update.setAvailable(false);

        ItemResponse response = itemService.update(item.getId(), update, owner.getId());

        assertThat(response.getName()).isEqualTo("Sledgehammer");
        assertThat(response.getAvailable()).isFalse();
        assertThat(response.getDescription()).isEqualTo("iron hammer");
        Item saved = entityManager.find(Item.class, item.getId());
        assertThat(saved.getName()).isEqualTo("Sledgehammer");
        assertThat(saved.getAvailable()).isFalse();
        assertThat(saved.getDescription()).isEqualTo("iron hammer");
    }

    @Test
    @DisplayName("Ошибка при обновлении чужой вещи")
    void update_throwsWhenNotOwner() {
        User stranger = saveUser("stranger", "stranger@test.ru");

        UpdateItemRequest update = new UpdateItemRequest();
        update.setName("Sledgehammer");

        assertThatThrownBy(() -> itemService.update(item.getId(), update, stranger.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Ошибка если вещь не найдена")
    void update_throwsWhenItemMissing() {
        UpdateItemRequest update = new UpdateItemRequest();
        update.setName("Ghost");

        assertThatThrownBy(() -> itemService.update(999L, update, owner.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Владелец видит бронирования и комментарии в вещи")
    void findById_returnsBookingsAndCommentsForOwner() {
        LocalDateTime now = LocalDateTime.now();
        saveBooking(item, booker, now.minusDays(2), now.minusDays(1), BookingStatus.APPROVED);
        saveBooking(item, booker, now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED);
        saveComment(item, booker, "good tool", now.minusDays(1));

        ItemResponse response = itemService.findById(item.getId(), owner.getId());

        assertThat(response.getLastBooking()).isNotNull();
        assertThat(response.getNextBooking()).isNotNull();
        assertThat(response.getComments()).hasSize(1);
    }

    @Test
    @DisplayName("Другой пользователь не видит бронирования")
    void findById_returnsNoBookingsForOtherUser() {
        User stranger = saveUser("stranger", "stranger@test.ru");

        ItemResponse response = itemService.findById(item.getId(), stranger.getId());

        assertThat(response.getLastBooking()).isNull();
        assertThat(response.getNextBooking()).isNull();
        assertThat(response.getComments()).isEmpty();
    }

    @Test
    @DisplayName("Ошибка если вещь не найдена")
    void findById_throwsWhenItemMissing() {
        assertThatThrownBy(() -> itemService.findById(999L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Поиск доступных вещей по тексту")
    void search_returnsOnlyAvailableMatchingItems() {
        saveItem("Drill", "power drill", true, owner);
        saveItem("BrokenDrill", "drill out of order", false, owner);

        List<ItemResponse> byName = itemService.search("drill");
        List<ItemResponse> byDescription = itemService.search("hammer");
        List<ItemResponse> noMatch = itemService.search("nonexistent");

        assertThat(byName).hasSize(1);
        assertThat(byName.get(0).getName()).isEqualTo("Drill");
        assertThat(byDescription).hasSize(1);
        assertThat(noMatch).isEmpty();
    }

    @Test
    @DisplayName("Добавление комментария после завершенного бронирования")
    void addComment_allowedForFinishedBooking() {
        LocalDateTime now = LocalDateTime.now();
        saveBooking(item, booker, now.minusDays(2), now.minusDays(1), BookingStatus.APPROVED);

        CreateCommentRequest comment = new CreateCommentRequest();
        comment.setText("great");
        CommentResponse response = itemService.addComment(item.getId(), comment, booker.getId());

        assertThat(response.getText()).isEqualTo("great");
        assertThat(response.getAuthorName()).isEqualTo(booker.getName());
    }

    @Test
    @DisplayName("Ошибка добавления комментария без завершенного бронирования")
    void addComment_throwsWhenNoFinishedBooking() {
        CreateCommentRequest comment = new CreateCommentRequest();
        comment.setText("great");

        assertThatThrownBy(() -> itemService.addComment(item.getId(), comment, booker.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private User saveUser(String name, String email) {
        User user = User.builder().name(name).email(email).build();
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Item saveItem(String name, String description, boolean available, User owner) {
        Item item = Item.builder()
                .name(name)
                .description(description)
                .available(available)
                .owner(owner)
                .build();
        entityManager.persist(item);
        entityManager.flush();
        return item;
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

    private Booking saveBooking(Item item, User booker, LocalDateTime start, LocalDateTime end,
                                BookingStatus status) {
        Booking booking = Booking.builder()
                .start(start)
                .end(end)
                .item(item)
                .booker(booker)
                .status(status)
                .build();
        entityManager.persist(booking);
        entityManager.flush();
        return booking;
    }

    private Comment saveComment(Item item, User author, String text, LocalDateTime created) {
        Comment comment = Comment.builder()
                .text(text)
                .item(item)
                .author(author)
                .created(created)
                .build();
        entityManager.persist(comment);
        entityManager.flush();
        return comment;
    }
}
