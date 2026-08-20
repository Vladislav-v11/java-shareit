package ru.practicum.shareit.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BookingServiceImplIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @PersistenceContext
    private EntityManager entityManager;

    private User owner;
    private User booker;
    private Item availableItem;

    @BeforeEach
    void setUp() {
        owner = saveUser("owner", "owner@test.ru");
        booker = saveUser("booker", "booker@test.ru");
        availableItem = saveItem(owner, true);
    }

    @Test
    @DisplayName("Создание бронирования со статусом WAITING")
    void create_savesWaitingBooking() {
        LocalDateTime now = LocalDateTime.now();

        BookingResponse response = bookingService.create(
                bookingRequest(availableItem.getId(), now.plusHours(1), now.plusHours(2)), booker.getId());

        assertThat(response.getId()).isPositive();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(response.getItem().getId()).isEqualTo(availableItem.getId());
        assertThat(response.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    @DisplayName("Ошибка при создании на недоступную вещь")
    void create_throwsWhenItemUnavailable() {
        Item unavailable = saveItem(owner, false);

        assertThatThrownBy(() -> bookingService.create(bookingRequest(unavailable.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ошибка при бронировании своей вещи")
    void create_throwsWhenBookingOwnItem() {
        assertThatThrownBy(() -> bookingService.create(bookingRequest(availableItem.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), owner.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ошибка если дата конца раньше даты начала")
    void create_throwsWhenEndBeforeStart() {
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> bookingService.create(
                bookingRequest(availableItem.getId(), now.plusHours(2), now.plusHours(1)), booker.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bookingService.create(
                bookingRequest(availableItem.getId(), now.plusHours(1), now.plusHours(1)), booker.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ошибка при пересечении интервалов бронирований")
    void create_throwsWhenOverlappingBookingExists() {
        LocalDateTime now = LocalDateTime.now();
        BookingResponse existing = bookingService.create(
                bookingRequest(availableItem.getId(), now.plusHours(1), now.plusHours(3)), booker.getId());
        bookingService.approve(existing.getId(), true, owner.getId());

        assertThatThrownBy(() -> bookingService.create(
                bookingRequest(availableItem.getId(), now.plusHours(2), now.plusHours(4)), booker.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Владелец подтверждает бронирование")
    void approve_byOwnerChangesStatus() {
        BookingResponse created = bookingService.create(bookingRequest(availableItem.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        BookingResponse approved = bookingService.approve(created.getId(), true, owner.getId());

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    @DisplayName("Владелец отклоняет бронирование")
    void approve_byOwnerRejectsBooking() {
        BookingResponse created = bookingService.create(bookingRequest(availableItem.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        BookingResponse rejected = bookingService.approve(created.getId(), false, owner.getId());

        assertThat(rejected.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    @DisplayName("Ошибка подтверждения не владельцем")
    void approve_throwsWhenNotOwner() {
        BookingResponse created = bookingService.create(bookingRequest(availableItem.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        assertThatThrownBy(() -> bookingService.approve(created.getId(), true, booker.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Ошибка повторного подтверждения завершенного бронирования")
    void approve_throwsWhenNotWaiting() {
        BookingResponse created = bookingService.create(bookingRequest(availableItem.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());
        bookingService.approve(created.getId(), true, owner.getId());

        assertThatThrownBy(() -> bookingService.approve(created.getId(), true, owner.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ошибка доступа к чужому бронированию")
    void findById_throwsWhenAccessDenied() {
        User stranger = saveUser("stranger", "stranger@test.ru");
        BookingResponse created = bookingService.create(bookingRequest(availableItem.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        assertThatThrownBy(() -> bookingService.findById(created.getId(), stranger.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> bookingService.findById(999L, booker.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Владелец и арендатор видят своё бронирование")
    void findById_returnsForOwnerAndBooker() {
        BookingResponse created = bookingService.create(bookingRequest(availableItem.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        BookingResponse byOwner = bookingService.findById(created.getId(), owner.getId());
        BookingResponse byBooker = bookingService.findById(created.getId(), booker.getId());

        assertThat(byOwner.getId()).isEqualTo(created.getId());
        assertThat(byBooker.getId()).isEqualTo(created.getId());
    }

    @Test
    @DisplayName("Фильтрация бронирований арендатора по статусам")
    void findAllByBooker_filtersByState() {
        LocalDateTime now = LocalDateTime.now();
        bookingService.create(bookingRequest(availableItem.getId(), now.minusHours(3), now.minusHours(2)), booker.getId());
        bookingService.create(bookingRequest(availableItem.getId(), now.plusHours(1), now.plusHours(2)), booker.getId());

        List<BookingResponse> all = bookingService.findAllByBooker("ALL", booker.getId());
        List<BookingResponse> past = bookingService.findAllByBooker("PAST", booker.getId());
        List<BookingResponse> future = bookingService.findAllByBooker("FUTURE", booker.getId());

        assertThat(all).hasSize(2);
        assertThat(past).hasSize(1);
        assertThat(future).hasSize(1);
    }

    @Test
    @DisplayName("Фильтрация CURRENT, WAITING, REJECTED для арендатора")
    void findAllByBooker_filtersCurrentWaitingRejected() {
        LocalDateTime now = LocalDateTime.now();

        BookingResponse current = bookingService.create(
                bookingRequest(availableItem.getId(), now.minusHours(2), now.plusHours(2)), booker.getId());
        bookingService.approve(current.getId(), true, owner.getId());
        BookingResponse waiting = bookingService.create(
                bookingRequest(availableItem.getId(), now.plusDays(1), now.plusDays(2)), booker.getId());
        BookingResponse rejected = bookingService.create(
                bookingRequest(availableItem.getId(), now.plusDays(3), now.plusDays(4)), booker.getId());
        bookingService.approve(rejected.getId(), false, owner.getId());

        List<BookingResponse> currentBookings = bookingService.findAllByBooker("CURRENT", booker.getId());
        List<BookingResponse> waitingBookings = bookingService.findAllByBooker("WAITING", booker.getId());
        List<BookingResponse> rejectedBookings = bookingService.findAllByBooker("REJECTED", booker.getId());

        assertThat(currentBookings).extracting(BookingResponse::getId).containsExactly(current.getId());
        assertThat(waitingBookings).extracting(BookingResponse::getId).containsExactly(waiting.getId());
        assertThat(rejectedBookings).extracting(BookingResponse::getId).containsExactly(rejected.getId());
    }

    @Test
    @DisplayName("Получение всех бронирований владельца")
    void findAllByOwner_returnsOwnerItemsBookings() {
        bookingService.create(bookingRequest(availableItem.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        List<BookingResponse> responses = bookingService.findAllByOwner("ALL", owner.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getItem().getId()).isEqualTo(availableItem.getId());
    }

    @Test
    @DisplayName("Ошибка если арендатор не найден")
    void findAllByBooker_throwsWhenUserMissing() {
        assertThatThrownBy(() -> bookingService.findAllByBooker("ALL", 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Ошибка если владелец не найден")
    void findAllByOwner_throwsWhenUserMissing() {
        assertThatThrownBy(() -> bookingService.findAllByOwner("ALL", 999L))
                .isInstanceOf(NotFoundException.class);
    }

    private User saveUser(String name, String email) {
        User user = User.builder().name(name).email(email).build();
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Item saveItem(User owner, boolean available) {
        Item item = Item.builder()
                .name("Hammer")
                .description("iron hammer")
                .available(available)
                .owner(owner)
                .build();
        entityManager.persist(item);
        entityManager.flush();
        return item;
    }

    private CreateBookingRequest bookingRequest(long itemId, LocalDateTime start, LocalDateTime end) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setItemId(itemId);
        request.setStart(start);
        request.setEnd(end);
        return request;
    }
}