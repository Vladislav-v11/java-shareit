package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.*;

import java.util.List;

public interface ItemService {

    ItemResponse create(CreateItemRequest request, long userId);

    ItemResponse update(long itemId, UpdateItemRequest request, long userId);

    ItemResponse findById(long id, long userId);

    List<ItemResponse> findAllOwnerItems(long ownerId);

    List<ItemResponse> search(String text);

    CommentResponse addComment(long itemId, CreateCommentRequest request, long userId);
}

