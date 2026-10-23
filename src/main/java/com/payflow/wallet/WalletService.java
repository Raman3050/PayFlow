package com.payflow.wallet;

import com.payflow.exception.InsufficientBalanceException;
import com.payflow.wallet.dto.TopUpRequest;
import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.exceptions.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public WalletResponse createWallet(com.payflow.user.User user) {
        if (walletRepository.existsByUserId(user.getId())) {
            log.warn("Wallet already exists for user: {}", user.getId());
            throw new RuntimeException("Wallet already exists for user");
        }

        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .currency("USD")
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Created wallet with ID {} for user {}", wallet.getId(), user.getId());

        return toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        return toWalletResponse(wallet);
    }

    @Transactional
    public WalletResponse topUpWallet(TopUpRequest request) {
        Wallet wallet = walletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + request.getUserId()));

        log.info("Topping up wallet {} with amount {}", wallet.getId(), request.getAmount());

        wallet.setVersion(wallet.getVersion() + 1);
        walletRepository.save(wallet);

        return toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getBalance(Long userId) {
        return getWallet(userId);
    }

    public WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    public Wallet getWalletEntity(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
    }

    public boolean existsByUserId(Long userId) {
        return walletRepository.existsByUserId(userId);
    }
}