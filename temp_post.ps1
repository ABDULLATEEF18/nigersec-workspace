$uri='http://localhost:8080/api/v1/fraud/mock/score'
$body = @{
    transactionId='txn-001'
    senderAccount='acct-001'
    receiverAccount='acct-999'
    amount=2500000
    channel='WEB'
    deviceFingerprint='device-123'
    ipAddress='197.210.75.10'
} | ConvertTo-Json
try {
    $resp = Invoke-RestMethod -Uri $uri -Method Post -ContentType 'application/json' -Body $body -Headers @{ Origin='http://localhost:5173' }
    $resp | ConvertTo-Json
} catch {
    Write-Error $_.Exception.Message
}
