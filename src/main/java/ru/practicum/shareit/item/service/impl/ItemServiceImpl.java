package ru.practicum.shareit.item.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.item.storage.CommentRepository;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    public ItemResponse create(CreateItemRequest request, long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Item item = ItemMapper.toEntity(request);
        item.setOwner(owner);
        Item created = itemRepository.save(item);

        return ItemMapper.toResponse(created);
    }

    @Override
    public ItemResponse update(long itemId, UpdateItemRequest request, long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (item.getOwner().getId() != userId) {
            throw new ForbiddenException("Only owner can edit");
        }
        ItemMapper.toEntity(request, item);
        itemRepository.save(item);
        return ItemMapper.toResponse(item);
    }

    @Override
    public ItemResponse findById(long id, long userId) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        List<CommentResponse> comments = commentRepository.findByItemId(id).stream()
                .map(ItemMapper::toCommentResponse)
                .toList();

        Booking last = null;
        Booking next = null;
        if (item.getOwner().getId() == userId) {
            List<Booking> allApproved = bookingRepository.findAllApprovedForItems(List.of(id));
            last = findLastBooking(allApproved);
            next = findNextBooking(allApproved);
        }

        return ItemMapper.toResponse(item, last, next, comments);
    }

    @Override
    public List<ItemResponse> findAllOwnerItems(long ownerId) {
        List<Item> items = itemRepository.findByOwnerId(ownerId);
        if (items.isEmpty()) return List.of();

        List<Long> itemIds = items.stream().map(Item::getId).toList();

        List<Booking> allApproved = bookingRepository.findAllApprovedForItems(itemIds);

        Map<Long, Booking> lastBookings = new HashMap<>();
        Map<Long, Booking> nextBookings = new HashMap<>();

        Map<Long, List<Booking>> byItemId = allApproved.stream()
                .collect(Collectors.groupingBy(b -> b.getItem().getId()));

        byItemId.forEach((itemId, bookings) -> {
            Booking last = findLastBooking(bookings);
            Booking next = findNextBooking(bookings);
            if (last != null) lastBookings.put(itemId, last);
            if (next != null) nextBookings.put(itemId, next);
        });

        Map<Long, List<CommentResponse>> commentsMap = commentRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(
                        c -> c.getItem().getId(),
                        Collectors.mapping(ItemMapper::toCommentResponse, Collectors.toList())
                ));

        return items.stream()
                .map(item -> ItemMapper.toResponse(item,
                        lastBookings.get(item.getId()),
                        nextBookings.get(item.getId()),
                        commentsMap.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    @Override
    public List<ItemResponse> search(String text) {
        return itemRepository.search(text).stream()
                .map(ItemMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse addComment(long itemId, CreateCommentRequest request, long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean hasBooked = bookingRepository
                .existsByItemIdAndBookerIdAndApprovedAndEndBefore(itemId, userId);
        if (!hasBooked) {
            throw new IllegalArgumentException("User has not booked this item");
        }

        Comment comment = ItemMapper.toEntity(request, item, author);

        Comment saved = commentRepository.save(comment);
        return ItemMapper.toCommentResponse(saved);
    }

    private Booking findLastBooking(List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();
        return bookings.stream()
                .filter(b -> !b.getStart().isAfter(now))
                .findFirst()
                .orElse(null);
    }

    private Booking findNextBooking(List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();
        return bookings.stream()
                .filter(b -> b.getStart().isAfter(now))
                .reduce((first, second) -> second)
                .orElse(null);
    }
}
