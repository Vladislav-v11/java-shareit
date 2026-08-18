package ru.practicum.shareit.request.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.dto.CreateItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestResponse;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestResponse create(CreateItemRequest request, long userId) {
        User requestor = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        ItemRequest saved = itemRequestRepository.save(ItemRequestMapper.toEntity(request, requestor));
        return ItemRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemRequestResponse> getOwnRequests(long userId) {
        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(userId);
        if (requests.isEmpty()) {
            return List.of();
        }
        List<Long> requestIds = requests.stream().map(ItemRequest::getId).toList();
        Map<Long, List<Item>> itemsByRequestId = itemRepository.findByRequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId()));
        return requests.stream()
                .map(request -> ItemRequestMapper.toResponse(request,
                        itemsByRequestId.getOrDefault(request.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemRequestResponse> getAllOtherRequests(long userId) {
        return itemRequestRepository.findAllByRequestorIdNotOrderByCreatedDesc(userId).stream()
                .map(ItemRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ItemRequestResponse getById(long requestId) {
        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request not found"));
        List<Item> items = itemRepository.findByRequestIdIn(List.of(requestId));
        return ItemRequestMapper.toResponse(request, items);
    }
}
