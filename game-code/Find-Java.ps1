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
        if ((Test-Path -LiteralPath "$candidate\bin\javac.exe") -and (Test-Path -LiteralPath "$candidate\bin\jpackage.exe")) { return $candidate }
    }
    throw 'Can JDK 21 tro len de tao ban phat hanh. Ban exe da dong goi khong can cai Java.'
}
