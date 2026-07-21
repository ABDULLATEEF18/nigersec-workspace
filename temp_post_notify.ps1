$body = @{
    transactionId = 'txn-notify-001'
    senderAccount = 'acct-001'
    receiverAccount = 'acct-999'
    amount = 2500000
    channel = 'WEB'
    deviceFingerprint = 'demo-mock-device'
    ipAddress = '197.210.75.10'
} | ConvertTo-Json

try {
    $resp = Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/fraud/mock/score' -Method Post -Headers @{ 'Origin' = 'http://localhost:5173' } -ContentType 'application/json' -Body $body -ErrorAction Stop
    $resp | ConvertTo-Json -Depth 5
} catch {
    Write-Host 'ERROR:' $_.Exception.Message
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host ($reader.ReadToEnd())
        $reader.Close()
    }
    exit 1
}
