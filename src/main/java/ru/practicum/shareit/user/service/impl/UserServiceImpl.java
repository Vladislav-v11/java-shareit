package ru.practicum.shareit.user.service.impl;

import jakarta.transaction.Transactional;
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
import ru.practicum.shareit.user.storage.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse findById(long id) {
        return userRepository.findById(id)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(user -> {
                        log.warn("User with email {} already exists", user.getEmail());
                        throw new ConflictException("Email already in use");
                    });
        }
        User user = UserMapper.toEntity(request);
        User created = userRepository.save(user);
        return UserMapper.toResponse(created);
    }

    @Override
    @Transactional
    public UserResponse update(UpdateUserRequest request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserMapper.toEntity(request, user);

        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> existing.getId() != user.getId())
                    .ifPresent(existing -> {
                        throw new ConflictException("Email already in use");
                    });
        }

        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void delete(long id) {
        userRepository.deleteById(id);
    }
}
