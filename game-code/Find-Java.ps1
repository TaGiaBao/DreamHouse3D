$ErrorActionPreference = 'Stop'
function Find-DreamHouseJava {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $candidates += Split-Path (Split-Path $compiler.Source -Parent) -Parent }
    foreach ($base in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft")) {
        if (Test-Path -LiteralPath $base) { $candidates += Get-ChildItem -LiteralPath $base -Directory | Sort-Object LastWriteTime -Descending | ForEach-Object FullName }
    }
    foreach ($candidate in $candidates | Select-Object -Unique) {
        if ((Test-Path -LiteralPath "$candidate\bin\javac.exe") -and (Test-Path -LiteralPath "$candidate\bin\jpackage.exe")) {
            $version = & "$candidate\bin\javac.exe" -version 2>&1
            if ($LASTEXITCODE -eq 0 -and "$version" -match 'javac\s+(\d+)' -and [int]$Matches[1] -ge 21) {
                return $candidate
            }
        }
    }
    throw 'Can JDK 21 tro len de tao ban phat hanh. Ban exe da dong goi khong can cai Java.'
}
