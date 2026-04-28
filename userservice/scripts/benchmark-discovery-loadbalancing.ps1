param(
    [string]$UserserviceBaseUrl = "http://localhost:8080",
    [int]$Iterations = 100,
    [int]$TimeoutSec = 10,
    [string]$OutputPath = "docs/api-test-evidence/discovery-loadbalancing-benchmark.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Iterations -lt 1) {
    throw "Iterations must be at least 1."
}

function Get-PercentileValue {
    param(
        [double[]]$Values,
        [double]$Percentile
    )

    if ($Values.Count -eq 0) {
        return 0.0
    }

    $sorted = $Values | Sort-Object
    $rank = [Math]::Ceiling(($Percentile / 100.0) * $sorted.Count)
    $index = [Math]::Max(0, [Math]::Min($sorted.Count - 1, $rank - 1))
    return [Math]::Round([double]$sorted[$index], 2)
}

function Build-Stats {
    param([double[]]$Values)

    if ($Values.Count -eq 0) {
        return [ordered]@{
            minMs = 0.0
            avgMs = 0.0
            p95Ms = 0.0
            maxMs = 0.0
        }
    }

    $avg = ($Values | Measure-Object -Average).Average
    $min = ($Values | Measure-Object -Minimum).Minimum
    $max = ($Values | Measure-Object -Maximum).Maximum

    return [ordered]@{
        minMs = [Math]::Round([double]$min, 2)
        avgMs = [Math]::Round([double]$avg, 2)
        p95Ms = Get-PercentileValue -Values $Values -Percentile 95
        maxMs = [Math]::Round([double]$max, 2)
    }
}

function Invoke-ModeBenchmark {
    param(
        [string]$Mode,
        [string]$Url,
        [int]$Iterations,
        [int]$TimeoutSec
    )

    Write-Host "Running $Iterations requests for mode '$Mode'..."

    $instanceCounts = @{}
    $requestDurations = New-Object System.Collections.Generic.List[double]
    $downstreamDurations = New-Object System.Collections.Generic.List[double]
    $successCount = 0
    $failureCount = 0

    $totalStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

    for ($i = 1; $i -le $Iterations; $i++) {
        $singleStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

        try {
            $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec $TimeoutSec
            $singleStopwatch.Stop()

            $requestDurationMs = [Math]::Round($singleStopwatch.Elapsed.TotalMilliseconds, 2)
            $requestDurations.Add($requestDurationMs)

            if ($null -ne $response.durationMs) {
                $downstreamDurations.Add([double]$response.durationMs)
            }

            $instanceId = if ($null -ne $response.instanceId -and "$($response.instanceId)".Trim().Length -gt 0) {
                [string]$response.instanceId
            }
            else {
                "unknown-instance"
            }

            if (-not $instanceCounts.ContainsKey($instanceId)) {
                $instanceCounts[$instanceId] = 0
            }
            $instanceCounts[$instanceId]++

            $successCount++
        }
        catch {
            $singleStopwatch.Stop()
            $failureCount++
            Write-Warning "[$Mode][$i/$Iterations] request failed: $($_.Exception.Message)"
        }
    }

    $totalStopwatch.Stop()

    [ordered]@{
        mode = $Mode
        successCount = $successCount
        failureCount = $failureCount
        totalDurationMs = [Math]::Round($totalStopwatch.Elapsed.TotalMilliseconds, 2)
        requestStats = Build-Stats -Values $requestDurations.ToArray()
        downstreamStats = Build-Stats -Values $downstreamDurations.ToArray()
        instanceDistribution = $instanceCounts
    }
}

$directUrl = "$UserserviceBaseUrl/api/v1/discovery/feedback/ping?mode=direct"
$lbUrl = "$UserserviceBaseUrl/api/v1/discovery/feedback/ping?mode=lb"

$directResult = Invoke-ModeBenchmark -Mode "direct" -Url $directUrl -Iterations $Iterations -TimeoutSec $TimeoutSec
$loadBalancedResult = Invoke-ModeBenchmark -Mode "lb" -Url $lbUrl -Iterations $Iterations -TimeoutSec $TimeoutSec

$result = [ordered]@{
    generatedAt = (Get-Date).ToString("o")
    userserviceBaseUrl = $UserserviceBaseUrl
    iterations = $Iterations
    endpoints = [ordered]@{
        direct = $directUrl
        loadBalanced = $lbUrl
    }
    direct = $directResult
    loadBalanced = $loadBalancedResult
}

$absoluteOutputPath = if ([System.IO.Path]::IsPathRooted($OutputPath)) {
    $OutputPath
}
else {
    Join-Path (Get-Location) $OutputPath
}

$outputDirectory = Split-Path -Parent $absoluteOutputPath
if (-not [string]::IsNullOrWhiteSpace($outputDirectory) -and -not (Test-Path $outputDirectory)) {
    New-Item -Path $outputDirectory -ItemType Directory | Out-Null
}

$result | ConvertTo-Json -Depth 8 | Set-Content -Path $absoluteOutputPath -Encoding UTF8

Write-Host ""
Write-Host "Benchmark finished."
Write-Host "Result file: $absoluteOutputPath"
Write-Host ""
Write-Host "Direct mode summary:"
$directResult | ConvertTo-Json -Depth 6
Write-Host ""
Write-Host "Load-balanced mode summary:"
$loadBalancedResult | ConvertTo-Json -Depth 6
