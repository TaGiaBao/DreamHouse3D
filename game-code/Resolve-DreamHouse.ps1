Import-Module -Name (Join-Path $PSHOME 'Modules\Microsoft.PowerShell.Utility\Microsoft.PowerShell.Utility.psd1') -ErrorAction Stop
function Resolve-DreamHouseRelease {
    param([Parameter(Mandatory=$true)][string]$Root)
    $manifest = Join-Path $Root 'release\current.json'
    if (!(Test-Path -LiteralPath $manifest -PathType Leaf)) { throw 'Chưa tìm thấy bản chơi. Hãy dùng thư mục DreamHouse đã đóng gói đầy đủ.' }
    try { $build = Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json } catch { throw 'Thông tin bản chơi bị lỗi. Hãy khôi phục bản đóng gói đã kiểm tra.' }
    if ($build.executable -notmatch '^build-[0-9-]+\\bundled\\DreamHouse\\DreamHouse\.exe$') { throw 'Thông tin đường dẫn bản chơi không hợp lệ.' }
    $exe = Join-Path (Join-Path $Root 'release') $build.executable
    $app = Join-Path (Split-Path $exe -Parent) 'app\DreamHouse.jar'
    $runtime = Join-Path (Split-Path $exe -Parent) 'runtime\bin\server\jvm.dll'
    if (!(Test-Path -LiteralPath $exe -PathType Leaf) -or !(Test-Path -LiteralPath $app -PathType Leaf) -or !(Test-Path -LiteralPath $runtime -PathType Leaf)) { throw 'Bản chơi thiếu tệp. Giữ DreamHouse.exe cùng thư mục app và runtime, rồi mở lại.' }
    if ($build.jarSha256 -notmatch '^[0-9a-fA-F]{64}$' -or (Get-DreamHouseFileHash -Path $app) -ne $build.jarSha256) { throw 'Tệp chương trình chưa khớp bản đã đóng gói. Hãy dùng lại bản phát hành đầy đủ.' }
    return $exe
}
function Get-DreamHouseFileHash {
    param([Parameter(Mandatory=$true)][string]$Path)
    $algorithm=[Security.Cryptography.SHA256]::Create()
    $stream=[IO.File]::OpenRead($Path)
    try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-','') }
    finally { $stream.Dispose();$algorithm.Dispose() }
}
