package com.laitusneo.recon.repo;

import com.laitusneo.recon.domain.SentFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SentFactRepository extends JpaRepository<SentFact, Long> {

    Optional<SentFact> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT s FROM SentFact s
        WHERE s.reference = :reference AND s.recordedAt <= :asOf
        ORDER BY s.recordedAt DESC
        """)
    List<SentFact> findAsOf(@Param("reference") String reference, @Param("asOf") Instant asOf);

    @Query("SELECT DISTINCT s.reference FROM SentFact s")
    List<String> findAllReferences();
}
