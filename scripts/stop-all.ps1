$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot

Write-Host "[INFO] SarajevoTransit shutdown"

Write-Host "[INFO] Stopping Docker services from docker-compose.yml..."
docker compose -f (Join-Path $root "docker-compose.yml") down

Write-Host "[INFO] Stopping OTP docker services..."
docker compose -f (Join-Path $root "docker-compose.otp.yml") down

Write-Host "[INFO] Stopping Java processes for SarajevoTransit services..."
$namePatterns = @(
  "configserver",
  "eurekaserver",
  "userservice",
  "feedbackservice",
  "notificationservice",
  "vehicleservice",
  "routingservice",
  "moneyman",
  "apigateway",
  "otpproxyservice"
)

$javaProcs = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq "java.exe" -and $_.CommandLine }
$killed = 0

foreach ($proc in $javaProcs) {
  $cmd = [string]$proc.CommandLine
  $isTarget = $false
  foreach ($pattern in $namePatterns) {
    if ($cmd -match [Regex]::Escape($pattern)) {
      $isTarget = $true
      break
    }
  }

  if ($isTarget) {
    Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    if (-not $?) {
      continue
    }
    $killed++
    Write-Host "[OK] Stopped PID $($proc.ProcessId)"
  }
}

Write-Host "[INFO] Stopped $killed Java process(es)."
Write-Host "[DONE] Shutdown complete."
