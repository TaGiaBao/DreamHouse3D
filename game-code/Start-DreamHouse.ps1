param([switch]$Rebuild,[switch]$CheckOnly,[switch]$Console)
$ErrorActionPreference = 'Stop'
try {
    if ($Rebuild) { & "$PSScriptRoot\Build-DreamHouse.ps1" }
    . "$PSScriptRoot\Resolve-DreamHouse.ps1"
    $exe = Resolve-DreamHouseRelease -Root $PSScriptRoot
    if ($CheckOnly) { Write-Output $exe; exit 0 }
    # Open the verified package. Rebuilding is an explicit development command.
    Start-Process -FilePath $exe -WorkingDirectory (Split-Path $exe -Parent) -WindowStyle Normal
} catch {
    $message = "Không mở được DreamHouse.`r`n`r`n$($_.Exception.Message)"
    if ($Console) { Write-Output $message }
    else { Add-Type -AssemblyName System.Windows.Forms; [System.Windows.Forms.MessageBox]::Show($message,'DreamHouse',[System.Windows.Forms.MessageBoxButtons]::OK,[System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null }
    exit 1
}
