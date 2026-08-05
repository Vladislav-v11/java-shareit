package ru.practicum.shareit.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.model.ConflictException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.user.dto.CreateUserRequest;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserResponse;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.user.storage.UserStorage;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserStorage userStorage;

    @Override
    public UserResponse findById(long id) {
        return userStorage.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        if (request.getEmail() != null) {
            userStorage.findByEmail(request.getEmail())
                    .ifPresent(user -> {
                        log.warn("User with email {} already exists", user.getEmail());
                        throw new ConflictException("Email already in use");
                    });
        }
        User user = UserMapper.toEntity(request);
        User created = userStorage.create(user);
        return UserMapper.toResponse(created);
    }

    @Override
    public UserResponse update(UpdateUserRequest request) {
        User user = userStorage.findById(request.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserMapper.toEntity(request, user);

        if (request.getEmail() != null) {
            userStorage.findByEmail(request.getEmail())
                    .filter(existing -> existing.getId() != user.getId())
                    .ifPresent(existing -> {
                        throw new ConflictException("Email already in use");
                    });
        }

        userStorage.update(user);
        return UserMapper.toResponse(user);
    }

    @Override
    public void delete(long id) {
        userStorage.delete(id);
    }
}
