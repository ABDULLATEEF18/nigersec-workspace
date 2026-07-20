package com.nigersec.intelligence_backend.citizen.dto;

import com.nigersec.intelligence_backend.citizen.entity.SeverityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BreachCheckResponse {
    private boolean breached;
    private int breachCount;
    private List<BreachSummary> breaches;
    private String recommendation;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BreachSummary {
        private String source;
        private String exposedFields;
        private SeverityLevel severity;
        private Instant breachDate;
        private String action;
    }
}
