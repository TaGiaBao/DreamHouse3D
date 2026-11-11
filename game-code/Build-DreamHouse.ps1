param([switch]$NoPackage)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Find-Java.ps1"
. "$PSScriptRoot\Resolve-DreamHouse.ps1"
$jdk = Find-DreamHouseJava
$root = $PSScriptRoot
$id = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$build = Join-Path $root "release\build-$id"
$classes = Join-Path $build 'classes'
$inputDir = Join-Path $build 'input'
New-Item -ItemType Directory -Force -Path $classes, "$inputDir\lib" | Out-Null
$sources = Get-ChildItem -LiteralPath "$root\glade\src\main\java\vn\dreamhouse\glade" -Filter '*.java' | ForEach-Object FullName
& "$jdk\bin\javac.exe" --release 21 -encoding UTF-8 -cp "$root\glade\lib\*" -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Bien dich game that bai.' }
if ($NoPackage) { Write-Output $classes; exit 0 }
$jars = Get-ChildItem -LiteralPath "$root\glade\lib" -Filter '*.jar'
$jars | Copy-Item -Destination "$inputDir\lib"
$classpath = ($jars | ForEach-Object { "lib/$($_.Name)" }) -join ' '
$manifest = Join-Path $build 'manifest.mf'
# jar wraps manifest attributes to the required line length.
$manifestLines = @('Manifest-Version: 1.0', 'Main-Class: vn.dreamhouse.glade.GladeLauncher')
$line = "Class-Path: $classpath"
while ($line.Length -gt 68) { $manifestLines += $line.Substring(0,68); $line = ' ' + $line.Substring(68) }
$manifestLines += $line
(($manifestLines -join "`r`n") + "`r`n`r`n") | Set-Content -LiteralPath $manifest -Encoding ASCII
& "$jdk\bin\java.exe" -cp "$classes;$root\glade\lib\*" vn.dreamhouse.glade.LauncherIcon "$inputDir\DreamHouse.ico"
if ($LASTEXITCODE -ne 0) { throw 'Tao icon that bai.' }
& "$jdk\bin\jar.exe" cfm "$inputDir\DreamHouse.jar" $manifest -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Tao jar that bai.' }
Write-Host 'Dang tao DreamHouse.exe kem Java runtime...'
& "$jdk\bin\jpackage.exe" --type app-image --name DreamHouse --app-version 0.2.0 --vendor DreamHouse --input $inputDir --main-jar DreamHouse.jar --main-class vn.dreamhouse.glade.GladeLauncher --dest "$build\bundled" --icon "$inputDir\DreamHouse.ico" --add-modules 'java.desktop,java.logging,java.xml,java.management,java.prefs,jdk.unsupported,jdk.charsets,jdk.crypto.ec' --java-options '--enable-native-access=ALL-UNNAMED'
if ($LASTEXITCODE -ne 0) { throw 'Dong goi exe that bai.' }
$exe = Join-Path $build 'bundled\DreamHouse\DreamHouse.exe'
if (!(Test-Path -LiteralPath $exe)) { throw 'Khong tim thay exe sau dong goi.' }
$metadata = [ordered]@{ buildId=$id; createdUtc=(Get-Date).ToUniversalTime().ToString('o'); executable="build-$id\bundled\DreamHouse\DreamHouse.exe"; javaBundled=$true; jarSha256=(Get-DreamHouseFileHash -Path "$inputDir\DreamHouse.jar") }
$metadata | ConvertTo-Json | Set-Content -LiteralPath "$root\release\current.json" -Encoding UTF8
@'
DreamHouse Windows portable
Run DreamHouse.exe. Keep the app and runtime folders alongside it.
No JDK, Maven, Docker or administrator access is required to play.
Player saves, recovery versions, settings and logs are stored under LOCALAPPDATA\DreamHouse.
Third-party libraries remain in app\lib with their embedded license notices.
'@ | Set-Content -LiteralPath "$build\bundled\DreamHouse\README.txt" -Encoding UTF8
Write-Output $exe
