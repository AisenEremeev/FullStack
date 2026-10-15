param([ValidateSet('run', 'test', 'package')][string]$Action = 'run')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$toolRoot = Join-Path $PSScriptRoot '.tools'
if (-not (Test-Path $toolRoot)) { $toolRoot = Join-Path (Split-Path $PSScriptRoot -Parent) 'lab2/.tools' }
$jdk = Get-ChildItem $toolRoot -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
$mavenFolder = Get-ChildItem $toolRoot -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue | Select-Object -First 1
$maven = if ($mavenFolder) { Join-Path $mavenFolder.FullName 'bin/mvn.cmd' } else { 'mvn' }
$goal = switch ($Action) { 'run' { 'spring-boot:run' } 'test' { 'test' } 'package' { 'package' } }
$mavenArgs = @('-B', '-ntp')
if (Test-Path $toolRoot) { $mavenArgs += "-Dmaven.repo.local=$toolRoot/repository" }
& $maven @mavenArgs $goal
exit $LASTEXITCODE
