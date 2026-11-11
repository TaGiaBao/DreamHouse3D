param([Parameter(Mandatory=$true)][string]$ResultDirectory)
$ErrorActionPreference='Stop'
. "$PSScriptRoot\Resolve-DreamHouse.ps1"
$fixture=Join-Path $ResultDirectory 'launcher path co dau'
$release=Join-Path $fixture 'release'
$relative='build-20990101-010101-001\bundled\DreamHouse\DreamHouse.exe'
$exe=Join-Path $release $relative
$folder=Split-Path $exe -Parent
New-Item -ItemType Directory -Force -Path "$folder\app","$folder\runtime\bin\server" | Out-Null
[IO.File]::WriteAllText($exe,'fixture only - never execute')
[IO.File]::WriteAllText("$folder\app\DreamHouse.jar",'jar fixture')
[IO.File]::WriteAllText("$folder\runtime\bin\server\jvm.dll",'runtime fixture')
$metadata=@{executable=$relative;jarSha256=(Get-DreamHouseFileHash -Path "$folder\app\DreamHouse.jar")}
function Save-Manifest { $metadata | ConvertTo-Json | Set-Content -LiteralPath "$release\current.json" -Encoding UTF8 }
function Reject-Release([string]$label) { $rejected=$false;try { Resolve-DreamHouseRelease -Root $fixture | Out-Null }catch{$rejected=$true};if(!$rejected){throw "Expected rejection: $label"} }
Reject-Release 'missing manifest'
Save-Manifest
if((Resolve-DreamHouseRelease -Root $fixture) -ne $exe){throw 'Valid package did not resolve.'}
$metadata.executable='..\outside.exe';Save-Manifest;Reject-Release 'escaping executable path'
$metadata.executable=$relative;$metadata.jarSha256='0'*64;Save-Manifest;Reject-Release 'jar mismatch'
$metadata.jarSha256=(Get-DreamHouseFileHash -Path "$folder\app\DreamHouse.jar");Save-Manifest
Move-Item -LiteralPath "$folder\runtime\bin\server\jvm.dll" -Destination "$folder\runtime\bin\server\jvm.fixture"
Reject-Release 'missing runtime'
Move-Item -LiteralPath "$folder\runtime\bin\server\jvm.fixture" -Destination "$folder\runtime\bin\server\jvm.dll"
[IO.File]::WriteAllText("$release\current.json",'invalid json');Reject-Release 'broken manifest'
Save-Manifest
if((Resolve-DreamHouseRelease -Root $fixture) -ne $exe){throw 'Package did not recover after metadata repair.'}
Write-Output 'LAUNCHER_FILES_PASS: valid package, missing/broken metadata, restricted executable path, jar mismatch, missing runtime and repaired metadata; no screenshots'
