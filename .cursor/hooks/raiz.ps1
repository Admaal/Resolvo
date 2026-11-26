# Raiz del repo (o worktree) donde se trabaja. Los hooks viven en el checkout que abrio el
# cliente, pero en un worktree el fichero escrito y la sesion estan en otra raiz: sin esto el
# hook correria los gates del checkout equivocado y no veria los cambios.
function Get-RaizDeTrabajo([string]$ruta, [string]$porDefecto) {
  if (-not $ruta) { return $porDefecto }
  try {
    if ($ruta -match '^file://') { $ruta = ([Uri]$ruta).LocalPath }
    $dir = [System.IO.Path]::GetFullPath($ruta)
  } catch {
    return $porDefecto
  }
  # El fichero puede no existir aun (o ser una ruta nueva): se sube hasta una carpeta que exista.
  while ($dir -and -not (Test-Path -LiteralPath $dir -PathType Container)) {
    $dir = Split-Path $dir -Parent
  }
  if (-not $dir) { return $porDefecto }
  $previo = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  $top = & git -C $dir rev-parse --show-toplevel 2>$null
  $codigo = $LASTEXITCODE
  $ErrorActionPreference = $previo
  if ($codigo -ne 0 -or -not $top) { return $porDefecto }
  $top = ([string]$top).Trim().Replace("/", [string][System.IO.Path]::DirectorySeparatorChar)
  if (Test-Path -LiteralPath (Join-Path (Join-Path $top "scripts") "gates.ps1")) { return $top }
  return $porDefecto
}
