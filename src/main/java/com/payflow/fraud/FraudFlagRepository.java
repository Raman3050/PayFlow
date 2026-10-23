package com.payflow.fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {
    List<FraudFlag> findByReviewedFalse();
}