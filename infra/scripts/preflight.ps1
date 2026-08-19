#requires -Version 7.0

param(
    [string] $ManifestPath = 'infra\config\runtime-manifest.psd1',
    [switch] $Json
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'runtime-common.ps1')

$manifest = Import-RuntimeManifest $ManifestPath
$results = @($manifest.Components | ForEach-Object {
    Get-RuntimeComponentStatus $_
})

if ($Json) {
    if ($results.Count -eq 1) {
        $results[0] | ConvertTo-Json -Depth 4 -Compress
    } else {
        $results | ConvertTo-Json -Depth 4 -Compress
    }
} else {
    $results | Format-Table Name, Status, DetectedVersion, ExpectedVersion, Path -AutoSize
}

$failedResults = @($results | Where-Object Status -ne 'Ready')
if ($failedResults.Count -gt 0) {
    exit 1
}

exit 0
