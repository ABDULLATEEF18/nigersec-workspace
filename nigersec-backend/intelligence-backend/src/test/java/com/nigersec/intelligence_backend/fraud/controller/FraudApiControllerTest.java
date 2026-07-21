package com.nigersec.intelligence_backend.fraud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nigersec.intelligence_backend.fraud.dto.TransactionScoreRequest;
import com.nigersec.intelligence_backend.fraud.dto.TransactionScoreResponse;
import com.nigersec.intelligence_backend.fraud.entity.FraudDecision;
import com.nigersec.intelligence_backend.fraud.entity.RiskLevel;
import com.nigersec.intelligence_backend.fraud.service.FraudDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FraudApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class FraudApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FraudDetectionService fraudDetectionService;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void mockScoreEndpointReturnsFraudResponse() throws Exception {
        TransactionScoreResponse response = TransactionScoreResponse.builder()
                .transactionId("txn-mock-100")
                .riskScore(BigDecimal.valueOf(82.5))
                .riskLevel(RiskLevel.HIGH)
                .decision(FraudDecision.BLOCK)
                .flags(List.of("mock device fingerprint matched"))
                .recommendation("Block this transaction immediately")
                .build();

        when(fraudDetectionService.scoreTransaction(any(), any(TransactionScoreRequest.class)))
                .thenReturn(response);

        TransactionScoreRequest request = new TransactionScoreRequest();
        request.setTransactionId("txn-mock-100");
        request.setSenderAccount("acct-001");
        request.setReceiverAccount("acct-999");
        request.setAmount(BigDecimal.valueOf(2500000));
        request.setChannel("WEB");
        request.setDeviceFingerprint("mock-device-001");
        request.setIpAddress("197.210.75.10");

        mockMvc.perform(post("/api/v1/fraud/mock/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mock fraud scoring response"))
                .andExpect(jsonPath("$.data.transactionId").value("txn-mock-100"))
                .andExpect(jsonPath("$.data.decision").value("BLOCK"));
    }
}
