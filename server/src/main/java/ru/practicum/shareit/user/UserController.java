package ru.practicum.shareit.user;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.user.dto.CreateUserRequest;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserResponse;
import ru.practicum.shareit.user.service.UserService;

@RestController
@RequestMapping(path = "/users")
@Validated
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable @Positive long id) {
        log.debug("Получение пользователя: id={}", id);
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody CreateUserRequest userDto) {
        UserResponse created = userService.create(userDto);
        log.info("Создан пользователь: id={}", created.getId());
        return created;
    }

    @PatchMapping("/{id}")
    public UserResponse patchUser(@PathVariable @Positive long id, @RequestBody UpdateUserRequest userDto) {
        userDto.setId(id);
        UserResponse updated = userService.update(userDto);
        log.info("Обновлен пользователь: id={}", updated.getId());
        return updated;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable @Positive long id) {
        userService.delete(id);
        log.info("Удален пользователь: id={}", id);
    }
}
