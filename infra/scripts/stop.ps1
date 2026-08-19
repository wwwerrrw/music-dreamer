#requires -Version 7.0

param(
    [string] $ManifestPath = 'infra\config\runtime-manifest.psd1',
    [switch] $Json
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'runtime-common.ps1')

$manifest = Import-RuntimeManifest $ManifestPath
$runtimeLock = Enter-RuntimeLifecycleLock $manifest
try {
$state = Read-RuntimeState $manifest.StateFile
$updatedProcessStates = [ordered]@{}
$stopResults = @()
$refused = $false

foreach ($processSpec in $manifest.Processes) {
    $currentStatus = Get-ManagedProcessStatus -ProcessSpec $processSpec -State $state
    $stateProperty = $state.Processes.PSObject.Properties[$processSpec.Name]
    $processState = [ordered]@{}

    if ($null -ne $stateProperty) {
        foreach ($property in $stateProperty.Value.PSObject.Properties) {
            $processState[$property.Name] = $property.Value
        }
    } else {
        $processState.Name = $processSpec.Name
        $processState.Pid = $null
        $processState.ExecutablePath = $null
        $processState.StartedAtUtc = $null
        $processState.CommandMarker = $processSpec.CommandMarker
    }

    $stopMethod = $null
    $gracefulRequestSucceeded = $false
    if ($currentStatus.Status -eq 'Running' -and $currentStatus.Owned) {
        $stoppedGracefully = $false
        if ($processSpec.ContainsKey('Stop')) {
            $gracefulRequestSucceeded = Invoke-RuntimeGracefulStop $processSpec.Stop
            if ($gracefulRequestSucceeded) {
                $gracefulTimeoutSeconds = if ($processSpec.Stop.ContainsKey('TimeoutSeconds')) { [int] $processSpec.Stop.TimeoutSeconds } else { 10 }
                $stoppedGracefully = Wait-RuntimeProcessExit -ProcessId $currentStatus.Pid -TimeoutSeconds $gracefulTimeoutSeconds
            }
        }

        if ($stoppedGracefully) {
            $stopMethod = 'Graceful'
            $resultStatus = 'Stopped'
            $message = 'Managed process stopped gracefully.'
        } else {
            $fallbackStatus = Get-ManagedProcessStatus -ProcessSpec $processSpec -State $state
            if ($fallbackStatus.Status -eq 'Running' -and $fallbackStatus.Owned) {
                Stop-Process -Id $fallbackStatus.Pid -Force -ErrorAction Stop
                $stopMethod = if ($gracefulRequestSucceeded) { 'GracefulThenForced' } else { 'Forced' }
                $resultStatus = 'Stopped'
                $message = if ($gracefulRequestSucceeded) {
                    'Graceful shutdown was accepted, but the managed process required a forced stop after the exit timeout.'
                } else {
                    'Managed process required a forced stop because graceful shutdown was not accepted.'
                }
            } elseif ($fallbackStatus.Status -eq 'Stopped') {
                $stopMethod = 'Graceful'
                $resultStatus = 'Stopped'
                $message = 'Managed process stopped gracefully.'
            } else {
                $refused = $true
                $resultStatus = 'Refused'
                $message = 'PID ownership changed during shutdown; no forced stop was attempted.'
            }
        }

        if ($resultStatus -eq 'Stopped') {
            $processState.Status = 'Stopped'
            $processState.StoppedAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
            $processState.StopMethod = $stopMethod
            $processState.GracefulRequestSucceeded = $gracefulRequestSucceeded
        }
    } elseif ($currentStatus.Status -eq 'Unowned') {
        $refused = $true
        $resultStatus = 'Refused'
        $message = 'PID ownership could not be verified; no process was stopped.'
    } else {
        $processState.Status = 'Stopped'
        $processState.StoppedAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
        $resultStatus = 'AlreadyStopped'
        $message = 'Managed process was already stopped.'
    }

    $updatedProcessStates[$processSpec.Name] = $processState
    $stopResults += [pscustomobject]@{
        Name = $processSpec.Name
        Status = $resultStatus
        Pid = $currentStatus.Pid
        Owned = $currentStatus.Owned
        StopMethod = $stopMethod
        GracefulRequestSucceeded = $gracefulRequestSucceeded
        Message = $message
    }
}

$stateFile = Write-RuntimeState -Path $manifest.StateFile -Processes $updatedProcessStates
$stopResults | ForEach-Object {
    $_ | Add-Member -NotePropertyName StateFile -NotePropertyValue $stateFile
}

if ($Json) {
    if ($stopResults.Count -eq 1) {
        $stopResults[0] | ConvertTo-Json -Depth 4 -Compress
    } else {
        $stopResults | ConvertTo-Json -Depth 4 -Compress
    }
} else {
    $stopResults | Format-Table Name, Status, Pid, Owned, Message -AutoSize
}

if ($refused) {
    exit 1
}
} finally {
    Exit-RuntimeLifecycleLock $runtimeLock
}

exit 0
