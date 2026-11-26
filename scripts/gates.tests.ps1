# Comprueba el diff contra la base y la mutación incremental/completa de gates.ps1.
# Cada caso corre una copia parcheada en un repo git temporal.
$ErrorActionPreference = "Stop"
Remove-Item Env:GITHUB_ACTIONS -ErrorAction SilentlyContinue
Remove-Item Env:GITHUB_BASE_SHA -ErrorAction SilentlyContinue

$script:failed = 0
# Copia literal del bloque de config de skills/valla/plantilla/scripts/gates.ps1.
$script:templateConfig = @'
$LintCommand = "TODO-LINT"
$FastTestCommand = "TODO-TEST"
$FastRoutes = @(
  @{ Prefix = "TODO-DOMAIN-PREFIX"; Command = "TODO-TEST-DOMAIN" },
  @{ Prefix = "TODO-APP-PREFIX"; Command = "TODO-TEST-APP" }
)
$FullCommands = @(
  "TODO-FULL"
)
$MutationCommand = "TODO-MUTATION"
# stryker | pit
$MutationTool = "TODO-MUTATION-TOOL"
# Prefijo git que Stryker no ve. En pit puede quedar vacio.
$MutationRoot = "TODO-MUTATION-ROOT"
# Solo pit: argumento que `full` anade al comando para pedir el historial
# incremental (p. ej. "-PpitIncremental=true" en Gradle). El plugin de Gradle
# ignora -PwithHistory=true: el build debe leer esa propiedad y solo entonces
# llamar a withHistory.set(true) dentro del bloque pitest. CI no la pasa.
# En stryker puede quedar vacio.
$PitIncrementalArg = "TODO-PIT-INCREMENTAL-ARG"
$DomainPaths = @("TODO-DOMAIN")
'@
$source = Join-Path $PSScriptRoot "gates.ps1"
$onWindows = ($PSVersionTable.PSEdition -eq "Desktop") -or [bool]$IsWindows
$tempRoot = [System.IO.Path]::GetTempPath()
$okCmd = if ($onWindows) { "cmd /c exit 0" } else { "true" }

function Remove-Tree([string]$path) {
  if ($path -and (Test-Path $path)) {
    if ($onWindows) {
      cmd /c "rmdir /s /q `"$path`"" | Out-Null
    } else {
      Remove-Item -LiteralPath $path -Recurse -Force -ErrorAction SilentlyContinue
    }
  }
}

function Git-Quiet([string]$repo, [string[]]$gitArgs) {
  $previous = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  & git -C $repo @gitArgs 2>$null | Out-Null
  $code = $LASTEXITCODE
  $ErrorActionPreference = $previous
  if ($code -ne 0) {
    throw "git -C $repo $($gitArgs -join ' ') salio $code"
  }
}

function New-Fixture([string]$tool, [string]$root, [string]$mutation = "") {
  $id = [guid]::NewGuid().ToString("n")
  $repo = Join-Path $tempRoot "gates-$id"
  $origin = Join-Path $tempRoot "origin-$id"
  $previous = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  & git init -b main $repo 2>$null | Out-Null
  $initCode = $LASTEXITCODE
  if ($initCode -ne 0) {
    New-Item -ItemType Directory -Path $repo -Force | Out-Null
    & git -C $repo init 2>$null | Out-Null
    $initCode = $LASTEXITCODE
    if ($initCode -eq 0) {
      & git -C $repo checkout -b main 2>$null | Out-Null
      $initCode = $LASTEXITCODE
    }
  }
  $ErrorActionPreference = $previous
  if ($initCode -ne 0) { throw "git init salio $initCode" }
  [System.IO.File]::WriteAllText((Join-Path $repo "README.md"), "base`n")
  Git-Quiet $repo @("add", "--", "README.md")
  Git-Quiet $repo @("-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", "base")
  $previousBare = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  & git init --bare $origin 2>$null | Out-Null
  $bareCode = $LASTEXITCODE
  $ErrorActionPreference = $previousBare
  if ($bareCode -ne 0) { throw "git init --bare salio $bareCode" }
  Git-Quiet $repo @("remote", "add", "origin", $origin)
  Git-Quiet $repo @("push", "origin", "main")

  if ($onWindows) {
    $recorder = Join-Path $repo "record.cmd"
    [System.IO.File]::WriteAllText($recorder, "@echo off`r`nexit /b 0`r`n")
  } else {
    $recorder = Join-Path $repo "record.sh"
    [System.IO.File]::WriteAllText($recorder, "#!/usr/bin/env bash`nexit 0`n")
    & chmod +x $recorder
  }
  $text = [System.IO.File]::ReadAllText($source)
  # Proyecto: el bloque de config real se cambia por el de la plantilla, con sus
  # marcadores TODO, para que los casos no lancen los comandos reales del stack.
  $begin = $text.IndexOf("# >>> config del proyecto")
  $endMark = "# <<< config del proyecto"
  $end = $text.IndexOf($endMark)
  if ($begin -ge 0 -and $end -gt $begin) {
    $text = $text.Substring(0, $begin) + $script:templateConfig + $text.Substring($end + $endMark.Length)
  }
  $text = $text.Replace('$MutationTool = "TODO-MUTATION-TOOL"', "`$MutationTool = `"$tool`"")
  $text = $text.Replace('$MutationRoot = "TODO-MUTATION-ROOT"', "`$MutationRoot = `"$root`"")
  if (-not $mutation) { $mutation = $recorder }
  $text = $text.Replace('"TODO-PIT-INCREMENTAL-ARG"', '"-PpitIncremental=true"')
  $text = $text.Replace('"TODO-MUTATION"', ('"' + $mutation + '"'))
  $text = $text.Replace('"TODO-LINT"', ('"' + $okCmd + '"'))
  $text = $text.Replace('"TODO-TEST-DOMAIN"', ('"' + $okCmd + '"'))
  $text = $text.Replace('"TODO-TEST-APP"', ('"' + $okCmd + '"'))
  $text = $text.Replace('"TODO-TEST"', ('"' + $okCmd + '"'))
  $text = $text.Replace('"TODO-FULL"', ('"' + $okCmd + '"'))
  $text = $text.Replace('"TODO-DOMAIN-PREFIX"', '"src/domain/"')
  $text = $text.Replace('"TODO-APP-PREFIX"', '"src/app/"')
  $text = $text.Replace('@("TODO-DOMAIN")', '@("src/domain/")')
  $scripts = Join-Path $repo "scripts"
  New-Item -ItemType Directory -Path $scripts -Force | Out-Null
  [System.IO.File]::WriteAllText((Join-Path $scripts "gates.ps1"), $text)
  # npx y pnpm falsos: el test no descarga ni ejecuta Stryker.
  $bin = Join-Path $repo ".stub-bin"
  New-Item -ItemType Directory -Path $bin -Force | Out-Null
  foreach ($tool in @("npx", "pnpm")) {
    if ($onWindows) {
      [System.IO.File]::WriteAllText((Join-Path $bin "$tool.cmd"), "@echo off`r`nexit /b 0`r`n")
    } else {
      $stub = Join-Path $bin $tool
      [System.IO.File]::WriteAllText($stub, "#!/usr/bin/env bash`nexit 0`n")
      & chmod +x $stub
    }
  }
  [System.IO.File]::WriteAllText((Join-Path $repo ".gitignore"), ".stub-bin/`n.cursor/`nrecord.*`n")
  return @{ Repo = $repo; Origin = $origin; Recorder = $recorder; Bin = $bin }
}

function Add-File([string]$repo, [string]$relative, [string]$content) {
  $path = Join-Path $repo ($relative.Replace("/", [string][System.IO.Path]::DirectorySeparatorChar))
  $dir = Split-Path $path
  if (-not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
  }
  [System.IO.File]::WriteAllText($path, $content)
  Git-Quiet $repo @("add", "--", $relative)
}

function Commit-Msg([string]$repo, [string]$msg) {
  Git-Quiet $repo @("-c", "user.email=test@example.com", "-c", "user.name=Test", "commit", "-m", $msg)
}

function Run-Gates($fx, [string]$mode, [string[]]$routes = @()) {
  $gates = Join-Path (Join-Path $fx.Repo "scripts") "gates.ps1"
  $shell = if ($PSVersionTable.PSEdition -eq "Desktop") { "powershell" } else { "pwsh" }
  $previousPath = $env:PATH
  $env:PATH = $fx.Bin + [System.IO.Path]::PathSeparator + $env:PATH
  try {
    $output = & $shell -NoProfile -File $gates $mode @routes 2>&1 | Out-String
  } finally {
    $env:PATH = $previousPath
  }
  return @{ Output = $output; Code = $LASTEXITCODE }
}

function Get-MutationCmd([string]$output) {
  $match = [regex]::Match($output, '(?m)^CMD mutation: (.+)$')
  if (-not $match.Success) { return $null }
  return $match.Groups[1].Value.Trim()
}

function Invoke-Case([string]$name, [scriptblock]$body) {
  $fx = $null
  try {
    & $body ([ref]$fx)
    Write-Output "OK  $name"
  } catch {
    Write-Output "FAIL $name — $($_.Exception.Message)"
    $script:failed++
  } finally {
    if ($fx -and $fx.Value) {
      Remove-Tree $fx.Value.Repo
      Remove-Tree $fx.Value.Origin
    }
  }
}

Invoke-Case "Test-DomainChangedIncludesCommitsAgainstOriginMain" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $repo = $fxRef.Value.Repo
  Git-Quiet $repo @("checkout", "-b", "feature")
  Add-File $repo "src/domain/Foo.ts" "export const n = 1`n"
  Commit-Msg $repo "dominio"
  $tracked = @(& git -C $repo diff --name-only) + @(& git -C $repo diff --name-only --cached)
  $tracked = @($tracked | Where-Object { $_ })
  if ($tracked.Count -gt 0) { throw "el arbol no quedo limpio: $($tracked -join ', ')" }
  $run = Run-Gates $fxRef.Value "full"
  if ($run.Output -match 'dominio no cambio') {
    throw "omitio la mutacion con un commit de dominio sobre origin/main"
  }
  $cmd = Get-MutationCmd $run.Output
  if (-not $cmd) { throw "no escribio CMD mutation. Salida: $($run.Output)" }
  if ($cmd -notmatch '--incremental' -or $cmd -notmatch 'Foo\.ts') {
    throw "comando inesperado: $cmd"
  }
}

Invoke-Case "Test-UncommittedDomainChangeStillRunsMutation" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $repo = $fxRef.Value.Repo
  $path = [System.IO.Path]::Combine($repo, "src", "domain", "Bar.ts")
  New-Item -ItemType Directory -Path (Split-Path $path) -Force | Out-Null
  [System.IO.File]::WriteAllText($path, "export const n = 2`n")
  $run = Run-Gates $fxRef.Value "full"
  $cmd = Get-MutationCmd $run.Output
  if (-not $cmd) { throw "no escribio CMD mutation. Salida: $($run.Output)" }
  if ($cmd -notmatch '--incremental' -or $cmd -notmatch 'Bar\.ts') {
    throw "comando inesperado: $cmd"
  }
}

Invoke-Case "Test-FullRunsStrykerIncrementalOnChangedDomainFiles" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $repo = $fxRef.Value.Repo
  Git-Quiet $repo @("checkout", "-b", "feature")
  Add-File $repo "src/domain/Foo.ts" "export const n = 1`n"
  Add-File $repo "src/domain/Foo.test.ts" "test`n"
  Add-File $repo "notes.md" "nota`n"
  Commit-Msg $repo "diff"
  $run = Run-Gates $fxRef.Value "full"
  $cmd = Get-MutationCmd $run.Output
  if (-not $cmd) { throw "no escribio CMD mutation. Salida: $($run.Output)" }
  if ($cmd -notmatch '--incremental') { throw "falta --incremental: $cmd" }
  if ($cmd -notmatch '--mutate\s+"Foo\.ts"') { throw "el mutate no es solo Foo.ts: $cmd" }
  if ($cmd -match 'Foo\.test\.ts' -or $cmd -match 'notes\.md') {
    throw "el mutate incluye ficheros que no son produccion: $cmd"
  }
}

Invoke-Case "Test-TestOnlyDiffKeepsIncrementalWithoutMutate" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $repo = $fxRef.Value.Repo
  Git-Quiet $repo @("checkout", "-b", "feature")
  Add-File $repo "src/domain/Foo.test.ts" "test`n"
  Commit-Msg $repo "solo test"
  $run = Run-Gates $fxRef.Value "full"
  $cmd = Get-MutationCmd $run.Output
  if (-not $cmd) { throw "no escribio CMD mutation. Salida: $($run.Output)" }
  if ($cmd -notmatch '--incremental') { throw "falta --incremental: $cmd" }
  if ($cmd -match '--mutate') { throw "limito el mutate a un test: $cmd" }
}

Invoke-Case "Test-CiRunsCompleteMutationWithoutIncrementalFlags" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $repo = $fxRef.Value.Repo
  Git-Quiet $repo @("checkout", "-b", "feature")
  Add-File $repo "src/domain/Foo.ts" "export const n = 1`n"
  Commit-Msg $repo "dominio"
  $run = Run-Gates $fxRef.Value "ci"
  if ($run.Code -ne 0) { throw "ci salio $($run.Code). Salida: $($run.Output)" }
  $cmd = Get-MutationCmd $run.Output
  if ($cmd -ne $fxRef.Value.Recorder) { throw "ci no uso el comando completo: $cmd" }
  if ($cmd -match '--incremental' -or $cmd -match '--mutate' -or $cmd -match 'withHistory') {
    throw "ci anadio flags incrementales: $cmd"
  }
}

Invoke-Case "Test-FullRunsPitWithHistory" {
  param($fxRef)
  $fxRef.Value = New-Fixture "pit" ""
  $repo = $fxRef.Value.Repo
  Git-Quiet $repo @("checkout", "-b", "feature")
  Add-File $repo "src/domain/Foo.ts" "export const n = 1`n"
  Commit-Msg $repo "dominio"
  $full = Run-Gates $fxRef.Value "full"
  $fullCmd = Get-MutationCmd $full.Output
  if (-not $fullCmd -or $fullCmd -notmatch '-PpitIncremental=true') {
    throw "full no activo el historial: $fullCmd"
  }
  if ($fullCmd -match '--incremental' -or $fullCmd -match '--mutate') {
    throw "pit recibio flags de stryker: $fullCmd"
  }
  $ci = Run-Gates $fxRef.Value "ci"
  $ciCmd = Get-MutationCmd $ci.Output
  if ($ciCmd -ne $fxRef.Value.Recorder) { throw "ci no uso el comando completo: $ciCmd" }
  if ($ciCmd -match 'pitIncremental' -or $ciCmd -match 'withHistory') { throw "ci llevo el historial: $ciCmd" }
}

Invoke-Case "Test-FullRecordsHeadSha" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $repo = $fxRef.Value.Repo
  Git-Quiet $repo @("checkout", "-b", "feature")
  Add-File $repo "src/domain/Foo.ts" "export const n = 1`n"
  Commit-Msg $repo "dominio"
  $sha = (& git -C $repo rev-parse HEAD).Trim()
  $run = Run-Gates $fxRef.Value "full"
  if ($run.Code -ne 0) { throw "full salio $($run.Code). Salida: $($run.Output)" }
  $logPath = [System.IO.Path]::Combine($repo, ".cursor", "hooks", "gate-timings.log")
  if (-not (Test-Path $logPath)) { throw "no escribio gate-timings.log" }
  $lines = @(Get-Content -Path $logPath)
  $result = @($lines | Where-Object { $_ -match [regex]::Escape($sha) -and $_ -match '\bRESULT\b' -and $_ -match 'exit=0' })
  if ($result.Count -lt 1) { throw "sin RESULT del HEAD. Log: $($lines -join ' | ')" }
  foreach ($line in $lines) {
    if ($line -notmatch [regex]::Escape($sha)) { throw "linea sin sha: $line" }
  }
}

Invoke-Case "Test-CiRunsCompleteMutationWithoutDomainChanges" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $repo = $fxRef.Value.Repo
  Git-Quiet $repo @("checkout", "-b", "feature")
  Add-File $repo "notes.md" "nota`n"
  Commit-Msg $repo "sin dominio"
  $run = Run-Gates $fxRef.Value "ci"
  if ($run.Code -ne 0) { throw "ci salio $($run.Code). Salida: $($run.Output)" }
  if ($run.Output -match 'dominio no cambio') { throw "ci omitio la mutacion" }
  $cmd = Get-MutationCmd $run.Output
  if ($cmd -ne $fxRef.Value.Recorder) { throw "ci no corrio la mutacion completa: $cmd" }
}

Invoke-Case "Test-NpxAndPnpmOmitDoubleDashNpmKeepsIt" {
  param($fxRef)
  foreach ($case in @(@{ Cmd = "npx stryker run"; Sep = $false }, @{ Cmd = "pnpm stryker"; Sep = $false }, @{ Cmd = "npm run mutation"; Sep = $true })) {
    $fxRef.Value = New-Fixture "stryker" "src/domain/" $case.Cmd
    $repo = $fxRef.Value.Repo
    Git-Quiet $repo @("checkout", "-b", "feature")
    Add-File $repo "src/domain/Foo.ts" "export const n = 1`n"
    Commit-Msg $repo "dominio"
    $run = Run-Gates $fxRef.Value "full"
    $cmd = Get-MutationCmd $run.Output
    $expected = if ($case.Sep) { "$($case.Cmd) -- --incremental" } else { "$($case.Cmd) --incremental" }
    if (-not $cmd -or -not $cmd.StartsWith($expected)) { throw "con '$($case.Cmd)' el comando fue: $cmd" }
    Remove-Tree $fxRef.Value.Repo
    Remove-Tree $fxRef.Value.Origin
  }
}

Invoke-Case "Test-MutateExcludesDeletedAndTestFiles" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $repo = $fxRef.Value.Repo
  Add-File $repo "src/domain/Gone.ts" "export const g = 0`n"
  Commit-Msg $repo "base con Gone"
  Git-Quiet $repo @("push", "origin", "main")
  Git-Quiet $repo @("checkout", "-b", "feature")
  Git-Quiet $repo @("rm", "-q", "--", "src/domain/Gone.ts")
  Add-File $repo "src/domain/Keep.ts" "export const k = 1`n"
  Add-File $repo "src/domain/View.test.tsx" "test`n"
  Add-File $repo "src/domain/util.spec.js" "test`n"
  Add-File $repo "src/domain/__tests__/helper.ts" "test`n"
  Commit-Msg $repo "mezcla"
  $run = Run-Gates $fxRef.Value "full"
  $cmd = Get-MutationCmd $run.Output
  if (-not $cmd) { throw "no escribio CMD mutation. Salida: $($run.Output)" }
  if ($cmd -notmatch '--mutate\s+"Keep\.ts"$') { throw "el mutate no es solo Keep.ts: $cmd" }
}

function Get-TestGates([string]$output) {
  return @([regex]::Matches($output, '(?m)^<< (test[^:]*):') | ForEach-Object { $_.Groups[1].Value })
}

Invoke-Case "Test-FastGroupsRoutesByPrefix" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $run = Run-Gates $fxRef.Value "fast" @("src/domain/A.ts", "src/domain/B.ts", "src/app/C.ts")
  $gates = Get-TestGates $run.Output
  if (($gates -join ",") -ne "test[src/domain/],test[src/app/]") { throw "pasadas inesperadas: $($gates -join ','). Salida: $($run.Output)" }
}

Invoke-Case "Test-FastAnotaRedCuandoFallaUnFicheroDeTest" {
  param($fxRef)
  $fxRef.Value = New-Fixture "pit" ""
  $repo = $fxRef.Value.Repo
  $gatesPath = Join-Path (Join-Path $repo "scripts") "gates.ps1"
  $ok = if ($onWindows) { "cmd /c exit 0" } else { "true" }
  $fail = if ($onWindows) { "cmd /c exit 7" } else { "sh -c 'exit 7'" }
  $text = [System.IO.File]::ReadAllText($gatesPath)
  $needle = '$FastTestCommand = "' + $ok + '"'
  if (-not $text.Contains($needle)) { throw "no encontre FastTestCommand" }
  [System.IO.File]::WriteAllText($gatesPath, $text.Replace($needle, '$FastTestCommand = "' + $fail + '"'))
  $run = Run-Gates $fxRef.Value "fast" @("notes/FooTest.java", "README.md")
  if ($run.Code -eq 0) { throw "el gate debio fallar. Salida: $($run.Output)" }
  $red = Join-Path (Join-Path $repo ".cursor") "red.log"
  if (-not (Test-Path $red)) { throw "no escribio red.log. Salida: $($run.Output)" }
  $line = Get-Content -Path $red -Raw
  if ($line -notmatch 'RED notes/FooTest.java exit=7') { throw "linea inesperada: $line" }
  if ($line -match 'README.md') { throw "anoto un fichero que no es un test: $line" }
}

Invoke-Case "Test-FastRunsDefaultOnlyForUnmatchedRoute" {
  param($fxRef)
  $fxRef.Value = New-Fixture "stryker" "src/domain/"
  $run = Run-Gates $fxRef.Value "fast" @("src/domain/A.ts", "README.md")
  $gates = Get-TestGates $run.Output
  if (($gates -join ",") -ne "test[src/domain/],test") { throw "pasadas inesperadas: $($gates -join ',')" }
  $none = Get-TestGates (Run-Gates $fxRef.Value "fast").Output
  if (($none -join ",") -ne "test") { throw "sin rutas debe correr solo la suite por defecto: $($none -join ',')" }
}

function Set-RedCommand($fx, [string]$command) {
  # La plantilla de la valla no trae RedCommand: el caso lo anade tras el bloque de config.
  $gatesPath = Join-Path (Join-Path $fx.Repo "scripts") "gates.ps1"
  $text = [System.IO.File]::ReadAllText($gatesPath)
  $needle = '$DomainPaths = @("src/domain/")'
  if (-not $text.Contains($needle)) { throw "no encontre DomainPaths" }
  $escaped = $command.Replace('"', '`"')
  [System.IO.File]::WriteAllText($gatesPath, $text.Replace($needle, $needle + "`n`$RedCommand = `"$escaped`""))
}

Invoke-Case "Test-RedAnotaLaLineaConModoRedSiElTestFalla" {
  param($fxRef)
  $fxRef.Value = New-Fixture "pit" ""
  $fail = if ($onWindows) { "cmd /c exit 1" } else { "sh -c 'exit 1'" }
  Set-RedCommand $fxRef.Value $fail
  $run = Run-Gates $fxRef.Value "red" @("backend/src/test/java/com/x/FooTest.java")
  if ($run.Code -ne 0) { throw "un RED confirmado debe salir 0. Salida: $($run.Output)" }
  $red = Join-Path (Join-Path $fxRef.Value.Repo ".cursor") "red.log"
  if (-not (Test-Path $red)) { throw "no escribio red.log. Salida: $($run.Output)" }
  $line = Get-Content -Path $red -Raw
  if ($line -notmatch ' red \S+ RED backend/src/test/java/com/x/FooTest.java exit=1') { throw "linea inesperada: $line" }
}

Invoke-Case "Test-RedNoAnotaNadaSiElTestPasa" {
  param($fxRef)
  $fxRef.Value = New-Fixture "pit" ""
  $ok = if ($onWindows) { "cmd /c exit 0" } else { "true" }
  Set-RedCommand $fxRef.Value $ok
  $run = Run-Gates $fxRef.Value "red" @("backend/src/test/java/com/x/FooTest.java")
  if ($run.Code -eq 0) { throw "un test en verde no es un RED: debe salir 1" }
  $red = Join-Path (Join-Path $fxRef.Value.Repo ".cursor") "red.log"
  if ((Test-Path $red) -and ((Get-Content -Path $red -Raw) -match 'FooTest')) { throw "anoto un RED que no existe" }
}

Invoke-Case "Test-RedPasaLaClaseJavaAlComando" {
  param($fxRef)
  $fxRef.Value = New-Fixture "pit" ""
  $echo = if ($onWindows) { "echo CLASE={clase} & exit /b 1" } else { "echo CLASE={clase}; exit 1" }
  Set-RedCommand $fxRef.Value $echo
  $run = Run-Gates $fxRef.Value "red" @("backend/src/test/java/com/x/FooTest.java")
  if ($run.Output -notmatch 'CLASE=com\.x\.FooTest') { throw "no paso la clase. Salida: $($run.Output)" }
}

if ($script:failed -gt 0) { exit 1 }
exit 0
