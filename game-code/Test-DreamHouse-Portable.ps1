param([Parameter(Mandatory=$true)][string]$ResultDirectory)
$ErrorActionPreference='Stop'
. "$PSScriptRoot\Resolve-DreamHouse.ps1"
. "$PSScriptRoot\Checked-Process.ps1"

$exe=Resolve-DreamHouseRelease -Root $PSScriptRoot
$resultRoot=New-Item -ItemType Directory -Force -Path $ResultDirectory
$accented='B'+[char]0x1EA3+'n n'+[char]0x1ED9+'p'
$fixture=Join-Path $resultRoot.FullName ("portable-"+[Guid]::NewGuid().ToString('N').Substring(0,8))
$installation=Join-Path $fixture "$accented DreamHouse"
$working=Join-Path $fixture 'Other working directory'
$player=Join-Path $fixture "$accented player profile"
New-Item -ItemType Directory -Force -Path $installation,$working | Out-Null
Get-ChildItem -LiteralPath (Split-Path $exe -Parent) -Force |
    Copy-Item -Destination $installation -Recurse -Force
$encoded=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($player)).TrimEnd('=').Replace('+','-').Replace('/','_')
$oldOptions=$env:JAVA_TOOL_OPTIONS
$oldJava=$env:JAVA_HOME
$oldPath=$env:PATH
try {
    $env:JAVA_TOOL_OPTIONS='-Ddreamhouse.launcherProbe=true -Ddreamhouse.probe=true'
    $env:JAVA_HOME=''
    $env:PATH="$env:SystemRoot\System32;$env:SystemRoot"
    $javaNotOnPath=!(Get-Command java.exe -ErrorAction SilentlyContinue)
    $result=Invoke-CheckedProcess -FilePath (Join-Path $installation 'DreamHouse.exe') -Arguments @("--player-root64=$encoded") `
        -WorkingDirectory $working -Stdout "$fixture\launcher.log" -Stderr "$fixture\launcher.err.log" -TimeoutMs 90000
} finally {
    $env:JAVA_TOOL_OPTIONS=$oldOptions
    $env:JAVA_HOME=$oldJava
    $env:PATH=$oldPath
}
$launcherLog=Get-Content -LiteralPath "$fixture\launcher.log" -Raw
$gameLog=Get-ChildItem -LiteralPath "$player\logs" -Filter 'launcher-*.log' -ErrorAction SilentlyContinue | Select-Object -First 1
$gameReady=$null -ne $gameLog -and (Get-Content -LiteralPath $gameLog.FullName -Raw) -match 'PACKAGED_READY objects=43 '
$launcherReady=$launcherLog -match 'LAUNCHER_READY .*childExit=0'
$passed=$result.ExitCode -eq 0 -and !$result.Timeout -and $javaNotOnPath -and $launcherReady -and $gameReady
$summary=[ordered]@{
    passed=$passed
    exitCode=$result.ExitCode
    timeout=$result.Timeout
    seconds=$result.Seconds
    javaNotOnPath=$javaNotOnPath
    launcherReady=$launcherReady
    gameReady=$gameReady
    sourceExe=$exe
    installation=$installation
    player=$player
} | ConvertTo-Json
$summary | Set-Content -LiteralPath "$fixture\result.json" -Encoding UTF8
Write-Output $summary
if (!$passed) { throw "Portable handoff failed. See $fixture\result.json and launcher logs." }
