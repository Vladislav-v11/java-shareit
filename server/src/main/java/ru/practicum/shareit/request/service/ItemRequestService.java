package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.dto.CreateItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestResponse;

import java.util.List;

public interface ItemRequestService {

    ItemRequestResponse create(CreateItemRequest request, long userId);

    List<ItemRequestResponse> getOwnRequests(long userId);

    List<ItemRequestResponse> getAllOtherRequests(long userId);

    ItemRequestResponse getById(long requestId);
}