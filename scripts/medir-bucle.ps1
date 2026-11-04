# Mide el bucle rápido (frío y caliente) y el gate completo, para anotar el presupuesto
# en AGENTS.md. Uso: powershell -NoProfile -File scripts/medir-bucle.ps1
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false

$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repo
$route = "backend/src/test/java/com/admaal/resolvo/architecture/DomainPurityTest.java"
$gates = Join-Path $repo "scripts\gates.ps1"
$gradlew = Join-Path $repo "backend\gradlew.bat"

function Invoke-Gate([string]$mode) {
  $sw = [Diagnostics.Stopwatch]::StartNew()
  $p = Start-Process -FilePath "powershell" `
    -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $gates, $mode, $route) `
    -NoNewWindow -PassThru
  $p.WaitForExit()
  $sw.Stop()
  return @{ Ms = $sw.ElapsedMilliseconds; Code = $p.ExitCode }
}

"=== FRIO ==="
$cold = @()
foreach ($i in 1..3) {
  $s = Start-Process -FilePath $gradlew -ArgumentList @("--stop") -NoNewWindow -PassThru
  $s.WaitForExit()
  Start-Sleep -Seconds 2
  $r = Invoke-Gate "fast"
  $cold += $r.Ms
  "cold $i : $($r.Ms) ms"
}

"=== CALIENTE ==="
$hot = @()
foreach ($i in 1..5) {
  $r = Invoke-Gate "fast"
  $hot += $r.Ms
  "hot $i : $($r.Ms) ms"
}

"=== COMPLETO CALIENTE ==="
$full = @()
foreach ($i in 1..3) {
  $sw = [Diagnostics.Stopwatch]::StartNew()
  $p = Start-Process -FilePath "powershell" `
    -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $gates, "full") `
    -NoNewWindow -PassThru
  $p.WaitForExit()
  $sw.Stop()
  $full += $sw.ElapsedMilliseconds
  "full $i : $($sw.ElapsedMilliseconds) ms"
}

function P95([int[]]$values) {
  $sorted = $values | Sort-Object
  return $sorted[[Math]::Ceiling(0.95 * $sorted.Count) - 1]
}

"frio: " + ($cold -join ", ")
"caliente: " + ($hot -join ", ")
"full: " + ($full -join ", ")
"p95 frio: $(P95 $cold) ms"
"p95 caliente: $(P95 $hot) ms"
"p95 full: $(P95 $full) ms"
