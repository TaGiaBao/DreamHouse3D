param([ValidateRange(30,7200)][int]$Seconds=720)
$ErrorActionPreference='Stop'
. "$PSScriptRoot\Find-Java.ps1"
$jdk=Find-DreamHouseJava
$root=$PSScriptRoot
$id=Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$result=Join-Path $root "glade\test-results\soak-$id"
$classes=Join-Path $result 'classes'
$player=Join-Path $result 'player'
New-Item -ItemType Directory -Force -Path $classes,$player | Out-Null
$source=Get-ChildItem -LiteralPath "$root\glade\src\main\java\vn\dreamhouse\glade" -Filter '*.java' | ForEach-Object FullName
$checks=Get-ChildItem -LiteralPath "$root\glade\checks" -Filter '*.java' | ForEach-Object FullName
& "$jdk\bin\javac.exe" --release 21 -encoding UTF-8 -cp "$root\glade\lib\*" -d $classes @source @checks
if($LASTEXITCODE -ne 0){throw 'Soak compilation failed.'}
$arguments=@('--enable-native-access=ALL-UNNAMED',"-Ddreamhouse.userDir=$player","-Ddreamhouse.soakSeconds=$Seconds",'-Ddreamhouse.captureTests=false','-cp',"$classes;$root\glade\lib\*",'vn.dreamhouse.glade.SessionSoakCheck')
$process=New-Object Diagnostics.Process
$process.StartInfo.FileName="$jdk\bin\java.exe"
$process.StartInfo.Arguments=($arguments | ForEach-Object {'"'+$_+'"'}) -join ' '
$process.StartInfo.WorkingDirectory=$root
$process.StartInfo.UseShellExecute=$false
$process.StartInfo.CreateNoWindow=$true
$process.StartInfo.RedirectStandardOutput=$true
$process.StartInfo.RedirectStandardError=$true
$watch=[Diagnostics.Stopwatch]::StartNew()
$samples=@();$timeout=$false;$next=0
try{
    if(!$process.Start()){throw 'Soak process did not start.'}
    $out=$process.StandardOutput.ReadToEndAsync();$err=$process.StandardError.ReadToEndAsync()
    [IO.File]::WriteAllText((Join-Path $result 'running.txt'),"pid=$($process.Id)`nplayer=$player`nseconds=$Seconds")
    while(!$process.WaitForExit(1000)){
        if($watch.Elapsed.TotalSeconds -gt $Seconds+150){$timeout=$true;$process.Kill();break}
        if($watch.Elapsed.TotalSeconds -ge $next){
            try{$stats=[Diagnostics.Process]::GetProcessById($process.Id);$samples+=@{seconds=[math]::Round($watch.Elapsed.TotalSeconds,2);working_set_bytes=$stats.WorkingSet64;private_bytes=$stats.PrivateMemorySize64;cpu_ms=$stats.TotalProcessorTime.TotalMilliseconds};$stats.Dispose();$samples | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $result 'process-samples.json') -Encoding UTF8}catch{}
            Write-Host "SOAK_MONITOR elapsed=$([int]$watch.Elapsed.TotalSeconds)s pid=$($process.Id)"
            $next=$watch.Elapsed.TotalSeconds+30
        }
    }
    $process.WaitForExit();$exitCode=[int]$process.ExitCode
    [IO.File]::WriteAllText((Join-Path $result 'stdout.log'),$out.GetAwaiter().GetResult())
    [IO.File]::WriteAllText((Join-Path $result 'stderr.log'),$err.GetAwaiter().GetResult())
    $progress=Join-Path $player 'session-soak.json'
    $complete=$false;if(Test-Path -LiteralPath $progress){$complete=(Get-Content -LiteralPath $progress -Raw | ConvertFrom-Json).complete}
    $errors=Join-Path $player 'logs\game-errors.log'
    $unexpectedErrors=(Test-Path -LiteralPath $errors) -and (Get-Item -LiteralPath $errors).Length -gt 0
    $passed=!$timeout -and $exitCode -eq 0 -and $complete -and !$unexpectedErrors -and (Select-String -LiteralPath (Join-Path $result 'stdout.log') -Pattern 'SESSION_SOAK_PASS' -SimpleMatch -Quiet)
    @{passed=$passed;exitCode=$exitCode;timeout=$timeout;requested_seconds=$Seconds;wall_seconds=[math]::Round($watch.Elapsed.TotalSeconds,2);screenshots=$false;unexpected_game_errors=$unexpectedErrors;progress_report=$progress;process_samples=(Join-Path $result 'process-samples.json')} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $result 'summary.json') -Encoding UTF8
    Write-Host "SOAK_RESULT $result\summary.json"
    if(!$passed){exit 1}
}finally{$process.Dispose()}
