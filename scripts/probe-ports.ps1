$map = @{configserver=8888; eurekaserver=8761; apigateway=8080; moneyman=8081; userservice=8082; vehicleservice=8083; notificationservice=8086; routingservice=9999; feedbackservice=8091}
$out = @()
foreach ($k in $map.Keys) {
    $p = $map[$k]
    $url = "http://127.0.0.1:$p/actuator/health"
    try {
        $r = Invoke-RestMethod -Uri $url -UseBasicParsing -TimeoutSec 4
        $status = $r.status
        $body = $r | ConvertTo-Json -Depth 2
    } catch {
        $status = ('ERROR: ' + $_.Exception.Message)
        $body = $_.Exception.Message
    }
    $out += [PSCustomObject]@{service=$k; port=$p; status=$status; body=$body}
}
$out | ConvertTo-Json -Depth 4 | Write-Output
