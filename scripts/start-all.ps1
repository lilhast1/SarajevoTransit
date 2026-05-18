param(
  [switch]$IncludeOtp
)

$ErrorActionPreference = "Stop"

function Import-DotEnv {
  param([string]$Path)

  if (-not (Test-Path $Path)) {
    return
  }

  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith("#")) {
      return
    }

    $parts = $line -split "=", 2
    if ($parts.Count -ne 2) {
      return
    }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
  }
}

function Start-ServiceWindow {
  param(
    [string]$Root,
    [string]$Service,
    [int]$Port
  )

  $servicePath = Join-Path $Root $Service
  if (-not (Test-Path $servicePath)) {
    throw "Service directory not found: $servicePath"
  }

  $wrapperPath = Join-Path $servicePath "mvnw.cmd"
  if (Test-Path $wrapperPath) {
    $runCommand = "Set-Location '$servicePath'; .\\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=$Port'"
  } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    $runCommand = "Set-Location '$servicePath'; mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=$Port'"
  } else {
    throw "Cannot start '$Service': missing mvnw.cmd and Maven (mvn) is not available on PATH."
  }

  Start-Process -FilePath "powershell" -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $runCommand) -WindowStyle Normal | Out-Null
  Write-Host "[OK] Started $Service on port $Port"
}

function Test-PortAvailable {
  param([int]$Port)

  $listener = $null
  try {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
    $listener.Start()
    return $true
  } catch {
    return $false
  } finally {
    if ($listener -ne $null) {
      $listener.Stop()
    }
  }
}

function Wait-ForHttpOk {
  param(
    [string]$Url,
    [int]$TimeoutSeconds = 120,
    [int]$PollIntervalSeconds = 2
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
      if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
        return
      }
    } catch {
      # Keep waiting until the endpoint comes up.
    }

    Start-Sleep -Seconds $PollIntervalSeconds
  }

  throw "Timed out waiting for $Url"
}

function Get-RandomFreePort {
  param([System.Collections.Generic.HashSet[int]]$Reserved)

  for ($i = 0; $i -lt 50; $i++) {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    $listener.Stop()

    if (-not $Reserved.Contains($port) -and (Test-PortAvailable -Port $port)) {
      return $port
    }
  }

  throw "Failed to allocate a free random port after multiple attempts."
}

function Assert-ServiceDependencies {
  param(
    [string]$Root,
    [string[]]$Services
  )

  $missing = @()
  foreach ($service in $Services) {
    $servicePath = Join-Path $Root $service
    if (-not (Test-Path $servicePath)) {
      $missing += "Missing service directory: $servicePath"
      continue
    }

    $wrapperPath = Join-Path $servicePath "mvnw.cmd"
    if ((-not (Test-Path $wrapperPath)) -and (-not (Get-Command mvn -ErrorAction SilentlyContinue))) {
      $missing += "Service '$service' has no mvnw.cmd and Maven (mvn) is not on PATH"
    }
  }

  if ($missing.Count -gt 0) {
    throw ("Startup pre-check failed:`n - " + ($missing -join "`n - "))
  }
}

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "[INFO] SarajevoTransit startup"
Write-Host "[INFO] Root: $root"

Import-DotEnv -Path (Join-Path $root ".env")

if (-not $env:DB_PASSWORD) {
  Write-Warning "DB_PASSWORD is not set. Create .env from .env.example before starting."
}

Write-Host "[INFO] Starting databases with Docker Compose..."
docker compose up -d

if ($IncludeOtp) {
  Write-Host "[INFO] Starting OTP container..."
  docker compose -f (Join-Path $root "docker-compose.otp.yml") up -d otp
}

$services = @(
  "configserver",
  "eurekaserver",
  "userservice",
  "feedbackservice",
  "notificationservice",
  "vehicleservice",
  "routingservice",
  "otpproxyservice",
  "moneyman",
  "apigateway"
)

Assert-ServiceDependencies -Root $root -Services $services

$fixedPorts = @{
  "configserver" = 8888
  "eurekaserver" = 8761
  "apigateway" = 8080
}

$preferredPorts = @{
  "userservice" = 8082
  "feedbackservice" = 8091
  "notificationservice" = 8086
  "vehicleservice" = 8083
  "routingservice" = 9999
  "moneyman" = 8081
  "otpproxyservice" = 8084
}

$reservedPorts = [System.Collections.Generic.HashSet[int]]::new()
$selectedPorts = @{}

Write-Host "[INFO] Starting Spring services in separate windows..."
foreach ($service in $services) {
  if ($fixedPorts.ContainsKey($service)) {
    $port = [int]$fixedPorts[$service]
    if ($reservedPorts.Contains($port) -or -not (Test-PortAvailable -Port $port)) {
      throw "Cannot start '$service' on locked port $port because the port is already in use."
    }
  } else {
    $preferred = [int]$preferredPorts[$service]
    if (-not $reservedPorts.Contains($preferred) -and (Test-PortAvailable -Port $preferred)) {
      $port = $preferred
    } else {
      $port = Get-RandomFreePort -Reserved $reservedPorts
      Write-Host "[WARN] $service preferred port $preferred is busy; using random port $port"
    }
  }

  $reservedPorts.Add($port) | Out-Null
  $selectedPorts[$service] = $port
  Start-ServiceWindow -Root $root -Service $service -Port $port
  Start-Sleep -Seconds 2

  if ($service -eq "configserver") {
    Write-Host "[INFO] Waiting for configserver health..."
    Wait-ForHttpOk -Url "http://localhost:8888/actuator/health" -TimeoutSeconds 120
  }

  if ($service -eq "eurekaserver") {
    Write-Host "[INFO] Waiting for eurekaserver health..."
    Wait-ForHttpOk -Url "http://localhost:8761/actuator/health" -TimeoutSeconds 180
  }
}

Write-Host ""
Write-Host "[DONE] Startup commands sent."
Write-Host "[INFO] Selected ports:"
foreach ($service in $services) {
  Write-Host "  - $service -> $($selectedPorts[$service])"
}
Write-Host "[INFO] Eureka: http://localhost:8761"
Write-Host "[INFO] Gateway Swagger: http://localhost:8080/swagger-ui/index.html"
Write-Host "[INFO] To stop everything: powershell -ExecutionPolicy Bypass -File .\\scripts\\stop-all.ps1"
