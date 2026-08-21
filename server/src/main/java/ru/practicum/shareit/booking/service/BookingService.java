package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;

import java.util.List;

public interface BookingService {
    BookingResponse create(CreateBookingRequest request, long userId);

    BookingResponse approve(long bookingId, boolean approved, long userId);

    BookingResponse findById(long bookingId, long userId);

    List<BookingResponse> findAllByBooker(String state, long userId);

    List<BookingResponse> findAllByOwner(String state, long userId);
}