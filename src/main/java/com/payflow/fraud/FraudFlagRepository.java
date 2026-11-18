package com.payflow.fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {
    List<FraudFlag> findByReviewedFalseOrderByCreatedAtDesc();

    boolean existsByTransaction_IdAndReviewedFalse(Long transactionId);

    /** Which of the given transactions still have an unreviewed fraud flag. */
    @Query("SELECT DISTINCT f.transaction.id FROM FraudFlag f WHERE f.transaction.id IN :ids AND f.reviewed = false")
    List<Long> findOpenFlaggedTransactionIds(@Param("ids") Collection<Long> ids);
}
