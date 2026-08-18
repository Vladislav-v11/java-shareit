package ru.practicum.shareit.request;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.CreateItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestResponse;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

@RestController
@RequestMapping(path = "/requests")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ItemRequestController {

    private final ItemRequestService itemRequestService;

    @PostMapping
    public ItemRequestResponse create(@RequestBody CreateItemRequest request,
                                      @RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        ItemRequestResponse created = itemRequestService.create(request, userId);
        log.info("Создан запрос вещи: requestId={}, userId={}", created.getId(), userId);
        return created;
    }

    @GetMapping
    public List<ItemRequestResponse> getOwnRequests(@RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        log.debug("Получение своих запросов: userId={}", userId);
        return itemRequestService.getOwnRequests(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestResponse> getAllOtherRequests(@RequestHeader("X-Sharer-User-Id") @Positive long userId) {
        log.debug("Получение чужих запросов: userId={}", userId);
        return itemRequestService.getAllOtherRequests(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestResponse getById(@PathVariable @Positive long requestId,
                                       @RequestHeader("X-Sharer-User-Id") long userId) {
        log.debug("Получение запроса: requestId={}, userId={}", requestId, userId);
        return itemRequestService.getById(requestId);
    }
}
