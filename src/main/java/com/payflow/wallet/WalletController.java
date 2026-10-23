package com.payflow.wallet;

import com.payflow.wallet.dto.TopUpRequest;
import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.exceptions.WalletNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.payflow.security.UserPrincipal;
import com.payflow.user.User;
import com.payflow.user.UserRepository;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    private Long getUserId(UserDetails userDetails) {
        if (userDetails instanceof UserPrincipal up) {
            return up.getId();
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting wallet for user: {}", userDetails.getUsername());
        Long userId = getUserId(userDetails);
        WalletResponse wallet = walletService.getWallet(userId);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/top-up")
    public ResponseEntity<WalletResponse> topUpWallet(
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        request.setUserId(userId);
        log.info("Topping up wallet for user {} with amount {}", userId, request.getAmount());

        WalletResponse wallet = walletService.topUpWallet(request);
        return ResponseEntity.ok(wallet);
    }
}