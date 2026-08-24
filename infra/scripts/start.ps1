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
$componentResults = @($manifest.Components | ForEach-Object {
    Get-RuntimeComponentStatus $_
})
$failedComponents = @($componentResults | Where-Object Status -ne 'Ready')

if ($failedComponents.Count -gt 0) {
    if ($Json) {
        $failedComponents | ConvertTo-Json -Depth 4 -Compress
    } else {
        $failedComponents | Format-Table Name, Status, DetectedVersion, ExpectedVersion, Path -AutoSize
    }
    exit 1
}

$startResults = @()
$existingState = Read-RuntimeState $manifest.StateFile
$processStates = [ordered]@{}
foreach ($existingProcessProperty in $existingState.Processes.PSObject.Properties) {
    $existingProcessState = [ordered]@{}
    foreach ($property in $existingProcessProperty.Value.PSObject.Properties) {
        $existingProcessState[$property.Name] = $property.Value
    }
    $processStates[$existingProcessProperty.Name] = $existingProcessState
}

foreach ($processSpec in $manifest.Processes) {
    $existingStatus = Get-ManagedProcessStatus -ProcessSpec $processSpec -State $existingState
    if ($existingStatus.Status -eq 'Unowned') {
        $existingStateProperty = $existingState.Processes.PSObject.Properties[$processSpec.Name]
        $replacementAllowed = $null -ne $existingStateProperty -and (
            Test-RuntimeProcessReplacementAllowed `
                -ProcessSpec $processSpec `
                -ExistingStatus $existingStatus `
                -RecordedState $existingStateProperty.Value)
        if ($replacementAllowed) {
            $existingStatus = [pscustomobject]@{
                Name = $processSpec.Name
                Status = 'Stopped'
                Pid = $existingStatus.Pid
                Owned = $false
                ExecutablePath = $existingStatus.ExecutablePath
                Message = 'Recorded process ownership changed and its readiness check failed; starting a replacement without stopping the reused PID.'
            }
        }
    }
    if ($existingStatus.Status -eq 'Unowned') {
        $refusedResult = [pscustomobject]@{
            Name = $processSpec.Name
            Pid = $existingStatus.Pid
            Status = 'Refused'
            ExecutablePath = $existingStatus.ExecutablePath
            Message = 'PID ownership could not be verified; no replacement process was started.'
        }
        if ($Json) {
            $refusedResult | ConvertTo-Json -Depth 4 -Compress
        } else {
            $refusedResult | Format-Table Name, Status, Pid, ExecutablePath, Message -AutoSize
        }
        exit 1
    }
    if ($existingStatus.Status -eq 'Running' -and $existingStatus.Owned) {
        $existingStateProperty = $existingState.Processes.PSObject.Properties[$processSpec.Name]
        $existingProcessState = [ordered]@{}
        foreach ($property in $existingStateProperty.Value.PSObject.Properties) {
            $existingProcessState[$property.Name] = $property.Value
        }
        $processStates[$processSpec.Name] = $existingProcessState
        $startResults += [pscustomobject]@{
            Name = $processSpec.Name
            Pid = $existingStatus.Pid
            Status = 'AlreadyRunning'
            ExecutablePath = $existingStatus.ExecutablePath
            StartedAtUtc = $existingProcessState.StartedAtUtc
            CommandMarker = $existingProcessState.CommandMarker
            WorkingDirectory = $existingStatus.WorkingDirectory
            StandardOutputPath = $existingStatus.StandardOutputPath
            StandardErrorPath = $existingStatus.StandardErrorPath
        }
        continue
    }

    if ($processSpec.ContainsKey('RequiredDirectories')) {
        foreach ($requiredDirectory in @($processSpec.RequiredDirectories)) {
            $resolvedDirectory = Resolve-MusicDreamerPath $requiredDirectory
            if (Test-Path -LiteralPath $resolvedDirectory) {
                if (-not (Test-Path -LiteralPath $resolvedDirectory -PathType Container)) {
                    throw "Required runtime directory '$resolvedDirectory' exists but is not a directory."
                }
                continue
            }

            New-Item -ItemType Directory -Path $resolvedDirectory -Force | Out-Null
        }
    }

    $filePath = Resolve-MusicDreamerCommandPath $processSpec.FilePath
    $workingDirectory = if ($processSpec.ContainsKey('WorkingDirectory')) {
        Resolve-MusicDreamerPath $processSpec.WorkingDirectory
    } else {
        $script:MusicDreamerProjectRoot
    }
    if (-not (Test-Path -LiteralPath $workingDirectory -PathType Container)) {
        throw "Runtime working directory '$workingDirectory' does not exist."
    }

    $startArguments = @{
        FilePath = $filePath
        ArgumentList = @($processSpec.Arguments)
        WorkingDirectory = $workingDirectory
        WindowStyle = 'Hidden'
        PassThru = $true
    }
    if ($processSpec.ContainsKey('EnvironmentFiles')) {
        $startArguments.Environment = Get-RuntimeProcessEnvironment -EnvironmentFiles @($processSpec.EnvironmentFiles)
    }

    $standardOutputPath = $null
    $standardErrorPath = $null
    if ($processSpec.ContainsKey('LogDirectory')) {
        $logPaths = New-RuntimeLogPaths -ProcessName $processSpec.Name -LogDirectory $processSpec.LogDirectory
        $standardOutputPath = $logPaths.StandardOutputPath
        $standardErrorPath = $logPaths.StandardErrorPath
        $startArguments.RedirectStandardOutput = $standardOutputPath
        $startArguments.RedirectStandardError = $standardErrorPath
    }

    $startedProcess = Start-Process @startArguments
    $startedProcess.Refresh()

    $processState = [ordered]@{
        Name = $processSpec.Name
        Pid = $startedProcess.Id
        Status = 'Running'
        ExecutablePath = $filePath
        StartedAtUtc = $startedProcess.StartTime.ToUniversalTime().ToString('O')
        CommandMarker = $processSpec.CommandMarker
        WorkingDirectory = $workingDirectory
        StandardOutputPath = $standardOutputPath
        StandardErrorPath = $standardErrorPath
    }
    $processStates[$processSpec.Name] = $processState
    $stateFile = Write-RuntimeState -Path $manifest.StateFile -Processes $processStates

    if ($processSpec.ContainsKey('Readiness')) {
        Wait-RuntimeProcessReadiness -ProcessSpec $processSpec -ProcessId $startedProcess.Id
        $processState.Status = 'Ready'
        $processState.Ready = $true
        $processState.ReadyAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
        $processStates[$processSpec.Name] = $processState
        $stateFile = Write-RuntimeState -Path $manifest.StateFile -Processes $processStates
    }

    $startResults += [pscustomobject] $processState
}

$stateFile = Write-RuntimeState -Path $manifest.StateFile -Processes $processStates
$startResults | ForEach-Object {
    $_ | Add-Member -NotePropertyName StateFile -NotePropertyValue $stateFile
}

if ($Json) {
    if ($startResults.Count -eq 1) {
        $startResults[0] | ConvertTo-Json -Depth 4 -Compress
    } else {
        $startResults | ConvertTo-Json -Depth 4 -Compress
    }
} else {
    $startResults | Format-Table Name, Status, Pid, ExecutablePath -AutoSize
}
} finally {
    Exit-RuntimeLifecycleLock $runtimeLock
}

exit 0
