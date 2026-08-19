#requires -Version 7.0

param(
    [string] $ManifestPath = 'infra\config\runtime-manifest.psd1',
    [switch] $Json
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'runtime-common.ps1')

$manifest = Import-RuntimeManifest $ManifestPath
$state = Read-RuntimeState $manifest.StateFile
$stateFile = Resolve-MusicDreamerPath $manifest.StateFile
$results = @($manifest.Processes | ForEach-Object {
    $result = Get-ManagedProcessStatus -ProcessSpec $_ -State $state
    if ($result.Status -eq 'Running' -and $result.Owned -and $_.ContainsKey('Readiness')) {
        $ready = Test-RuntimeProcessReadiness $_.Readiness
        $result.Status = if ($ready) { 'Ready' } else { 'Unready' }
        $result.Message = if ($ready) { 'Managed process is ready.' } else { 'Managed process is running but its readiness check failed.' }
        $result | Add-Member -NotePropertyName Ready -NotePropertyValue $ready
    }
    $result | Add-Member -NotePropertyName StateFile -NotePropertyValue $stateFile
    $result
})

if ($Json) {
    if ($results.Count -eq 1) {
        $results[0] | ConvertTo-Json -Depth 4 -Compress
    } else {
        $results | ConvertTo-Json -Depth 4 -Compress
    }
} else {
    $results | Format-Table Name, Status, Pid, Owned, ExecutablePath, StandardOutputPath, StandardErrorPath -AutoSize
}

exit 0
