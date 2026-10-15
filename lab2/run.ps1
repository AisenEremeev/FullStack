param([ValidateSet('run', 'test', 'package')][string]$Action = 'run')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$localJdk = Get-ChildItem "$PSScriptRoot/.tools" -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
$localMaven = Get-ChildItem "$PSScriptRoot/.tools" -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue | Select-Object -First 1
$maven = if ($localMaven) { Join-Path $localMaven.FullName 'bin/mvn.cmd' } else { 'mvn' }
$goal = switch ($Action) { 'run' { 'spring-boot:run' } 'test' { 'test' } 'package' { 'package' } }
& $maven "-Dmaven.repo.local=$PSScriptRoot/.tools/repository" $goal
exit $LASTEXITCODE
