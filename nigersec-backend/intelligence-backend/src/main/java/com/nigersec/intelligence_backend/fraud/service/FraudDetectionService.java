package com.nigersec.intelligence_backend.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nigersec.intelligence_backend.common.exception.NigerSecException;
import com.nigersec.intelligence_backend.fraud.dto.ApiKeyResponse;
import com.nigersec.intelligence_backend.fraud.dto.PricingPlanResponse;
import com.nigersec.intelligence_backend.fraud.dto.TransactionScoreRequest;
import com.nigersec.intelligence_backend.fraud.dto.TransactionScoreResponse;
import com.nigersec.intelligence_backend.fraud.entity.ApiKey;
import com.nigersec.intelligence_backend.fraud.entity.ApiKeyTier;
import com.nigersec.intelligence_backend.fraud.entity.FraudSignal;
import com.nigersec.intelligence_backend.fraud.ml.FraudScoringEngine;
import com.nigersec.intelligence_backend.fraud.repository.ApiKeyRepository;
import com.nigersec.intelligence_backend.fraud.repository.FraudSignalRepository;
import com.nigersec.intelligence_backend.messaging.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nigersec.intelligence_backend.citizen.service.BreachCheckService.sha256;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudScoringEngine scoringEngine;
    private final FraudSignalRepository fraudSignalRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final ObjectMapper objectMapper;
    private final FraudRealtimeNotificationService fraudRealtimeNotificationService;

    /**
     * Core fraud scoring endpoint.
     * Scores a transaction and persists the signal for model training.
     * Target: respond in under 200ms.
     */
    @Transactional
    public TransactionScoreResponse scoreTransaction(UUID institutionId,
                                                     TransactionScoreRequest request) {
        UUID resolvedInstitutionId = institutionId != null ? institutionId : UUID.fromString("11111111-1111-1111-1111-111111111111");
        FraudScoringEngine.ScoringResult result = scoringEngine.score(request);

        // Persist signal for ML training and velocity checks
        FraudSignal signal = FraudSignal.builder()
                .identifierHash(result.getSenderHash())
                .riskScore(result.getScore())
                .riskLevel(result.getRiskLevel())
                .decision(result.getDecision())
                .flagReasons(toJson(result.getFlags()))
                .reportedByInstitution(resolvedInstitutionId)
                .build();
        signal = fraudSignalRepository.save(signal);

        // Publish high-risk decisions to Kafka for real-time alerting
        if (result.getRiskLevel() == com.nigersec.intelligence_backend.fraud.entity.RiskLevel.HIGH) {
            kafkaEventPublisher.publishFraudFlagged(Map.of(
                    "transactionId",   request.getTransactionId(),
                    "institutionId",   resolvedInstitutionId.toString(),
                    "riskScore",       result.getScore().toString(),
                    "decision",        result.getDecision().name()
            ));
        }

        String recommendation = switch (result.getDecision()) {
            case BLOCK   -> "Block this transaction immediately and notify the account holder.";
            case REVIEW  -> "Flag for manual review before processing.";
            case APPROVE -> "Transaction appears legitimate. Proceed normally.";
        };

        fraudRealtimeNotificationService.notifyFraudEvent(resolvedInstitutionId, TransactionScoreResponse.builder()
                .transactionId(request.getTransactionId())
                .riskScore(result.getScore())
                .riskLevel(result.getRiskLevel())
                .decision(result.getDecision())
                .flags(result.getFlags())
                .recommendation(recommendation)
                .build());

        return TransactionScoreResponse.builder()
                .scoreId(signal.getId())
                .transactionId(request.getTransactionId())
                .riskScore(result.getScore())
                .riskLevel(result.getRiskLevel())
                .decision(result.getDecision())
                .flags(result.getFlags())
                .recommendation(recommendation)
                .processingTimeMs(result.getProcessingTimeMs())
                .scoredAt(Instant.now())
                .build();
    }

    /**
     * Get fraud signal history for an institution's dashboard.
     */
    public Page<FraudSignal> getFraudHistory(UUID institutionId, Pageable pageable) {
        return fraudSignalRepository.findByReportedByInstitutionOrderByCreatedAtDesc(
                institutionId, pageable);
    }

    public List<PricingPlanResponse> getPricingPlans() {
        return List.of(
                PricingPlanResponse.builder()
                        .planId("starter")
                        .name("Starter")
                        .summary("Ideal for regional fintechs")
                        .monthlyFee(new java.math.BigDecimal("250000"))
                        .perThousandTransactions(new java.math.BigDecimal("150"))
                        .recommendedFor("SMEs and digital lenders")
                        .features(List.of("2,000 monthly checks", "Realtime rule alerts", "Postman-friendly demo API"))
                        .setupFee("Free")
                        .build(),
                PricingPlanResponse.builder()
                        .planId("growth")
                        .name("Growth")
                        .summary("For scaling payment providers")
                        .monthlyFee(new java.math.BigDecimal("750000"))
                        .perThousandTransactions(new java.math.BigDecimal("110"))
                        .recommendedFor("Payment aggregators and banks")
                        .features(List.of("50,000 monthly checks", "Webhook and WS alerts", "Institution dashboard"))
                        .setupFee("₦250,000")
                        .build(),
                PricingPlanResponse.builder()
                        .planId("enterprise")
                        .name("Enterprise")
                        .summary("Dedicated fraud operations for national platforms")
                        .monthlyFee(new java.math.BigDecimal("2500000"))
                        .perThousandTransactions(new java.math.BigDecimal("80"))
                        .recommendedFor("Tier-1 processors and telcos")
                        .features(List.of("Unlimited volume", "Dedicated analyst workspace", "Priority incident support"))
                        .setupFee("Custom")
                        .build()
        );
    }

    /**
     * Issue a new API key. The raw key is returned once and never stored.
     * Only the SHA-256 hash is persisted.
     */
    @Transactional
    public ApiKeyResponse issueApiKey(UUID institutionId, ApiKeyTier tier) {
        String rawKey = "nsk_" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(generateSecureBytes(32));
        String keyHash = sha256(rawKey);

        long limit = switch (tier) {
            case DEVELOPER  -> 100_000L;
            case BUSINESS   -> 1_000_000L;
            case ENTERPRISE -> Long.MAX_VALUE;
        };

        ApiKey apiKey = ApiKey.builder()
                .keyHash(keyHash)
                .institutionId(institutionId)
                .tier(tier)
                .monthlyCallLimit(limit)
                .active(true)
                .build();
        apiKey = apiKeyRepository.save(apiKey);

        return ApiKeyResponse.builder()
                .keyId(apiKey.getId())
                .rawApiKey(rawKey)
                .tier(tier)
                .monthlyCallLimit(limit)
                .build();
    }

    public List<ApiKey> listApiKeys(UUID institutionId) {
        return apiKeyRepository.findByInstitutionIdAndActiveTrue(institutionId);
    }

    @Transactional
    public void revokeApiKey(UUID keyId, UUID institutionId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> NigerSecException.notFound("API key not found"));
        if (!key.getInstitutionId().equals(institutionId)) {
            throw NigerSecException.forbidden("You do not own this API key");
        }
        key.setActive(false);
        apiKeyRepository.save(key);
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private byte[] generateSecureBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
