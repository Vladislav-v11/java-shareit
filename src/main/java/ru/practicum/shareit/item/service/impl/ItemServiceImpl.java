package ru.practicum.shareit.item.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemResponse;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.item.storage.ItemStorage;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemStorage itemStorage;
    private final UserStorage userStorage;

    @Override
    public ItemResponse create(CreateItemRequest request, long userId) {
        User owner = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Item item = ItemMapper.toEntity(request);
        item.setOwner(owner);
        Item created = itemStorage.create(item);

        return ItemMapper.toResponse(created);
    }

    @Override
    public ItemResponse update(long itemId, UpdateItemRequest request, long userId) {
        Item item = itemStorage.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (item.getOwner().getId() != userId) {
            throw new ForbiddenException("Only owner can edit");
        }
        ItemMapper.toEntity(request, item);
        itemStorage.update(item);
        return ItemMapper.toResponse(item);
    }

    @Override
    public ItemResponse findById(long id) {
        return  itemStorage.findById(id)
                .map(ItemMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Item not found"));
    }

    @Override
    public List<ItemResponse> findAllOwnerItems(long ownerId) {
        return itemStorage.findAllOwnerItems(ownerId).stream()
                .map(ItemMapper::toResponse)
                .toList();
    }

    @Override
    public List<ItemResponse> search(String text) {
        return itemStorage.search(text).stream()
                .map(ItemMapper::toResponse)
                .toList();
    }
}
