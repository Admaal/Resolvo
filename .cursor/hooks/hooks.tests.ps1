# Comprueba que los hooks de Claude Code corren los gates del repo (o worktree) donde se trabaja,
# no los del checkout donde vive el hook. Cada caso usa un repo git temporal con un gates.ps1 falso.
$ErrorActionPreference = "Stop"
$script:failed = 0
$tempRoot = [System.IO.Path]::GetTempPath()
$shell = if ($PSVersionTable.PSEdition -eq "Desktop") { "powershell" } else { "pwsh" }

function New-RepoFalso([int]$codigo = 0) {
  $repo = Join-Path $tempRoot ("hooks-" + [guid]::NewGuid().ToString("n"))
  New-Item -ItemType Directory -Path (Join-Path $repo "scripts") -Force | Out-Null
  $previous = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  & git init -q $repo 2>$null | Out-Null
  $ErrorActionPreference = $previous
  # El gates.ps1 falso solo dice con que modo y rutas lo llamaron.
  $stub = 'param([string]$Mode, [Parameter(ValueFromRemainingArguments = $true)][string[]]$Route = @()) ' +
    ('Write-Output ("STUB " + $Mode + " " + ($Route -join " ")); exit ' + $codigo)
  [System.IO.File]::WriteAllText((Join-Path (Join-Path $repo "scripts") "gates.ps1"), $stub)
  return $repo
}

function Invoke-Hook([string]$hook, [string]$payload) {
  $script = Join-Path $PSScriptRoot $hook
  $output = $payload | & $shell -NoProfile -ExecutionPolicy Bypass -File $script -Cliente claude 2>&1 | Out-String
  return $output
}

function Invoke-Case([string]$name, [scriptblock]$body, [int]$codigo = 0) {
  $repo = $null
  try {
    $repo = New-RepoFalso $codigo
    & $body $repo
    Write-Output "OK  $name"
  } catch {
    Write-Output "FAIL $name - $($_.Exception.Message)"
    $script:failed++
  } finally {
    if ($repo -and (Test-Path $repo)) { cmd /c "rmdir /s /q `"$repo`"" | Out-Null }
  }
}

Invoke-Case "Test-PostToolUseCorreLosGatesDelRepoDelFicheroEscrito" {
  param($repo)
  $fichero = Join-Path $repo "backend\src\main\java\X.java"
  $payload = @{ tool_input = @{ file_path = $fichero } } | ConvertTo-Json -Compress
  $salida = Invoke-Hook "post-tool-use.ps1" $payload
  if ($salida -notmatch 'STUB fast backend/src/main/java/X\.java') { throw "no corrio los gates del repo del fichero. Salida: $salida" }
}

Invoke-Case "Test-StopCorreLosGatesDelDirectorioDeLaSesion" {
  param($repo)
  [System.IO.File]::WriteAllText((Join-Path $repo "cambio.txt"), "x")
  $payload = @{ cwd = $repo; stop_hook_active = $false } | ConvertTo-Json -Compress
  $salida = Invoke-Hook "stop.ps1" $payload
  # Con los gates en verde stop solo devuelve {}: el gates falso falla para que su salida llegue
  # en el motivo del bloqueo.
  if ($salida -notmatch 'STUB full') { throw "no corrio los gates del directorio de la sesion. Salida: $salida" }
} 1

if ($script:failed -gt 0) { exit 1 }
exit 0
