package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemResponse;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;


@RestController
@RequestMapping("/items")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ItemResponse create(@Valid @RequestBody CreateItemRequest request,
                               @RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        ItemResponse created = itemService.create(request, userId);
        log.info("Создана вещь: itemId={} userId={}", created.getId(), userId);
        return created;
    }

    @PatchMapping("/{itemId}")
    public ItemResponse update(@PathVariable long itemId,
                               @RequestBody UpdateItemRequest itemDto,
                               @RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        ItemResponse updated = itemService.update(itemId, itemDto, userId);
        log.info("Обновлена вещь: itemId={} userId={}", updated.getId(), userId);
        return updated;
    }

    @GetMapping("/{itemId}")
    public ItemResponse getItem(@PathVariable @Positive long itemId) {
        log.debug("Получение вещи: id={}", itemId);
        return itemService.findById(itemId);
    }

    @GetMapping
    public List<ItemResponse> getAll(@RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        log.debug("Получение всех вещей пользователя: userId={}", userId);
        return itemService.findAllOwnerItems(userId);
    }

    @GetMapping("/search")
    public List<ItemResponse> search(@RequestParam String text) {
        log.info("Поиск вещей: text='{}'", text);
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemService.search(text);
    }
}
