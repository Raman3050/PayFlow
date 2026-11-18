package com.payflow.fraud;

import com.payflow.exception.ResourceNotFoundException;
import com.payflow.transaction.Transaction;
import com.payflow.user.User;
import com.payflow.user.UserRepository;
import com.payflow.wallet.Wallet;
import com.payflow.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAdminService {

    private final FraudFlagRepository fraudFlagRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FraudFlagResponse> getUnreviewedFlags() {
        return fraudFlagRepository.findByReviewedFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FraudFlagResponse reviewFlag(Long flagId, boolean reviewed) {
        FraudFlag flag = fraudFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud flag not found: " + flagId));

        flag.setReviewed(reviewed);
        log.info("Fraud flag {} marked reviewed={}", flagId, reviewed);
        return toResponse(fraudFlagRepository.save(flag));
    }

    private FraudFlagResponse toResponse(FraudFlag flag) {
        Transaction t = flag.getTransaction();
        Optional<Wallet> fromWallet = walletRepository.findById(t.getFromWallet());

        return FraudFlagResponse.builder()
                .id(flag.getId())
                .rule(flag.getRule())
                .description(flag.getDescription())
                .reviewed(flag.isReviewed())
                .createdAt(flag.getCreatedAt())
                .transactionId(t.getId())
                .fromWalletId(t.getFromWallet())
                .toWalletId(t.getToWallet())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .fromWalletStatus(fromWallet.map(w -> w.getStatus().name()).orElse(null))
                .fromUserEmail(fromWallet.flatMap(w -> userRepository.findById(w.getUserId()))
                        .map(User::getEmail).orElse(null))
                .build();
    }
}
