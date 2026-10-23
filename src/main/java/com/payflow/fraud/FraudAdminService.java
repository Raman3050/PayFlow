package com.payflow.fraud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAdminService {

    private final FraudFlagRepository fraudFlagRepository;

    @Transactional(readOnly = true)
    public List<FraudFlag> getUnreviewedFlags() {
        return fraudFlagRepository.findByReviewedFalse();
    }

    @Transactional
    public FraudFlag reviewFlag(Long flagId, boolean reviewed) {
        FraudFlag flag = fraudFlagRepository.findById(flagId)
                .orElseThrow(() -> new RuntimeException("Fraud flag not found: " + flagId));

        flag.setReviewed(reviewed);
        return fraudFlagRepository.save(flag);
    }
}