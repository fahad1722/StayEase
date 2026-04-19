package com.fahad.stayease.auth.service;

import com.fahad.stayease.auth.JwtUtil;
import com.fahad.stayease.auth.dto.AuthResponse;
import com.fahad.stayease.auth.dto.LoginRequest;
import com.fahad.stayease.auth.dto.RefreshTokenRequest;
import com.fahad.stayease.auth.dto.RegisterRequest;
import com.fahad.stayease.auth.model.RefreshToken;
import com.fahad.stayease.auth.repository.RefreshTokenRepository;
import com.fahad.stayease.exception.ResourceNotFoundException;
import com.fahad.stayease.exception.UnauthorizedActionException;
import com.fahad.stayease.user.model.User;
import com.fahad.stayease.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Register attempt for email={} role={}", request.getEmail(), request.getRole());
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Register rejected: email already in use={}", request.getEmail());
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: userId={} email={}", savedUser.getId(), savedUser.getEmail());
        return issueTokens(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email={}", request.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = (User) authentication.getPrincipal();
        log.info("Login successful for userId={} email={}", user.getId(), user.getEmail());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token attempt");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedActionException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token rejected: revoked or expired for userId={}", refreshToken.getUser().getId());
            throw new UnauthorizedActionException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();

        if (!jwtUtil.isTokenValid(refreshToken.getToken(), user, "refresh")) {
            log.warn("Refresh token rejected: invalid token for userId={}", user.getId());
            throw new UnauthorizedActionException("Refresh token is invalid");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token rotated for userId={}", user.getId());

        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        log.info("Logout attempt");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        SecurityContextHolder.clearContext();
        log.info("Logout successful for userId={}", refreshToken.getUser().getId());
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenValue = jwtUtil.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(jwtUtil.extractExpiration(refreshTokenValue).toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        log.info("Issued access/refresh tokens for userId={}", user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .build();
    }
}
