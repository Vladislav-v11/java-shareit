package ru.practicum.shareit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.impl.BookingServiceImpl;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("owner").email("owner@test.ru").build();
        booker = User.builder().id(2L).name("booker").email("booker@test.ru").build();
        item = Item.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .owner(owner)
                .build();
        now = LocalDateTime.now();
    }

    @Test
    @DisplayName("Создание бронирования: успешное сохранение со статусом WAITING")
    void create_savesWaitingBooking() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        Booking saved = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingResponse response = bookingService.create(createRequest(1L, now.plusHours(1), now.plusHours(2)), 2L);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(response.getBooker().getId()).isEqualTo(2L);
        assertThat(response.getItem().getId()).isEqualTo(1L);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Создание бронирования: ошибка, если пользователь не найден")
    void create_throwsWhenUserMissing() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(createRequest(1L, now.plusHours(1), now.plusHours(2)), 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Создание бронирования: ошибка, если вещь не найдена")
    void create_throwsWhenItemMissing() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(createRequest(1L, now.plusHours(1), now.plusHours(2)), 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Создание бронирования: ошибка, если вещь недоступна")
    void create_throwsWhenItemUnavailable() {
        Item unavailable = Item.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(false)
                .owner(owner)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(unavailable));

        assertThatThrownBy(() -> bookingService.create(createRequest(1L, now.plusHours(1), now.plusHours(2)), 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Создание бронирования: ошибка, если дата конца раньше начала")
    void create_throwsWhenEndBeforeStart() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(createRequest(1L, now.plusHours(2), now.plusHours(1)), 2L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bookingService.create(createRequest(1L, now.plusHours(1), now.plusHours(1)), 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Создание бронирования: ошибка при попытке забронировать свою вещь")
    void create_throwsWhenBookingOwnItem() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(createRequest(1L, now.plusHours(1), now.plusHours(2)), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Создание бронирования: ошибка при пересечении бронирований")
    void create_throwsWhenOverlappingBookingExists() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsOverlappingBooking(1L, now.plusHours(1), now.plusHours(2))).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(createRequest(1L, now.plusHours(1), now.plusHours(2)), 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Подтверждение бронирования: успешное подтверждение WAITING бронирования")
    void approve_approvesWaitingBooking() {
        Booking booking = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        when(bookingRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.approve(1L, true, 1L);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.APPROVED);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Подтверждение бронирования: успешное отклонение WAITING бронирования")
    void approve_rejectsWaitingBooking() {
        Booking booking = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        when(bookingRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.approve(1L, false, 1L);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    @DisplayName("Подтверждение бронирования: ошибка, если пользователь не владелец")
    void approve_throwsWhenNotOwner() {
        when(bookingRepository.findByIdAndOwnerId(1L, 2L)).thenReturn(Optional.empty());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(1L, now.plusHours(1), now.plusHours(2),
                BookingStatus.WAITING)));

        assertThatThrownBy(() -> bookingService.approve(1L, true, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Подтверждение бронирования: ошибка, если бронирование не найдено")
    void approve_throwsWhenBookingMissing() {
        when(bookingRepository.findByIdAndOwnerId(9L, 1L)).thenReturn(Optional.empty());
        when(bookingRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.approve(9L, true, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Подтверждение бронирования: ошибка, если бронирование уже обработано")
    void approve_throwsWhenAlreadyProcessed() {
        Booking booking = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.APPROVED);
        when(bookingRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approve(1L, true, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Получение бронирования по ID: успешный возврат для арендатора")
    void findById_returnsForBooker() {
        Booking booking = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.findById(1L, 2L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Получение бронирования: ошибка доступа для постороннего пользователя")
    void findById_throwsWhenAccessDenied() {
        User stranger = User.builder().id(3L).name("stranger").email("stranger@test.ru").build();
        Booking booking = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.findById(1L, stranger.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Получение бронирования: ошибка, если бронирование не найдено")
    void findById_throwsWhenMissing() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findById(9L, 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Получение бронирований арендатора: ошибка, если пользователь не найден")
    void findAllByBooker_throwsWhenUserMissing() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findAllByBooker("ALL", 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Получение всех бронирований арендатора: успешный возврат")
    void findAllByBooker_returnsAll() {
        Booking past = booking(1L, now.minusHours(3), now.minusHours(2), BookingStatus.APPROVED);
        Booking future = booking(2L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdOrderByStartDesc(2L)).thenReturn(List.of(future, past));

        List<BookingResponse> responses = bookingService.findAllByBooker("ALL", 2L);

        assertThat(responses).extracting(BookingResponse::getId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("Получение бронирований арендатора: фильтрация CURRENT")
    void findAllByBooker_filtersCurrent() {
        Booking current = booking(1L, now.minusHours(1), now.plusHours(1), BookingStatus.APPROVED);
        Booking past = booking(2L, now.minusHours(3), now.minusHours(2), BookingStatus.APPROVED);
        Booking future = booking(3L, now.plusHours(1), now.plusHours(2), BookingStatus.APPROVED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdOrderByStartDesc(2L)).thenReturn(List.of(future, current, past));

        List<BookingResponse> responses = bookingService.findAllByBooker("CURRENT", 2L);

        assertThat(responses).extracting(BookingResponse::getId).containsExactly(current.getId());
    }

    @Test
    @DisplayName("Получение бронирований арендатора: фильтрация PAST")
    void findAllByBooker_filtersPast() {
        Booking current = booking(1L, now.minusHours(1), now.plusHours(1), BookingStatus.APPROVED);
        Booking past = booking(2L, now.minusHours(3), now.minusHours(2), BookingStatus.APPROVED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdOrderByStartDesc(2L)).thenReturn(List.of(current, past));

        List<BookingResponse> responses = bookingService.findAllByBooker("PAST", 2L);

        assertThat(responses).extracting(BookingResponse::getId).containsExactly(past.getId());
    }

    @Test
    @DisplayName("Получение бронирований арендатора: фильтрация FUTURE")
    void findAllByBooker_filtersFuture() {
        Booking past = booking(1L, now.minusHours(3), now.minusHours(2), BookingStatus.APPROVED);
        Booking future = booking(2L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdOrderByStartDesc(2L)).thenReturn(List.of(future, past));

        List<BookingResponse> responses = bookingService.findAllByBooker("FUTURE", 2L);

        assertThat(responses).extracting(BookingResponse::getId).containsExactly(future.getId());
    }

    @Test
    @DisplayName("Получение бронирований арендатора: фильтрация WAITING")
    void findAllByBooker_filtersWaiting() {
        Booking waiting = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        Booking approved = booking(2L, now.plusDays(1), now.plusDays(2), BookingStatus.APPROVED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdOrderByStartDesc(2L)).thenReturn(List.of(approved, waiting));

        List<BookingResponse> responses = bookingService.findAllByBooker("WAITING", 2L);

        assertThat(responses).extracting(BookingResponse::getId).containsExactly(waiting.getId());
    }

    @Test
    @DisplayName("Получение бронирований арендатора: фильтрация REJECTED")
    void findAllByBooker_filtersRejected() {
        Booking rejected = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.REJECTED);
        Booking waiting = booking(2L, now.plusDays(1), now.plusDays(2), BookingStatus.WAITING);
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdOrderByStartDesc(2L)).thenReturn(List.of(waiting, rejected));

        List<BookingResponse> responses = bookingService.findAllByBooker("REJECTED", 2L);

        assertThat(responses).extracting(BookingResponse::getId).containsExactly(rejected.getId());
    }

    @Test
    @DisplayName("Получение бронирований владельца: успешный возврат")
    void findAllByOwner_returnsOwnerBookings() {
        Booking booking = booking(1L, now.plusHours(1), now.plusHours(2), BookingStatus.WAITING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByItemOwnerIdOrderByStartDesc(1L)).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.findAllByOwner("ALL", 1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Получение бронирований владельца: ошибка, если пользователь не найден")
    void findAllByOwner_throwsWhenUserMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findAllByOwner("ALL", 1L))
                .isInstanceOf(NotFoundException.class);
    }

    private Booking booking(long id, LocalDateTime start, LocalDateTime end, BookingStatus status) {
        return Booking.builder()
                .id(id)
                .start(start)
                .end(end)
                .item(item)
                .booker(booker)
                .status(status)
                .build();
    }

    private CreateBookingRequest createRequest(long itemId, LocalDateTime start, LocalDateTime end) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setItemId(itemId);
        request.setStart(start);
        request.setEnd(end);
        return request;
    }
}