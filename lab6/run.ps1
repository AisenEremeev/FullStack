param([ValidateSet('install', 'dev', 'build', 'preview', 'check')][string]$Action = 'dev')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$toolsDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) '.tools'
$localNode = Get-ChildItem $toolsDirectory -Directory -Filter 'node-*-win-x64' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($localNode) { $env:PATH = "$($localNode.FullName);$env:PATH" }
$npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
if (-not $npm) { throw 'Install Node.js 24 LTS with npm, then reopen the terminal.' }
if ($Action -eq 'install' -or -not (Test-Path 'node_modules')) {
    & $npm.Source ci --cache "$toolsDirectory/npm-cache"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
if ($Action -ne 'install') { & $npm.Source run $Action }
exit $LASTEXITCODE
