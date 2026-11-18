package com.payflow.user;

import com.payflow.exception.EmailAlreadyUsedException;
import com.payflow.security.JwtUtils;
import com.payflow.security.UserPrincipal;
import com.payflow.user.dto.AuthResponse;
import com.payflow.user.dto.LoginRequest;
import com.payflow.user.dto.RegisterRequest;
import com.payflow.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    /**
     * Public sign-up always creates a USER. Admins are created from configuration
     * (see AdminBootstrap), never by the client choosing a role.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException("An account with this email already exists");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user with ID: {}", user.getId());

        walletService.createWallet(user);

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return issueTokens(user);
    }

    /** Swaps a valid refresh token for a fresh access + refresh token pair. */
    public AuthResponse refresh(String refreshToken) {
        String email = jwtUtils.parse(refreshToken, JwtUtils.REFRESH)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"))
                .getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(principal))
                .refreshToken(jwtUtils.generateRefreshToken(principal))
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
