package com.nigersec.intelligence_backend.fraud.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PricingPlanResponse {
    private String planId;
    private String name;
    private String summary;
    private BigDecimal monthlyFee;
    private BigDecimal perThousandTransactions;
    private String recommendedFor;
    private List<String> features;
    private String setupFee;
}
