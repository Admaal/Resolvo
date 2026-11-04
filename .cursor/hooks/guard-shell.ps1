# Bloquea comandos que borran trabajo sin commitear.
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false

function Write-Allow {
  [Console]::Out.WriteLine('{"permission":"allow"}')
}

function Write-Deny([string]$why) {
  $obj = @{
    permission    = "deny"
    user_message  = "Comando bloqueado por la valla: $why"
    agent_message = "La valla ha bloqueado este comando ($why). No lo reintentes. Si hace falta de verdad, lo ejecuta la persona en su terminal."
  }
  [Console]::Out.WriteLine(($obj | ConvertTo-Json -Compress))
}

function Get-Invocation([string]$segment) {
  $s = $segment.Trim()
  $s = [regex]::Replace($s, '(?i)^(?:cmd(?:\.exe)?\s+/c\s+)+', '')
  return $s.Trim().Trim('"')
}

function Test-GitResetHard([string]$s) {
  return $s -match '(?i)^git\s+reset\b' -and $s -match '(?i)(^|\s)--hard(\s|$)'
}

function Test-GitCheckoutPath([string]$s) {
  return $s -match '(?i)^git\s+checkout\b' -and $s -match '(^|\s)--(\s|$)'
}

function Test-GitClean([string]$s) {
  if ($s -notmatch '(?i)^git\s+clean\b') { return $false }
  $hasF = $false
  $hasD = $false
  foreach ($t in ($s -split '\s+')) {
    if ($t -match '^-[A-Za-z]+$') {
      if ($t -match 'f') { $hasF = $true }
      if ($t -match 'd') { $hasD = $true }
    }
  }
  return ($hasF -and $hasD)
}

function Test-RmRf([string]$s) {
  if ($s -notmatch '(?i)^rm\s+') { return $false }
  $hasR = $false
  $hasF = $false
  foreach ($t in ($s -split '\s+')) {
    if ($t -match '(?i)^--recursive$') { $hasR = $true }
    if ($t -eq '--force') { $hasF = $true }
    if ($t -match '^-[A-Za-z]+$') {
      if ($t -match 'r') { $hasR = $true }
      if ($t -match 'f') { $hasF = $true }
    }
  }
  return ($hasR -and $hasF)
}

function Test-RemoveItemHitsRepo([string]$s, [string]$cwd) {
  if ($s -notmatch '(?i)^Remove-Item\b') { return $false }
  if ($s -notmatch '(?i)(^|\s)-Recurse\b') { return $false }
  $paths = @()
  foreach ($t in ($s -split '\s+')) {
    if ($t -match '(?i)^Remove-Item$') { continue }
    if ($t -match '^-') { continue }
    $paths += $t.Trim('"').Trim("'")
  }
  if ($paths.Count -eq 0) { return $true }
  $cwdFull = ""
  try { $cwdFull = [System.IO.Path]::GetFullPath($cwd).TrimEnd('\') } catch { return $true }
  foreach ($p in $paths) {
    if ($p -match '(?i)^\$env:(TEMP|TMP)\b') { continue }
    if ($p -match '(?i)^[A-Za-z]:\\Windows\\Temp\\') { continue }
    if ($p -match '(?i)^[A-Za-z]:\\Users\\[^\\]+\\AppData\\Local\\Temp\\') { continue }
    try {
      if ([System.IO.Path]::IsPathRooted($p)) {
        $full = [System.IO.Path]::GetFullPath($p)
      } else {
        $full = [System.IO.Path]::GetFullPath((Join-Path $cwd $p))
      }
      $full = $full.TrimEnd('\')
      if ($full.StartsWith($cwdFull, [System.StringComparison]::OrdinalIgnoreCase)) { return $true }
    } catch {
      return $true
    }
  }
  return $false
}

function Test-MainProtected([string]$cwd) {
  $root = & git -C $cwd rev-parse --show-toplevel 2>$null
  if ($LASTEXITCODE -ne 0 -or -not $root) { return $false }
  return Test-Path (Join-Path ([string]$root).Trim() ".cursor\proteger-main")
}

function Test-PushToMain([string]$s, [string]$cwd) {
  if ($s -notmatch '(?i)^git\s+push\b') { return $false }
  $tokens = @(($s -split '\s+') | Select-Object -Skip 2 | ForEach-Object { $_.Trim('"').Trim("'") })
  foreach ($t in $tokens) {
    if ($t -eq '--all' -or $t -eq '--mirror') { return $true }
  }
  $positional = @($tokens | Where-Object { $_ -and $_ -notmatch '^-' })
  $refspecs = @($positional | Select-Object -Skip 1)
  if ($refspecs.Count -eq 0) {
    $branch = & git -C $cwd rev-parse --abbrev-ref HEAD 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $branch) { return $true }
    $refspecs = @(([string]$branch).Trim())
  }
  foreach ($r in $refspecs) {
    $dest = $r.TrimStart('+')
    if ($dest.Contains(':')) { $dest = $dest.Substring($dest.LastIndexOf(':') + 1) }
    if ($dest -eq 'HEAD') {
      $branch = & git -C $cwd rev-parse --abbrev-ref HEAD 2>$null
      $dest = ([string]$branch).Trim()
    }
    $dest = $dest -replace '^refs/heads/', ''
    if ($dest -eq 'main' -or $dest -eq 'master') { return $true }
  }
  return $false
}

$inputJson = [Console]::In.ReadToEnd()
if ($inputJson -and $inputJson.Length -gt 0 -and $inputJson[0] -eq [char]0xFEFF) {
  $inputJson = $inputJson.Substring(1)
}

$command = ""
$cwd = (Get-Location).Path
try {
  if ($inputJson) {
    $payload = $inputJson | ConvertFrom-Json
    if ($payload.command) { $command = [string]$payload.command }
    if ($payload.cwd) { $cwd = [string]$payload.cwd }
  }
} catch {
  Write-Allow
  exit 0
}

if (-not $command) {
  Write-Allow
  exit 0
}

$segments = [regex]::Split($command, '\s*(?:&&|\|\||;|\|)\s*')
$why = ""
foreach ($seg in $segments) {
  $s = Get-Invocation $seg
  if (Test-GitResetHard $s) { $why = "git reset --hard"; break }
  if (Test-GitCheckoutPath $s) { $why = "git checkout -- <ruta>"; break }
  if (Test-GitClean $s) { $why = "git clean -fd"; break }
  if (Test-RmRf $s) { $why = "rm -rf"; break }
  if (Test-RemoveItemHitsRepo $s $cwd) { $why = "Remove-Item -Recurse dentro del repo"; break }
  if ((Test-PushToMain $s $cwd) -and (Test-MainProtected $cwd)) { $why = "git push directo a main; usa una rama y un PR con CI en verde"; break }
}

if ($why) { Write-Deny $why } else { Write-Allow }
exit 0
