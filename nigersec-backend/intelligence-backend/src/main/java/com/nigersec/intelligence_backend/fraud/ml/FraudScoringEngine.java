package com.nigersec.intelligence_backend.fraud.ml;

import com.nigersec.intelligence_backend.citizen.repository.BreachRecordRepository;
import com.nigersec.intelligence_backend.citizen.service.BreachCheckService;
import com.nigersec.intelligence_backend.fraud.dto.TransactionScoreRequest;
import com.nigersec.intelligence_backend.fraud.entity.FraudDecision;
import com.nigersec.intelligence_backend.fraud.entity.RiskLevel;
import com.nigersec.intelligence_backend.fraud.repository.FraudSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based fraud scoring engine (Phase 1).
 * Designed to be replaced by a trained ML model in Phase 2
 * once data volume exceeds 1,000,000 transaction records.
 *
 * Scoring logic is modular - each rule contributes a weighted score.
 * Total score = sum of all rule contributions, capped at 100.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudScoringEngine {

    private final FraudSignalRepository fraudSignalRepository;
    private final BreachRecordRepository breachRecordRepository;

    @Value("${nigersec.fraud.risk-threshold-medium:40}")
    private int thresholdMedium;

    @Value("${nigersec.fraud.risk-threshold-high:70}")
    private int thresholdHigh;

    public ScoringResult score(TransactionScoreRequest request) {
        long start = System.currentTimeMillis();
        List<String> flags = new ArrayList<>();
        double totalScore = 0;

        String senderHash = BreachCheckService.sha256(request.getSenderAccount());

        // Rule 1: Sender account has known breach records
        long senderBreachCount = breachRecordRepository.countByDataHash(senderHash);
        if (senderBreachCount > 0) {
            totalScore += 30;
            flags.add("Sender account appears in " + senderBreachCount + " known breach record(s)");
        }

        // Rule 2: BVN has prior high/critical fraud signals
        if (request.getSenderBvnHash() != null) {
            boolean bvnHighRisk = fraudSignalRepository.existsByIdentifierHashAndRiskLevelIn(
                    request.getSenderBvnHash(), List.of(RiskLevel.HIGH));
            if (bvnHighRisk) {
                totalScore += 35;
                flags.add("Sender BVN linked to prior high-risk transactions");
            }
        }

        // Rule 3: High transaction velocity from same account (>5 in last hour)
        long recentTxCount = fraudSignalRepository.countByIdentifierHashAndCreatedAtAfter(
                senderHash, Instant.now().minus(1, ChronoUnit.HOURS));
        if (recentTxCount > 5) {
            totalScore += 20;
            flags.add("High transaction velocity: " + recentTxCount + " transactions in the last hour");
        }

        // Rule 4: Unusually large amount (>₦500,000)
        if (request.getAmount() != null && request.getAmount().compareTo(new BigDecimal("500000")) > 0) {
            totalScore += 15;
            flags.add("Transaction amount exceeds ₦500,000 threshold");
        }

        // Rule 5: Off-hours transaction (midnight to 5AM Nigeria time, UTC+1)
        if (request.getTransactionTime() != null) {
            int hour = request.getTransactionTime().atZone(java.time.ZoneId.of("Africa/Lagos")).getHour();
            if (hour >= 0 && hour < 5) {
                totalScore += 10;
                flags.add("Transaction initiated during off-hours (12AM - 5AM WAT)");
            }
        }

        // Rule 6: USSD channel with high amount
        if ("USSD".equals(request.getChannel()) && request.getAmount() != null
                && request.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            totalScore += 10;
            flags.add("Large USSD transaction - common SIM-swap fraud pattern");
        }

        // Mock simulation hooks for local demo traffic and Postman exercises
        if (request.getDeviceFingerprint() != null && request.getDeviceFingerprint().contains("mock")) {
            totalScore += 8;
            flags.add("Mock device fingerprint matched a high-risk demo profile");
        }
        if (request.getIpAddress() != null && request.getIpAddress().startsWith("197.")) {
            totalScore += 6;
            flags.add("Traffic originated from a suspicious IP range");
        }
        if ("POS".equals(request.getChannel()) && request.getAmount() != null
                && request.getAmount().compareTo(new BigDecimal("750000")) > 0) {
            totalScore += 7;
            flags.add("Large POS transaction flagged by the mock payment simulation");
        }

        double capped = Math.min(totalScore, 100.0);
        RiskLevel level   = computeRiskLevel(capped);
        FraudDecision dec = computeDecision(level);

        return ScoringResult.builder()
                .score(BigDecimal.valueOf(capped))
                .riskLevel(level)
                .decision(dec)
                .flags(flags)
                .processingTimeMs(System.currentTimeMillis() - start)
                .senderHash(senderHash)
                .build();
    }

    private RiskLevel computeRiskLevel(double score) {
        if (score >= thresholdHigh)   return RiskLevel.HIGH;
        if (score >= thresholdMedium) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private FraudDecision computeDecision(RiskLevel level) {
        return switch (level) {
            case HIGH   -> FraudDecision.BLOCK;
            case MEDIUM -> FraudDecision.REVIEW;
            case LOW    -> FraudDecision.APPROVE;
        };
    }

    @lombok.Builder @lombok.Getter
    public static class ScoringResult {
        private final BigDecimal score;
        private final RiskLevel riskLevel;
        private final FraudDecision decision;
        private final List<String> flags;
        private final long processingTimeMs;
        private final String senderHash;
    }
}
