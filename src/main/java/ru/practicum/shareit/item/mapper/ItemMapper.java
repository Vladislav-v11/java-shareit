package ru.practicum.shareit.item.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemResponse;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.model.Item;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemMapper {

    public static Item toEntity(CreateItemRequest request) {
        return Item.builder()
                .name(request.getName())
                .description(request.getDescription())
                .available(request.getAvailable())
                .build();
    }

    public static void toEntity(UpdateItemRequest request, Item item) {
        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }
    }

    public static ItemResponse toResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .build();
    }
}
