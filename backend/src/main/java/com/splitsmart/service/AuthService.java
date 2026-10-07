package com.splitsmart.service;

import com.splitsmart.dto.Dtos.*;
import com.splitsmart.exception.NotFoundException;
import com.splitsmart.exception.UnauthorizedException;
import com.splitsmart.model.AppUser;
import com.splitsmart.repository.UserRepository;
import com.splitsmart.security.CurrentUser;
import com.splitsmart.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;

    public AuthService(UserRepository userRepo, PasswordEncoder encoder,
                       JwtService jwtService, CurrentUser currentUser) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        AppUser user = userRepo.save(new AppUser(req.name().trim(), email, encoder.encode(req.password())));
        return toAuth(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        AppUser user = userRepo.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                // Same message for wrong email or wrong password (don't reveal which)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return toAuth(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        AppUser u = userRepo.findById(currentUser.id())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new UserResponse(u.getId(), u.getName(), u.getEmail());
    }

    @Transactional(readOnly = true)
    public AppUser currentUserEntity() {
        return userRepo.findById(currentUser.id())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
    }

    private AuthResponse toAuth(AppUser u) {
        String token = jwtService.generateToken(u.getId(), u.getEmail());
        return new AuthResponse(token, new UserResponse(u.getId(), u.getName(), u.getEmail()));
    }
}
