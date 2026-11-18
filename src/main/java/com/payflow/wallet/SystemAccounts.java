package com.payflow.wallet;

import com.payflow.user.User;
import com.payflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Looks up the internal funding wallet created by migration V7.
 * Top-ups are recorded as transfers from this wallet, so balances stay
 * fully ledger-derived.
 */
@Component
@RequiredArgsConstructor
public class SystemAccounts {

    public static final String SYSTEM_EMAIL = "system@payflow.internal";

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    private volatile Long systemWalletId;

    public Long systemWalletId() {
        Long id = systemWalletId;
        if (id == null) {
            User system = userRepository.findByEmail(SYSTEM_EMAIL)
                    .orElseThrow(() -> new IllegalStateException("System user missing - did migration V7 run?"));
            id = walletRepository.findByUserId(system.getId())
                    .orElseThrow(() -> new IllegalStateException("System wallet missing - did migration V7 run?"))
                    .getId();
            systemWalletId = id;
        }
        return id;
    }

    public boolean isSystemWallet(Long walletId) {
        return walletId != null && walletId.equals(systemWalletId());
    }

    public boolean isSystemUser(User user) {
        return SYSTEM_EMAIL.equalsIgnoreCase(user.getEmail());
    }
}
