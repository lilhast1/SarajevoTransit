$ErrorActionPreference = 'Continue'
try {
    $apps = Invoke-RestMethod -Uri 'http://localhost:8761/eureka/apps' -Headers @{Accept='application/json'} -TimeoutSec 10
} catch {
    Write-Output (ConvertTo-Json @{error="EUREKA_FETCH_ERROR"; message=$_.Exception.Message})
    exit 0
}
$results = @()
if ($null -eq $apps.applications.application) {
    Write-Output (ConvertTo-Json @{error="EUREKA_NO_APPS"})
    exit 0
}
foreach ($app in @($apps.applications.application)) {
    $appName = $app.name
    $instances = @($app.instance)
    foreach ($inst in $instances) {
        $instanceId = $inst.instanceId
        $status = $inst.status
        $homeUrl = $null
        if ($inst.homePageUrl) { $homeUrl = $inst.homePageUrl } elseif ($inst.homepageUrl) { $homeUrl = $inst.homepageUrl }
        if (-not $homeUrl) { $homeUrl = "http://localhost/" }
        $healthUrl = $null
        if ($inst.healthCheckUrl) { $healthUrl = $inst.healthCheckUrl } elseif ($inst.healthcheckUrl) { $healthUrl = $inst.healthcheckUrl }
        if (-not $healthUrl) { $healthUrl = $homeUrl.TrimEnd('/') + '/actuator/health' }
        $healthResp = $null
        try {
            $healthResp = Invoke-RestMethod -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
        } catch {
            $healthResp = @{error = $_.Exception.Message}
        }
        $gatewayDoc = $null
        try {
            $docUrl = 'http://localhost:8080/v3/api-docs/' + $appName.ToLower()
            $gwResp = Invoke-WebRequest -Uri $docUrl -UseBasicParsing -TimeoutSec 5
            $gatewayDoc = @{ status = $gwResp.StatusCode }
        } catch {
            $gatewayDoc = @{ error = $_.Exception.Message }
        }
        $results += [PSCustomObject]@{
            app = $appName
            instance = $instanceId
            eurekaStatus = $status
            homePageUrl = $homeUrl
            healthUrl = $healthUrl
            health = $healthResp
            gatewayApiDocs = $gatewayDoc
        }
    }
}
$results | ConvertTo-Json -Depth 6 | Write-Output
