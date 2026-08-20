package com.laitusneo.recon.repo;

import com.laitusneo.recon.domain.ReportedFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReportedFactRepository extends JpaRepository<ReportedFact, Long> {

    Optional<ReportedFact> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT r FROM ReportedFact r
        WHERE r.reference = :reference AND r.recordedAt <= :asOf
        ORDER BY r.recordedAt DESC
        """)
    List<ReportedFact> findAsOf(@Param("reference") String reference, @Param("asOf") Instant asOf);

    @Query("SELECT DISTINCT r.reference FROM ReportedFact r")
    List<String> findAllReferences();
}
