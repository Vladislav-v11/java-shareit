package ru.practicum.shareit.booking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    public BookingResponse create(CreateBookingRequest request, long userId) {
        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (Boolean.FALSE.equals(item.getAvailable())) {
            throw new IllegalArgumentException("Item is not available");
        }
        if (request.getEnd().isBefore(request.getStart()) || request.getEnd().isEqual(request.getStart())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        if (item.getOwner().getId() == userId) {
            throw new IllegalArgumentException("Owner cannot book own item");
        }
        boolean isOverlapping = bookingRepository.existsOverlappingBooking(
                request.getItemId(), request.getStart(), request.getEnd());
        if (isOverlapping) {
            throw new IllegalArgumentException("Item is already booked for this time period");
        }

        Booking booking = BookingMapper.toEntity(request, item, booker);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: id={}, userId={}, itemId={}", saved.getId(), userId, request.getItemId());
        return BookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse approve(long bookingId, boolean approved, long userId) {
        Booking booking = bookingRepository.findByIdAndOwnerId(bookingId, userId)
                .orElse(null);

        if (booking == null) {
            bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new NotFoundException("Booking not found"));
            throw new ForbiddenException("Only owner can approve/reject booking");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new IllegalArgumentException("Booking is not in WAITING status");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking {}: id={}, userId={}", approved ? "approved" : "rejected", bookingId, userId);
        return BookingMapper.toResponse(saved);
    }

    @Override
    public BookingResponse findById(long bookingId, long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getBooker().getId() != userId && booking.getItem().getOwner().getId() != userId) {
            throw new ForbiddenException("Access denied");
        }
        return BookingMapper.toResponse(booking);
    }

    @Override
    public List<BookingResponse> findAllByBooker(String state, long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<Booking> all = bookingRepository.findByBookerIdOrderByStartDesc(userId);
        List<Booking> filtered = filterByState(all, state);

        return filtered.stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    @Override
    public List<BookingResponse> findAllByOwner(String state, long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<Booking> all = bookingRepository.findByItemOwnerIdOrderByStartDesc(userId);
        List<Booking> filtered = filterByState(all, state);

        return filtered.stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    private List<Booking> filterByState(List<Booking> bookings, String state) {
        LocalDateTime now = LocalDateTime.now();
        return switch (BookingState.valueOf(state)) {
            case ALL -> bookings;
            case CURRENT -> bookings.stream()
                    .filter(b -> b.getStart().isBefore(now) && b.getEnd().isAfter(now))
                    .toList();
            case PAST -> bookings.stream()
                    .filter(b -> b.getEnd().isBefore(now))
                    .toList();
            case FUTURE -> bookings.stream()
                    .filter(b -> b.getStart().isAfter(now))
                    .toList();
            case WAITING -> bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.WAITING)
                    .toList();
            case REJECTED -> bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.REJECTED)
                    .toList();
        };
    }
}
