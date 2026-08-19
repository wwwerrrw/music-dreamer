#requires -Version 7.0

param(
    [Parameter(Mandatory)]
    [string] $PidFile,

    [int] $Seconds = 30
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$pidFilePath = [System.IO.Path]::GetFullPath($PidFile)
$pidFileParent = Split-Path -Parent $pidFilePath
if (-not (Test-Path -LiteralPath $pidFileParent -PathType Container)) {
    New-Item -ItemType Directory -Path $pidFileParent -Force | Out-Null
}

Set-Content -LiteralPath $pidFilePath -Value $PID -Encoding ascii
Start-Sleep -Seconds $Seconds
