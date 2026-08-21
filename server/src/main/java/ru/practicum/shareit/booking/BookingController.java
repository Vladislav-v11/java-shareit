package ru.practicum.shareit.booking;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;
import ru.practicum.shareit.booking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@Validated
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public BookingResponse create(@RequestBody CreateBookingRequest request,
                                  @RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        BookingResponse created = bookingService.create(request, userId);
        log.info("Создано бронирование: id={}", created.getId());
        return created;
    }

    @PatchMapping("/{bookingId}")
    public BookingResponse approve(@PathVariable long bookingId,
                                   @RequestParam boolean approved,
                                   @RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        BookingResponse response = bookingService.approve(bookingId, approved, userId);
        log.info("Бронирование {}: id={}", approved ? "подтверждено" : "отклонено", bookingId);
        return response;
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@PathVariable long bookingId,
                                      @RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        log.debug("Получение бронирования: id={}", bookingId);
        return bookingService.findById(bookingId, userId);
    }

    @GetMapping
    public List<BookingResponse> getAll(@RequestParam(defaultValue = "ALL") String state,
                                        @RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        log.debug("Получение бронирований пользователя: userId={}, state={}", userId, state);
        return bookingService.findAllByBooker(state, userId);
    }

    @GetMapping("/owner")
    public List<BookingResponse> getAllByOwner(@RequestParam(defaultValue = "ALL") String state,
                                               @RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        log.debug("Получение бронирований владельца: userId={}, state={}", userId, state);
        return bookingService.findAllByOwner(state, userId);
    }
}