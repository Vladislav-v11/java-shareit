package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemResponse;
import ru.practicum.shareit.item.dto.UpdateItemRequest;

import java.util.List;

public interface ItemService {

    ItemResponse create(CreateItemRequest request, long userId);

    ItemResponse update(long itemId, UpdateItemRequest request, long userId);

    ItemResponse findById(long id);

    List<ItemResponse> findAllOwnerItems(long ownerId);

    List<ItemResponse> search(String text);
}
