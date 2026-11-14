# stop: gauntlet solo si hay cambios. Sin cambios o aborted, sale al momento.
# -Cliente cursor (por defecto) o claude: cambia entrada, salida y como se limita el bucle.
param([string]$Cliente = "cursor")
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false

# Claude no tiene loop_limit: se cuenta aqui. A la 3.a vez seguida con fallo se deja parar.
$maxVueltas = 3
$countPath = Join-Path $PSScriptRoot "stop.count"

function Write-Empty {
  [Console]::Out.WriteLine("{}")
}

function Reset-Count {
  if (Test-Path $countPath) { Remove-Item $countPath -Force -ErrorAction SilentlyContinue }
}

$inputJson = [Console]::In.ReadToEnd()
if ($inputJson -and $inputJson.Length -gt 0 -and $inputJson[0] -eq [char]0xFEFF) {
  $inputJson = $inputJson.Substring(1)
}

$status = "completed"
$stopActive = $false
try {
  if ($inputJson) {
    $payload = $inputJson | ConvertFrom-Json
    if ($payload.PSObject.Properties.Name -contains "status" -and $payload.status) {
      $status = [string]$payload.status
    }
    if ($payload.PSObject.Properties.Name -contains "stop_hook_active" -and $payload.stop_hook_active) {
      $stopActive = $true
    }
  }
} catch {
  $status = "completed"
}

if ($status -eq "aborted") {
  Write-Empty
  exit 0
}

if ($Cliente -eq "claude" -and -not $stopActive) { Reset-Count }

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $repoRoot

$porcelain = & git status --porcelain 2>$null
if ($LASTEXITCODE -ne 0 -or -not $porcelain) {
  if ($Cliente -eq "claude") { Reset-Count }
  Write-Empty
  exit 0
}

if ($Cliente -eq "claude" -and $stopActive) {
  $vueltas = 0
  if (Test-Path $countPath) { [int]::TryParse((Get-Content $countPath -Raw).Trim(), [ref]$vueltas) | Out-Null }
  if ($vueltas -ge $maxVueltas) {
    Reset-Count
    Write-Empty
    exit 0
  }
}

$lockPath = Join-Path $PSScriptRoot "gauntlet.lock"
$lockStream = $null
try {
  $lockStream = [System.IO.File]::Open(
    $lockPath,
    [System.IO.FileMode]::OpenOrCreate,
    [System.IO.FileAccess]::Write,
    [System.IO.FileShare]::None
  )
} catch {
  [Console]::Error.WriteLine("Gauntlet ya en curso; se omite este stop.")
  Write-Empty
  exit 0
}

try {
  $gates = Join-Path (Join-Path $repoRoot "scripts") "gates.ps1"
  $out = & $gates full 2>&1 | Out-String
  $code = $LASTEXITCODE
  if ($code -ne 0 -or $out -match '(?m)^FALLO ') {
    $text = $out.Trim()
    if ($text.Length -gt 8000) {
      $text = $text.Substring($text.Length - 8000)
    }
    $msg = "La gauntlet ha fallado. Corrige antes de dar la tarea por cerrada.`n$text"
    if ($Cliente -eq "claude") {
      $v = 0
      if (Test-Path $countPath) { [int]::TryParse((Get-Content $countPath -Raw).Trim(), [ref]$v) | Out-Null }
      Set-Content -Path $countPath -Value ($v + 1) -Encoding ascii
      $json = @{ decision = "block"; reason = $msg } | ConvertTo-Json -Compress
    } else {
      $json = @{ followup_message = $msg } | ConvertTo-Json -Compress
    }
    [Console]::Out.WriteLine($json)
  } else {
    if ($Cliente -eq "claude") { Reset-Count }
    Write-Empty
  }
} finally {
  if ($lockStream) {
    $lockStream.Close()
    $lockStream.Dispose()
  }
}
exit 0
