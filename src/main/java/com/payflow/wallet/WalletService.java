package com.payflow.wallet;

import com.payflow.exception.InvalidTransferException;
import com.payflow.transaction.LedgerEntryRepository;
import com.payflow.user.User;
import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.exceptions.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SystemAccounts systemAccounts;

    @Transactional
    public WalletResponse createWallet(User user) {
        if (walletRepository.existsByUserId(user.getId())) {
            log.warn("Wallet already exists for user: {}", user.getId());
            throw new IllegalStateException("Wallet already exists for user");
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
        return toWalletResponse(getWalletEntity(userId));
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletById(Long walletId) {
        return toWalletResponse(findById(walletId));
    }

    @Transactional
    public WalletResponse setStatus(Long walletId, Wallet.WalletStatus status) {
        if (systemAccounts.isSystemWallet(walletId)) {
            throw new InvalidTransferException("The system wallet cannot be frozen");
        }
        Wallet wallet = findById(walletId);
        wallet.setStatus(status); // @Version is bumped by Hibernate on update
        wallet = walletRepository.saveAndFlush(wallet);
        log.info("Wallet {} is now {}", wallet.getId(), status);
        return toWalletResponse(wallet);
    }

    public WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .balance(ledgerEntryRepository.balanceOf(wallet.getId()))
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .build();
    }

    public Wallet getWalletEntity(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
    }

    private Wallet findById(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
    }
}
