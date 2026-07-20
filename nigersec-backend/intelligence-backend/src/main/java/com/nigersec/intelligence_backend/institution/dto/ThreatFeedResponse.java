package com.nigersec.intelligence_backend.institution.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nigersec.intelligence_backend.citizen.entity.SeverityLevel;
import com.nigersec.intelligence_backend.institution.entity.AttackType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class ThreatFeedResponse {
    private UUID id;
    private AttackType attackType;
    private String description;
    private String indicators;
    private SeverityLevel severity;
    private String mitigationSteps;
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant reportedAt;
    // reportingInstitutionId deliberately excluded (anonymized)
}
