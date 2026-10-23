package com.payflow.admin;

import com.payflow.admin.dto.UserResponse;
import com.payflow.user.User;
import com.payflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        log.info("Admin requested list of all users");
        return userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}