package ru.practicum.shareit.item.storage.inmemory;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemStorage;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryItemStorage implements ItemStorage {

    private final Map<Long, Item> items = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, Set<Long>> ownerItems = new HashMap<>();

    @Override
    public Item create(Item item) {
        item.setId(nextId.getAndIncrement());
        items.put(item.getId(), item);
        ownerItems.computeIfAbsent(item.getOwner().getId(), k -> new HashSet<>()).add(item.getId());
        return item;
    }

    @Override
    public void update(Item item) {
        items.put(item.getId(), item);
    }

    @Override
    public Optional<Item> findById(long id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<Item> findAllOwnerItems(long ownerId) {
        Set<Long> ids = ownerItems.getOrDefault(ownerId, Set.of());
        return ids.stream().map(items::get).toList();
    }

    @Override
    public List<Item> search(String text) {
        String lower = text.toLowerCase();
        return items.values().stream()
                .filter(Item::getAvailable)
                .filter(item -> item.getName().toLowerCase().contains(lower)
                        || item.getDescription().toLowerCase().contains(lower))
                .toList();
    }
}
