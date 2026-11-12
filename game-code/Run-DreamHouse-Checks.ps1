param([switch]$IncludeGraphics, [switch]$CaptureScreenshots)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Find-Java.ps1"
. "$PSScriptRoot\Checked-Process.ps1"
$jdk = Find-DreamHouseJava
$root = $PSScriptRoot
$id = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$resultDir = Join-Path $root "glade\test-results\$id"
$classes = Join-Path $resultDir 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$source = Get-ChildItem -LiteralPath "$root\glade\src\main\java\vn\dreamhouse\glade" -Filter '*.java' | ForEach-Object FullName
$checks = Get-ChildItem -LiteralPath "$root\glade\checks" -Filter '*.java' | ForEach-Object FullName
& "$jdk\bin\javac.exe" --release 21 -encoding UTF-8 -cp "$root\glade\lib\*" -d $classes @source @checks
if ($LASTEXITCODE -ne 0) { throw 'Bien dich kiem thu that bai.' }
Get-ChildItem -LiteralPath "$root\glade\src\main\resources" | Copy-Item -Destination $classes -Recurse -Force
$runnerCases = @()
foreach ($code in @(0,7,-1)) {
    $argsProbe = @('-cp', "$classes;$root\glade\lib\*", 'vn.dreamhouse.glade.RunnerProbe', "$code")
    $probe = Invoke-CheckedProcess -FilePath "$jdk\bin\java.exe" -Arguments $argsProbe -WorkingDirectory $root -Stdout "$resultDir\runner-$code.log" -Stderr "$resultDir\runner-$code.stderr.log" -TimeoutMs 2000
    if (($code -eq -1 -and !$probe.Timeout) -or ($code -ne -1 -and ($probe.Timeout -or $probe.ExitCode -ne $code))) { throw "Bo chay kiem thu ghi sai ma thoat: $code" }
    $runnerCases += [ordered]@{ requestedExit=$code; actualExit=$probe.ExitCode; timeout=$probe.Timeout }
}
Write-Host 'PASS Runner verification: exit 0, nonzero with PASS marker, timeout'
$suite = @(@{ Name='LauncherFilesCheck'; Marker='LAUNCHER_FILES_PASS' }, @{ Name='LauncherDataCheck'; Marker='LAUNCHER_DATA_PASS' }, @{ Name='ReleaseDataCheck'; Marker='RELEASE_DATA_PASS' }, @{ Name='RoofEnvelopeCheck'; Marker='ROOF_ENVELOPE_PASS' }, @{ Name='ArchitectureContinuityCheck'; Marker='ARCHITECTURE_CONTINUITY_PASS' }, @{ Name='ArchitectureJoinCheck'; Marker='ARCHITECTURE_JOIN_PASS' }, @{ Name='BlueprintDataCheck'; Marker='BLUEPRINT_DATA_PASS' })
if ($IncludeGraphics) {
    $suite += @(
        @{ Name='ReleaseUiCheck'; Marker='RELEASE_UI_PASS' },
        @{ Name='WorldDialogCheck'; Marker='WORLD_DIALOG_PASS' },
        @{ Name='LauncherUiCheck'; Marker='LAUNCHER_UI_PASS' },
        @{ Name='AtmosphereStyleCheck'; Marker='ATMOSPHERE_STYLE_PASS' },
        @{ Name='GardenStyleCheck'; Marker='GARDEN_STYLE_PASS' },
        @{ Name='RoofColorCheck'; Marker='ROOF_COLOR_PASS' },
        @{ Name='WaterDrawingCheck'; Marker='WATER_DRAWING_PASS' },
        @{ Name='StrokeToolsCheck'; Marker='STROKE_TOOLS_PASS' },
        @{ Name='BlueprintFlowCheck'; Marker='BLUEPRINT_FLOW_PASS' },
        @{ Name='BuildInteractionCheck'; Marker='BUILD_INTERACTION_PASS' },
        @{ Name='FreeCameraAndFlattenCheck'; Marker='FREE_CAMERA_FLATTEN_PASS' },
        @{ Name='ArchitectureMaterialCheck'; Marker='ARCHITECTURE_MATERIAL_PASS' },
        @{ Name='BuildMorphCheck'; Marker='BUILD_MORPH_PASS' },
        @{ Name='SoftWaterCheck'; Marker='SOFT_WATER_PASS' },
        @{ Name='ProceduralMechanicsCheck'; Marker='PROCEDURAL_MECHANICS_PASS' },
        @{ Name='RightOrbitCheck'; Marker='RIGHT_ORBIT_PASS' }
    )
}
$results = @()
$suite += @(@{ Name='SessionPersistenceCheck'; Marker='SESSION_PERSISTENCE_PASS' }, @{ Name='ResourceCacheCheck'; Marker='RESOURCE_CACHE_PASS' })
if($IncludeGraphics){$suite += @{ Name='PersistenceUiCheck'; Marker='PERSISTENCE_UI_PASS' }}
foreach ($test in $suite) {
    $name = $test.Name
    $log = Join-Path $resultDir "$name.log"
    $stderr = Join-Path $resultDir "$name.stderr.log"
    $arguments = @('--enable-native-access=ALL-UNNAMED', "-Ddreamhouse.userDir=$resultDir\player-$name", "-Ddreamhouse.captureTests=$($CaptureScreenshots.IsPresent.ToString().ToLowerInvariant())", '-cp', "$classes;$root\glade\lib\*", "vn.dreamhouse.glade.$name")
    if($name -eq 'LauncherFilesCheck'){
        $shell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
        $execution = Invoke-CheckedProcess -FilePath $shell -Arguments @('-NoLogo','-NoProfile','-ExecutionPolicy','Bypass','-File',"$root\Test-DreamHouse-Launcher.ps1",'-ResultDirectory',$resultDir) -WorkingDirectory $root -Stdout $log -Stderr $stderr
    }else{
        $execution = Invoke-CheckedProcess -FilePath "$jdk\bin\java.exe" -Arguments $arguments -WorkingDirectory $root -Stdout $log -Stderr $stderr
    }
    $pass = !$execution.Timeout -and $execution.ExitCode -eq 0 -and (Select-String -LiteralPath $log -SimpleMatch $test.Marker -Quiet)
    $status = if ($pass) { 'PASS' } else { 'FAIL' }
    $results += [ordered]@{ test=$name; status=$status; exitCode=$execution.ExitCode; timeout=$execution.Timeout; seconds=$execution.Seconds; log=$log; stderr=$stderr }
    Write-Host "$status $name"
}
$summary = [ordered]@{ buildId=$id; graphics=$IncludeGraphics.IsPresent; screenshots=$CaptureScreenshots.IsPresent; runnerVerified=$true; runnerCases=$runnerCases; passed=(@($results | Where-Object status -eq 'FAIL').Count -eq 0); tests=$results }
$summary | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "$resultDir\summary.json" -Encoding UTF8
Write-Host "Ket qua: $resultDir\summary.json"
if (!$summary.passed) { exit 1 }
