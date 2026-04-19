package com.fahad.stayease.user.service;

import com.fahad.stayease.auth.repository.RefreshTokenRepository;
import com.fahad.stayease.exception.ResourceNotFoundException;
import com.fahad.stayease.user.dto.UserProfileRequest;
import com.fahad.stayease.user.dto.UserProfileResponse;
import com.fahad.stayease.user.model.User;
import com.fahad.stayease.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        log.info("Get profile email={}", email);
        User user = getByEmail(email);
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileRequest request) {
        log.info("Update profile email={}", email);
        User user = getByEmail(email);
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        log.info("Profile updated userId={} email={}", saved.getId(), saved.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public void deleteProfile(String email) {
        log.info("Delete profile email={}", email);
        User user = getByEmail(email);
        refreshTokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
        log.info("Profile deleted userId={} email={}", user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
