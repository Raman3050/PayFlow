package com.payflow.fraud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/fraud")
@RequiredArgsConstructor
public class FraudAdminController {

    private final FraudAdminService fraudAdminService;

    @GetMapping("/flags")
    public List<FraudFlag> getUnreviewedFlags() {
        log.info("Admin requested unreviewed fraud flags");
        return fraudAdminService.getUnreviewedFlags();
    }

    @PostMapping("/flags/{id}/review")
    public FraudFlag reviewFlag(
            @PathVariable Long id,
            @RequestParam boolean reviewed) {
        log.info("Admin reviewing fraud flag {}: {}", id, reviewed);
        return fraudAdminService.reviewFlag(id, reviewed);
    }
}