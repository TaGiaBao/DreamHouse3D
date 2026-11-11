function Invoke-CheckedProcess {
    param([string]$FilePath, [string[]]$Arguments, [string]$WorkingDirectory, [string]$Stdout, [string]$Stderr, [int]$TimeoutMs = 180000)
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo.FileName = $FilePath
    $process.StartInfo.Arguments = ($Arguments | ForEach-Object { '"' + $_ + '"' }) -join ' '
    $process.StartInfo.WorkingDirectory = $WorkingDirectory
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.CreateNoWindow = $true
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    $clock = [Diagnostics.Stopwatch]::StartNew()
    try {
        if (!$process.Start()) { throw 'Khong khoi dong duoc tien trinh kiem thu.' }
        # Hold our own process handle; Start-Process objects can lose ExitCode on Windows PowerShell.
        $outTask = $process.StandardOutput.ReadToEndAsync()
        $errTask = $process.StandardError.ReadToEndAsync()
        $timedOut = !$process.WaitForExit($TimeoutMs)
        if ($timedOut) { $process.Kill() }
        $process.WaitForExit()
        $code = [int]$process.ExitCode
        [IO.File]::WriteAllText($Stdout, $outTask.GetAwaiter().GetResult())
        [IO.File]::WriteAllText($Stderr, $errTask.GetAwaiter().GetResult())
        $clock.Stop()
        return [pscustomobject]@{ ExitCode=$code; Timeout=$timedOut; Seconds=[math]::Round($clock.Elapsed.TotalSeconds,2) }
    } finally { $process.Dispose() }
}
