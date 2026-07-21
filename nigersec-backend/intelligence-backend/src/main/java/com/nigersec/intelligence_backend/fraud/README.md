# NigerSec Fraud API

This module exposes a mock-first fraud scoring API for fintechs, banks, and institutions that want a Paystack/Flutterwave-style experience without requiring a live ML model.

## Endpoints

### 1. Mock fraud scoring
POST /api/v1/fraud/mock/score

Example body:

```json
{
  "transactionId": "txn-demo-001",
  "senderAccount": "acct-001",
  "receiverAccount": "acct-999",
  "amount": 2500000,
  "currency": "NGN",
  "channel": "WEB",
  "deviceFingerprint": "mock-device-001",
  "ipAddress": "197.210.75.10"
}
```

Response includes a risk score, decision, and explanation flags.

### 2. Institutional pricing plans
GET /api/v1/fraud/pricing

Returns mock SaaS pricing for Starter, Growth, and Enterprise institutions.

### 3. Realtime notifications
WebSocket: ws://localhost:8080/ws/fraud

Connect with any websocket client (including Postman) and listen for alerts like:

```json
{
  "event": "fraud.alert",
  "riskLevel": "HIGH",
  "decision": "BLOCK"
}
```

## Notes
- The scoring engine uses mock heuristics and deterministic rules so it can be exercised locally without a real data pipeline.
- The endpoint is designed to be easy to test in Postman and similar tools.
