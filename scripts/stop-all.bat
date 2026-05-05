@echo off
setlocal

set "ROOT_DIR=%~dp0.."

echo [INFO] SarajevoTransit one-command shutdown

echo [INFO] Stopping OTP container...
docker compose -f "%ROOT_DIR%\docker-compose.otp.yml" down

echo [INFO] Stopping Java processes for Eureka, OTP proxy, and routing...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$targets = @();" ^
  "foreach ($p in Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' }) {" ^
  "  $cmd = [string]$p.CommandLine;" ^
  "  if ($cmd -match 'userservice-eureka-server' -or $cmd -match 'otpproxyservice' -or $cmd -match 'routingservice') { $targets += $p }" ^
  "}" ^
  "foreach ($t in $targets) { Stop-Process -Id $t.ProcessId -Force -ErrorAction SilentlyContinue; Write-Output ('Stopped PID ' + $t.ProcessId) }"

echo [INFO] Done.
echo [INFO] You can close any remaining service terminal windows.

endlocal
