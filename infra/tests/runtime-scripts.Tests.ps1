#requires -Version 7.0

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$preflightScript = Join-Path $projectRoot 'infra\scripts\preflight.ps1'
$manifestPath = Join-Path $PSScriptRoot 'fixtures\valid-runtime.psd1'

function Assert-Equal {
    param(
        [Parameter(Mandatory)]
        [AllowNull()]
        $Actual,

        [Parameter(Mandatory)]
        [AllowNull()]
        $Expected,

        [Parameter(Mandatory)]
        [string] $Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message. Expected '$Expected', got '$Actual'."
    }
}

function Assert-True {
    param(
        [Parameter(Mandatory)]
        [bool] $Condition,

        [Parameter(Mandatory)]
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

if (-not (Test-Path -LiteralPath $preflightScript -PathType Leaf)) {
    throw "preflightReportsExactVersionAndAbsolutePath failed: expected production script '$preflightScript' to exist."
}

$output = & $preflightScript -ManifestPath $manifestPath -Json 2>&1
$exitCode = $LASTEXITCODE
$result = ($output -join [Environment]::NewLine) | ConvertFrom-Json

Assert-Equal $exitCode 0 'preflightReportsExactVersionAndAbsolutePath exit code'
Assert-Equal $result.Name 'Node' 'preflightReportsExactVersionAndAbsolutePath component name'
Assert-Equal $result.Status 'Ready' 'preflightReportsExactVersionAndAbsolutePath status'
Assert-Equal $result.DetectedVersion '20.19.5' 'preflightReportsExactVersionAndAbsolutePath detected version'
Assert-Equal $result.ExpectedVersion '20.19.5' 'preflightReportsExactVersionAndAbsolutePath expected version'
Assert-True ([System.IO.Path]::IsPathFullyQualified($result.Path)) 'preflightReportsExactVersionAndAbsolutePath must report an absolute path'
Assert-True ($result.Path.EndsWith('infra\tests\fixtures\node20.cmd')) 'preflightReportsExactVersionAndAbsolutePath reported the wrong executable path'

Write-Output 'PASS preflightReportsExactVersionAndAbsolutePath'

$missingManifestPath = Join-Path $PSScriptRoot 'fixtures\missing-runtime.psd1'
try {
    $missingOutput = & $preflightScript -ManifestPath $missingManifestPath -Json 2>&1
    $missingExitCode = $LASTEXITCODE
} catch {
    throw "preflightReportsMissingRuntime failed: expected structured JSON, but preflight threw '$($_.Exception.Message)'."
}

$missingResult = ($missingOutput -join [Environment]::NewLine) | ConvertFrom-Json
$expectedMissingPath = Join-Path $projectRoot 'infra\runtime\node-v20.19.5-win-x64\node.exe'

Assert-Equal $missingExitCode 1 'preflightReportsMissingRuntime exit code'
Assert-Equal $missingResult.Status 'Missing' 'preflightReportsMissingRuntime status'
Assert-Equal $missingResult.ExpectedVersion '20.19.5' 'preflightReportsMissingRuntime expected version'
Assert-Equal $missingResult.DetectedVersion $null 'preflightReportsMissingRuntime detected version'
Assert-Equal $missingResult.Path $expectedMissingPath 'preflightReportsMissingRuntime absolute path'
Assert-True ($missingResult.Message.Contains('20.19.5')) 'preflightReportsMissingRuntime message must include the expected version'
Assert-True ($missingResult.Message.Contains($expectedMissingPath)) 'preflightReportsMissingRuntime message must include the absolute path'

Write-Output 'PASS preflightReportsMissingRuntime'

$wrongVersionManifestPath = Join-Path $PSScriptRoot 'fixtures\wrong-version-runtime.psd1'
$wrongVersionOutput = & $preflightScript -ManifestPath $wrongVersionManifestPath -Json 2>&1
$wrongVersionExitCode = $LASTEXITCODE
$wrongVersionResult = ($wrongVersionOutput -join [Environment]::NewLine) | ConvertFrom-Json
$expectedWrongVersionPath = Join-Path $projectRoot 'infra\tests\fixtures\node24.cmd'

Assert-Equal $wrongVersionExitCode 1 'preflightReportsVersionMismatch exit code'
Assert-Equal $wrongVersionResult.Status 'VersionMismatch' 'preflightReportsVersionMismatch status'
Assert-Equal $wrongVersionResult.DetectedVersion '24.18.0' 'preflightReportsVersionMismatch detected version'
Assert-Equal $wrongVersionResult.ExpectedVersion '20.19.5' 'preflightReportsVersionMismatch expected version'
Assert-Equal $wrongVersionResult.Path $expectedWrongVersionPath 'preflightReportsVersionMismatch absolute path'
Assert-True ($wrongVersionResult.Message.Contains('24.18.0')) 'preflightReportsVersionMismatch message must include the detected version'
Assert-True ($wrongVersionResult.Message.Contains('20.19.5')) 'preflightReportsVersionMismatch message must include the expected version'

Write-Output 'PASS preflightReportsVersionMismatch'

$startScript = Join-Path $projectRoot 'infra\scripts\start.ps1'
$lifecycleManifestPath = Join-Path $PSScriptRoot 'fixtures\lifecycle-runtime.psd1'
$lifecycleStatePath = Join-Path $projectRoot 'runtime-data\test-runtime-state.json'

if (-not (Test-Path -LiteralPath $startScript -PathType Leaf)) {
    throw "startCreatesManagedProcessState failed: expected production script '$startScript' to exist."
}

$startedPid = $null
try {
    $startOutput = & $startScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $startExitCode = $LASTEXITCODE
    $startResult = ($startOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $startedPid = [int] $startResult.Pid

    Assert-Equal $startExitCode 0 'startCreatesManagedProcessState exit code'
    Assert-Equal $startResult.Name 'Dummy' 'startCreatesManagedProcessState process name'
    Assert-Equal $startResult.Status 'Running' 'startCreatesManagedProcessState status'
    Assert-True ($startedPid -gt 0) 'startCreatesManagedProcessState must return a positive PID'
    Assert-Equal $startResult.StateFile $lifecycleStatePath 'startCreatesManagedProcessState absolute state file path'
    Assert-True (Test-Path -LiteralPath $lifecycleStatePath -PathType Leaf) 'startCreatesManagedProcessState must persist its state file'

    $persistedState = Get-Content -Raw -LiteralPath $lifecycleStatePath | ConvertFrom-Json
    Assert-Equal ([int] $persistedState.Processes.Dummy.Pid) $startedPid 'startCreatesManagedProcessState persisted PID'
    Assert-True ($null -ne (Get-Process -Id $startedPid -ErrorAction SilentlyContinue)) 'startCreatesManagedProcessState process must be alive'
} finally {
    if ($null -ne $startedPid) {
        Stop-Process -Id $startedPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS startCreatesManagedProcessState'

$statusScript = Join-Path $projectRoot 'infra\scripts\status.ps1'
if (-not (Test-Path -LiteralPath $statusScript -PathType Leaf)) {
    throw "statusReportsOwnedLiveProcess failed: expected production script '$statusScript' to exist."
}

$statusTestPid = $null
try {
    $statusStartOutput = & $startScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $statusStartResult = ($statusStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $statusTestPid = [int] $statusStartResult.Pid

    $statusOutput = & $statusScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $statusExitCode = $LASTEXITCODE
    $statusResult = ($statusOutput -join [Environment]::NewLine) | ConvertFrom-Json

    Assert-Equal $statusExitCode 0 'statusReportsOwnedLiveProcess exit code'
    Assert-Equal $statusResult.Name 'Dummy' 'statusReportsOwnedLiveProcess process name'
    Assert-Equal $statusResult.Status 'Running' 'statusReportsOwnedLiveProcess status'
    Assert-Equal ([int] $statusResult.Pid) $statusTestPid 'statusReportsOwnedLiveProcess PID'
    Assert-Equal $statusResult.Owned $true 'statusReportsOwnedLiveProcess ownership flag'
} finally {
    if ($null -ne $statusTestPid) {
        Stop-Process -Id $statusTestPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS statusReportsOwnedLiveProcess'

$unownedTestPid = $null
try {
    $unownedStartOutput = & $startScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $unownedStartResult = ($unownedStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $unownedTestPid = [int] $unownedStartResult.Pid

    $forgedState = Get-Content -Raw -LiteralPath $lifecycleStatePath | ConvertFrom-Json
    $forgedState.Processes.Dummy.CommandMarker = 'command-marker-that-is-not-present'
    $forgedState | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $lifecycleStatePath -Encoding utf8

    $unownedOutput = & $statusScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $unownedExitCode = $LASTEXITCODE
    $unownedResult = ($unownedOutput -join [Environment]::NewLine) | ConvertFrom-Json

    Assert-Equal $unownedExitCode 0 'statusRejectsCommandMarkerMismatch exit code'
    Assert-Equal $unownedResult.Status 'Unowned' 'statusRejectsCommandMarkerMismatch status'
    Assert-Equal $unownedResult.Owned $false 'statusRejectsCommandMarkerMismatch ownership flag'
    Assert-True ($null -ne (Get-Process -Id $unownedTestPid -ErrorAction SilentlyContinue)) 'statusRejectsCommandMarkerMismatch must not terminate the process'
} finally {
    if ($null -ne $unownedTestPid) {
        Stop-Process -Id $unownedTestPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS statusRejectsCommandMarkerMismatch'

$stopScript = Join-Path $projectRoot 'infra\scripts\stop.ps1'
$persistentFixturePath = Join-Path $projectRoot 'infra\tests\fixtures\persistent-data\preserve.txt'
if (-not (Test-Path -LiteralPath $stopScript -PathType Leaf)) {
    throw "stopTerminatesOnlyOwnedProcessAndPreservesData failed: expected production script '$stopScript' to exist."
}

$stopTestPid = $null
try {
    $stopStartOutput = & $startScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $stopStartResult = ($stopStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $stopTestPid = [int] $stopStartResult.Pid

    $stopOutput = & $stopScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $stopExitCode = $LASTEXITCODE
    $stopResult = ($stopOutput -join [Environment]::NewLine) | ConvertFrom-Json

    Assert-Equal $stopExitCode 0 'stopTerminatesOnlyOwnedProcessAndPreservesData exit code'
    Assert-Equal $stopResult.Name 'Dummy' 'stopTerminatesOnlyOwnedProcessAndPreservesData process name'
    Assert-Equal $stopResult.Status 'Stopped' 'stopTerminatesOnlyOwnedProcessAndPreservesData status'
    Assert-Equal ([int] $stopResult.Pid) $stopTestPid 'stopTerminatesOnlyOwnedProcessAndPreservesData PID'

    $waitDeadline = [DateTimeOffset]::UtcNow.AddSeconds(5)
    while ($null -ne (Get-Process -Id $stopTestPid -ErrorAction SilentlyContinue) -and [DateTimeOffset]::UtcNow -lt $waitDeadline) {
        Start-Sleep -Milliseconds 50
    }

    Assert-True ($null -eq (Get-Process -Id $stopTestPid -ErrorAction SilentlyContinue)) 'stopTerminatesOnlyOwnedProcessAndPreservesData process must exit'
    Assert-True (Test-Path -LiteralPath $lifecycleStatePath -PathType Leaf) 'stopTerminatesOnlyOwnedProcessAndPreservesData must preserve the state file'
    Assert-True (Test-Path -LiteralPath $persistentFixturePath -PathType Leaf) 'stopTerminatesOnlyOwnedProcessAndPreservesData must preserve persistent data'

    $stoppedState = Get-Content -Raw -LiteralPath $lifecycleStatePath | ConvertFrom-Json
    Assert-Equal $stoppedState.Processes.Dummy.Status 'Stopped' 'stopTerminatesOnlyOwnedProcessAndPreservesData persisted status'
    Assert-True (-not [string]::IsNullOrWhiteSpace($stoppedState.Processes.Dummy.StoppedAtUtc)) 'stopTerminatesOnlyOwnedProcessAndPreservesData must record stop time'
} finally {
    if ($null -ne $stopTestPid) {
        Stop-Process -Id $stopTestPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS stopTerminatesOnlyOwnedProcessAndPreservesData'

$firstStartPid = $null
$secondStartPid = $null
try {
    $firstStartOutput = & $startScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $firstStartResult = ($firstStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $firstStartPid = [int] $firstStartResult.Pid

    $secondStartOutput = & $startScript -ManifestPath $lifecycleManifestPath -Json 2>&1
    $secondStartExitCode = $LASTEXITCODE
    $secondStartResult = ($secondStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $secondStartPid = [int] $secondStartResult.Pid

    Assert-Equal $secondStartExitCode 0 'startIsIdempotent exit code'
    Assert-Equal $secondStartResult.Status 'AlreadyRunning' 'startIsIdempotent status'
    Assert-Equal $secondStartPid $firstStartPid 'startIsIdempotent PID'
} finally {
    if ($null -ne $firstStartPid) {
        Stop-Process -Id $firstStartPid -Force -ErrorAction SilentlyContinue
    }
    if ($null -ne $secondStartPid -and $secondStartPid -ne $firstStartPid) {
        Stop-Process -Id $secondStartPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS startIsIdempotent'

$markerManifestPath = Join-Path $PSScriptRoot 'fixtures\marker-runtime.psd1'
try {
    $markerOutput = & $preflightScript -ManifestPath $markerManifestPath -Json 2>&1
    $markerExitCode = $LASTEXITCODE
} catch {
    throw "preflightReadsVersionMarkerWithoutExecutingIt failed: preflight threw '$($_.Exception.Message)'."
}

$markerResult = ($markerOutput -join [Environment]::NewLine) | ConvertFrom-Json
Assert-Equal $markerExitCode 0 'preflightReadsVersionMarkerWithoutExecutingIt exit code'
Assert-Equal $markerResult.Status 'Ready' 'preflightReadsVersionMarkerWithoutExecutingIt status'
Assert-Equal $markerResult.DetectedVersion '2.3.2' 'preflightReadsVersionMarkerWithoutExecutingIt detected version'

Write-Output 'PASS preflightReadsVersionMarkerWithoutExecutingIt'

$argumentManifestPath = Join-Path $PSScriptRoot 'fixtures\argument-runtime.psd1'
try {
    $argumentOutput = & $preflightScript -ManifestPath $argumentManifestPath -Json 2>&1
    $argumentExitCode = $LASTEXITCODE
} catch {
    throw "preflightPreservesNonEmptyVersionArguments failed: preflight threw '$($_.Exception.Message)'."
}

$argumentResult = ($argumentOutput -join [Environment]::NewLine) | ConvertFrom-Json
Assert-Equal $argumentExitCode 0 'preflightPreservesNonEmptyVersionArguments exit code'
Assert-Equal $argumentResult.Status 'Ready' 'preflightPreservesNonEmptyVersionArguments status'
Assert-Equal $argumentResult.DetectedVersion '1.2.3' 'preflightPreservesNonEmptyVersionArguments detected version'

Write-Output 'PASS preflightPreservesNonEmptyVersionArguments'

$testArtifactRoot = "runtime-data/test-artifacts/$PID-required-directories"
$requiredDirectory = "$testArtifactRoot/required/data"
$requiredStatePath = "$testArtifactRoot/processes.json"
$requiredManifestPath = Join-Path $projectRoot "$testArtifactRoot/runtime.psd1"
$requiredManifestDirectory = Split-Path -Parent $requiredManifestPath
New-Item -ItemType Directory -Path $requiredManifestDirectory -Force | Out-Null
@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'RequiredDirectoryDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-Command', 'Start-Sleep -Seconds 30')
            CommandMarker = 'required-directory-dummy'
            RequiredDirectories = @('$requiredDirectory')
        }
    )
    StateFile = '$requiredStatePath'
}
"@ | Set-Content -LiteralPath $requiredManifestPath -Encoding utf8

$requiredDirectoryPid = $null
try {
    $requiredOutput = & $startScript -ManifestPath $requiredManifestPath -Json 2>&1
    $requiredExitCode = $LASTEXITCODE
    $requiredResult = ($requiredOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $requiredDirectoryPid = [int] $requiredResult.Pid

    Assert-Equal $requiredExitCode 0 'startCreatesRequiredDirectoriesBeforeLaunch exit code'
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $requiredDirectory) -PathType Container) 'startCreatesRequiredDirectoriesBeforeLaunch must create the declared directory'
} finally {
    if ($null -ne $requiredDirectoryPid) {
        Stop-Process -Id $requiredDirectoryPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS startCreatesRequiredDirectoriesBeforeLaunch'

$partialArtifactRoot = "runtime-data/test-artifacts/$PID-partial-failure"
$partialPidFile = "$partialArtifactRoot/first.pid"
$partialStatePath = "$partialArtifactRoot/processes.json"
$partialManifestPath = Join-Path $projectRoot "$partialArtifactRoot/runtime.psd1"
New-Item -ItemType Directory -Path (Split-Path -Parent $partialManifestPath) -Force | Out-Null
@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'FirstDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-File', 'infra/tests/fixtures/record-pid-and-wait.ps1', '-PidFile', '$partialPidFile')
            CommandMarker = 'partial-first-dummy'
            RequiredDirectories = @('$partialArtifactRoot')
        }
        @{
            Name = 'MissingSecondDummy'
            FilePath = 'infra/runtime/does-not-exist/missing.exe'
            Arguments = @()
            CommandMarker = 'partial-missing-dummy'
        }
    )
    StateFile = '$partialStatePath'
}
"@ | Set-Content -LiteralPath $partialManifestPath -Encoding utf8

$partialStartFailed = $false
$partialFirstPid = $null
try {
    try {
        & $startScript -ManifestPath $partialManifestPath -Json 2>&1 | Out-Null
    } catch {
        $partialStartFailed = $true
    }

    $partialPidFilePath = Join-Path $projectRoot $partialPidFile
    $partialPidDeadline = [DateTimeOffset]::UtcNow.AddSeconds(5)
    while (-not (Test-Path -LiteralPath $partialPidFilePath -PathType Leaf) -and [DateTimeOffset]::UtcNow -lt $partialPidDeadline) {
        Start-Sleep -Milliseconds 50
    }
    if (Test-Path -LiteralPath $partialPidFilePath -PathType Leaf) {
        $partialFirstPid = [int] (Get-Content -Raw -LiteralPath $partialPidFilePath)
    }

    Assert-True $partialStartFailed 'startPersistsEachSuccessfulProcessBeforeLaterFailure must report the later launch failure'
    $partialStateFilePath = Join-Path $projectRoot $partialStatePath
    Assert-True (Test-Path -LiteralPath $partialStateFilePath -PathType Leaf) 'startPersistsEachSuccessfulProcessBeforeLaterFailure must persist state before starting the next process'
    $partialState = Get-Content -Raw -LiteralPath $partialStateFilePath | ConvertFrom-Json
    Assert-Equal ([int] $partialState.Processes.FirstDummy.Pid) $partialFirstPid 'startPersistsEachSuccessfulProcessBeforeLaterFailure persisted PID'
} finally {
    if ($null -ne $partialFirstPid) {
        Stop-Process -Id $partialFirstPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS startPersistsEachSuccessfulProcessBeforeLaterFailure'

$mutexArtifactRoot = "runtime-data/test-artifacts/$PID-mutex"
$mutexStatePath = "$mutexArtifactRoot/processes.json"
$mutexManifestPath = Join-Path $projectRoot "$mutexArtifactRoot/runtime.psd1"
$mutexName = "Local\MusicDreamerRuntimeTests-$PID"
New-Item -ItemType Directory -Path (Split-Path -Parent $mutexManifestPath) -Force | Out-Null
@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'MutexDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-Command', 'Start-Sleep -Seconds 30')
            CommandMarker = 'mutex-dummy'
        }
    )
    StateFile = '$mutexStatePath'
    MutexName = '$mutexName'
    LockTimeoutSeconds = 1
}
"@ | Set-Content -LiteralPath $mutexManifestPath -Encoding utf8

$heldMutex = [System.Threading.Mutex]::new($false, $mutexName)
$mutexAcquired = $heldMutex.WaitOne([TimeSpan]::FromSeconds(5))
$mutexChildPid = $null
try {
    Assert-True $mutexAcquired 'lifecycleMutexBlocksConcurrentMutation test setup must acquire the mutex'
    $mutexStartProcess = Start-Process `
        -FilePath 'pwsh' `
        -ArgumentList @('-NoProfile', '-File', $startScript, '-ManifestPath', $mutexManifestPath, '-Json') `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -PassThru
    $mutexStartExited = $mutexStartProcess.WaitForExit(10000)
    Assert-True $mutexStartExited 'lifecycleMutexBlocksConcurrentMutation start wrapper must exit within the test timeout'

    $mutexStateFilePath = Join-Path $projectRoot $mutexStatePath
    if (Test-Path -LiteralPath $mutexStateFilePath -PathType Leaf) {
        $mutexState = Get-Content -Raw -LiteralPath $mutexStateFilePath | ConvertFrom-Json
        if ($null -ne $mutexState.Processes.MutexDummy.Pid) {
            $mutexChildPid = [int] $mutexState.Processes.MutexDummy.Pid
        }
    }

    Assert-Equal $mutexStartProcess.ExitCode 1 'lifecycleMutexBlocksConcurrentMutation exit code'
    Assert-True (-not (Test-Path -LiteralPath $mutexStateFilePath -PathType Leaf)) 'lifecycleMutexBlocksConcurrentMutation must not write state while another lifecycle operation holds the lock'
} finally {
    if ($mutexAcquired) {
        $heldMutex.ReleaseMutex()
    }
    $heldMutex.Dispose()
    if ($null -ne $mutexChildPid) {
        Stop-Process -Id $mutexChildPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS lifecycleMutexBlocksConcurrentMutation'

$stopMutexStartOutput = & $startScript -ManifestPath $mutexManifestPath -Json 2>&1
$stopMutexStartResult = ($stopMutexStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
$stopMutexPid = [int] $stopMutexStartResult.Pid
$stopMutex = [System.Threading.Mutex]::new($false, $mutexName)
$stopMutexAcquired = $stopMutex.WaitOne([TimeSpan]::FromSeconds(5))
$stopWhileLockedExitCode = $null
$stopWhileLockedProcessAlive = $false
try {
    Assert-True $stopMutexAcquired 'lifecycleMutexBlocksConcurrentStop test setup must acquire the mutex'
    $mutexStopProcess = Start-Process `
        -FilePath 'pwsh' `
        -ArgumentList @('-NoProfile', '-File', $stopScript, '-ManifestPath', $mutexManifestPath, '-Json') `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -PassThru
    $mutexStopExited = $mutexStopProcess.WaitForExit(10000)
    Assert-True $mutexStopExited 'lifecycleMutexBlocksConcurrentStop stop wrapper must exit within the test timeout'
    $stopWhileLockedExitCode = $mutexStopProcess.ExitCode
    $stopWhileLockedProcessAlive = $null -ne (Get-Process -Id $stopMutexPid -ErrorAction SilentlyContinue)
} finally {
    if ($stopMutexAcquired) {
        $stopMutex.ReleaseMutex()
    }
    $stopMutex.Dispose()
    if ($null -ne (Get-Process -Id $stopMutexPid -ErrorAction SilentlyContinue)) {
        & $stopScript -ManifestPath $mutexManifestPath -Json 2>&1 | Out-Null
    }
}

Assert-Equal $stopWhileLockedExitCode 1 'lifecycleMutexBlocksConcurrentStop exit code'
Assert-True $stopWhileLockedProcessAlive 'lifecycleMutexBlocksConcurrentStop must leave the managed process running while the lock is held'

Write-Output 'PASS lifecycleMutexBlocksConcurrentStop'

$unownedStartArtifactRoot = "runtime-data/test-artifacts/$PID-unowned-start"
$unownedStartStatePath = "$unownedStartArtifactRoot/processes.json"
$unownedStartManifestPath = Join-Path $projectRoot "$unownedStartArtifactRoot/runtime.psd1"
New-Item -ItemType Directory -Path (Split-Path -Parent $unownedStartManifestPath) -Force | Out-Null
@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'UnownedStartDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-Command', 'Start-Sleep -Seconds 30')
            CommandMarker = 'unowned-start-dummy'
        }
    )
    StateFile = '$unownedStartStatePath'
}
"@ | Set-Content -LiteralPath $unownedStartManifestPath -Encoding utf8

$unownedOriginalPid = $null
$unownedReplacementPid = $null
try {
    $unownedOriginalOutput = & $startScript -ManifestPath $unownedStartManifestPath -Json 2>&1
    $unownedOriginalResult = ($unownedOriginalOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $unownedOriginalPid = [int] $unownedOriginalResult.Pid

    $unownedStartStateFilePath = Join-Path $projectRoot $unownedStartStatePath
    $unownedStartState = Get-Content -Raw -LiteralPath $unownedStartStateFilePath | ConvertFrom-Json
    $unownedStartState.Processes.UnownedStartDummy.CommandMarker = 'forged-command-marker'
    $unownedStartState | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $unownedStartStateFilePath -Encoding utf8

    $unownedSecondOutput = & $startScript -ManifestPath $unownedStartManifestPath -Json 2>&1
    $unownedSecondExitCode = $LASTEXITCODE
    if ($unownedSecondExitCode -eq 0) {
        $unownedSecondResult = ($unownedSecondOutput -join [Environment]::NewLine) | ConvertFrom-Json
        $unownedReplacementPid = [int] $unownedSecondResult.Pid
    }

    Assert-Equal $unownedSecondExitCode 1 'startRefusesUnownedRecordedPid exit code'
    Assert-True ($null -ne (Get-Process -Id $unownedOriginalPid -ErrorAction SilentlyContinue)) 'startRefusesUnownedRecordedPid must leave the unowned process untouched'
    Assert-Equal $unownedReplacementPid $null 'startRefusesUnownedRecordedPid must not launch a replacement process'
} finally {
    if ($null -ne $unownedOriginalPid) {
        Stop-Process -Id $unownedOriginalPid -Force -ErrorAction SilentlyContinue
    }
    if ($null -ne $unownedReplacementPid) {
        Stop-Process -Id $unownedReplacementPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS startRefusesUnownedRecordedPid'

$portProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$portProbe.Start()
$httpLifecyclePort = ([System.Net.IPEndPoint] $portProbe.LocalEndpoint).Port
$portProbe.Stop()
$httpArtifactRoot = "runtime-data/test-artifacts/$PID-http-lifecycle"
$httpStatePath = "$httpArtifactRoot/processes.json"
$httpShutdownMarker = "$httpArtifactRoot/shutdown.marker"
$httpManifestPath = Join-Path $projectRoot "$httpArtifactRoot/runtime.psd1"
New-Item -ItemType Directory -Path (Split-Path -Parent $httpManifestPath) -Force | Out-Null
@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'HttpDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-File', 'infra/tests/fixtures/http-lifecycle-server.ps1', '-Port', '$httpLifecyclePort', '-ShutdownMarker', '$httpShutdownMarker')
            CommandMarker = 'http-lifecycle-dummy'
            Readiness = @{
                Type = 'Http'
                Uri = 'http://127.0.0.1:$httpLifecyclePort/ready'
                ExpectedContent = 'UP'
                TimeoutSeconds = 10
                IntervalMilliseconds = 100
            }
            Stop = @{
                Type = 'Http'
                Uri = 'http://127.0.0.1:$httpLifecyclePort/shutdown'
                Method = 'POST'
                TimeoutSeconds = 5
            }
        }
    )
    StateFile = '$httpStatePath'
}
"@ | Set-Content -LiteralPath $httpManifestPath -Encoding utf8

$httpLifecyclePid = $null
try {
    $httpStartOutput = & $startScript -ManifestPath $httpManifestPath -Json 2>&1
    $httpStartExitCode = $LASTEXITCODE
    $httpStartResult = ($httpStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $httpLifecyclePid = [int] $httpStartResult.Pid

    Assert-Equal $httpStartExitCode 0 'startWaitsForHttpReadiness exit code'
    Assert-Equal $httpStartResult.Status 'Ready' 'startWaitsForHttpReadiness status'
    Assert-Equal $httpStartResult.Ready $true 'startWaitsForHttpReadiness ready flag'
} finally {
    if ($null -ne $httpLifecyclePid) {
        Stop-Process -Id $httpLifecyclePid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS startWaitsForHttpReadiness'

$httpStatusPid = $null
try {
    $httpStatusStartOutput = & $startScript -ManifestPath $httpManifestPath -Json 2>&1
    $httpStatusStartResult = ($httpStatusStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $httpStatusPid = [int] $httpStatusStartResult.Pid

    $httpStatusOutput = & $statusScript -ManifestPath $httpManifestPath -Json 2>&1
    $httpStatusExitCode = $LASTEXITCODE
    $httpStatusResult = ($httpStatusOutput -join [Environment]::NewLine) | ConvertFrom-Json

    Assert-Equal $httpStatusExitCode 0 'statusReportsHttpReadiness exit code'
    Assert-Equal $httpStatusResult.Status 'Ready' 'statusReportsHttpReadiness status'
    Assert-Equal $httpStatusResult.Ready $true 'statusReportsHttpReadiness ready flag'
} finally {
    if ($null -ne $httpStatusPid) {
        Stop-Process -Id $httpStatusPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS statusReportsHttpReadiness'

$httpStopPid = $null
try {
    $httpStopStartOutput = & $startScript -ManifestPath $httpManifestPath -Json 2>&1
    $httpStopStartResult = ($httpStopStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $httpStopPid = [int] $httpStopStartResult.Pid

    $httpStopOutput = & $stopScript -ManifestPath $httpManifestPath -Json 2>&1
    $httpStopExitCode = $LASTEXITCODE
    $httpStopResult = ($httpStopOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $httpStopMethodProperty = $httpStopResult.PSObject.Properties['StopMethod']
    $httpStopMethod = if ($null -eq $httpStopMethodProperty) { $null } else { $httpStopMethodProperty.Value }

    $httpStopDeadline = [DateTimeOffset]::UtcNow.AddSeconds(5)
    while ($null -ne (Get-Process -Id $httpStopPid -ErrorAction SilentlyContinue) -and [DateTimeOffset]::UtcNow -lt $httpStopDeadline) {
        Start-Sleep -Milliseconds 50
    }

    Assert-Equal $httpStopExitCode 0 'stopUsesHttpGracefulShutdown exit code'
    Assert-Equal $httpStopMethod 'Graceful' 'stopUsesHttpGracefulShutdown method'
    Assert-True (Test-Path -LiteralPath (Join-Path $projectRoot $httpShutdownMarker) -PathType Leaf) 'stopUsesHttpGracefulShutdown must call the declared shutdown endpoint'
    Assert-True ($null -eq (Get-Process -Id $httpStopPid -ErrorAction SilentlyContinue)) 'stopUsesHttpGracefulShutdown process must exit'
} finally {
    if ($null -ne $httpStopPid) {
        Stop-Process -Id $httpStopPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS stopUsesHttpGracefulShutdown'

$redisPortProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$redisPortProbe.Start()
$realRedisPort = ([System.Net.IPEndPoint] $redisPortProbe.LocalEndpoint).Port
$redisPortProbe.Stop()
$realRedisArtifactRoot = "runtime-data/test-artifacts/$PID-real-redis"
$realRedisConfigPath = "$realRedisArtifactRoot/redis.conf"
$realRedisDataPath = "$realRedisArtifactRoot/data"
$realRedisLogPath = "$realRedisArtifactRoot/redis.log"
$realRedisStatePath = "$realRedisArtifactRoot/processes.json"
$realRedisManifestPath = Join-Path $projectRoot "$realRedisArtifactRoot/runtime.psd1"
$realRedisServerPath = 'infra/runtime/Redis-8.10.0-Windows-x64-msys2/redis-server.exe'
$realRedisCliPath = 'infra/runtime/Redis-8.10.0-Windows-x64-msys2/redis-cli.exe'
New-Item -ItemType Directory -Path (Split-Path -Parent $realRedisManifestPath) -Force | Out-Null
@"
bind 127.0.0.1
protected-mode yes
port $realRedisPort
daemonize no
dir $realRedisDataPath
dbfilename dump.rdb
appendonly yes
appendfilename "appendonly.aof"
logfile "../redis.log"
"@ | Set-Content -LiteralPath (Join-Path $projectRoot $realRedisConfigPath) -Encoding utf8
@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'RealRedis'
            FilePath = '$realRedisServerPath'
            Arguments = @('$realRedisConfigPath')
            CommandMarker = 'real-redis-lifecycle-test'
            RequiredDirectories = @('$realRedisDataPath')
            Readiness = @{
                Type = 'Redis'
                ClientPath = '$realRedisCliPath'
                Host = '127.0.0.1'
                Port = $realRedisPort
                TimeoutSeconds = 10
                IntervalMilliseconds = 100
            }
            Stop = @{
                Type = 'Redis'
                ClientPath = '$realRedisCliPath'
                Host = '127.0.0.1'
                Port = $realRedisPort
                TimeoutSeconds = 5
            }
        }
    )
    StateFile = '$realRedisStatePath'
}
"@ | Set-Content -LiteralPath $realRedisManifestPath -Encoding utf8

$realRedisPid = $null
$realRedisStartError = $null
$realRedisStartExitCode = $null
$realRedisStartResult = $null
try {
    try {
        $realRedisStartOutput = & $startScript -ManifestPath $realRedisManifestPath -Json 2>&1
        $realRedisStartExitCode = $LASTEXITCODE
        $realRedisStartResult = ($realRedisStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
        $realRedisPid = [int] $realRedisStartResult.Pid
    } catch {
        $realRedisStartError = $_.Exception.Message
        $realRedisStateFilePath = Join-Path $projectRoot $realRedisStatePath
        if (Test-Path -LiteralPath $realRedisStateFilePath -PathType Leaf) {
            $realRedisFailedState = Get-Content -Raw -LiteralPath $realRedisStateFilePath | ConvertFrom-Json
            $realRedisPid = [int] $realRedisFailedState.Processes.RealRedis.Pid
        }
    }
} finally {
    if ($null -ne $realRedisPid) {
        Stop-Process -Id $realRedisPid -Force -ErrorAction SilentlyContinue
    }
}

Assert-Equal $realRedisStartError $null 'startWaitsForRealRedisPing error'
Assert-Equal $realRedisStartExitCode 0 'startWaitsForRealRedisPing exit code'
Assert-Equal $realRedisStartResult.Status 'Ready' 'startWaitsForRealRedisPing status'
Assert-Equal $realRedisStartResult.Ready $true 'startWaitsForRealRedisPing ready flag'

Write-Output 'PASS startWaitsForRealRedisPing'

$realRedisLifecyclePid = $null
$realRedisLifecycleError = $null
try {
    try {
        $realRedisLifecycleStartOutput = & $startScript -ManifestPath $realRedisManifestPath -Json 2>&1
        $realRedisLifecycleStartResult = ($realRedisLifecycleStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
        $realRedisLifecyclePid = [int] $realRedisLifecycleStartResult.Pid

        $realRedisStatusOutput = & $statusScript -ManifestPath $realRedisManifestPath -Json 2>&1
        $realRedisStatusResult = ($realRedisStatusOutput -join [Environment]::NewLine) | ConvertFrom-Json
        Assert-Equal $realRedisStatusResult.Status 'Ready' 'realRedisLifecyclePersistsAcrossGracefulRestart status'
        Assert-Equal $realRedisStatusResult.Ready $true 'realRedisLifecyclePersistsAcrossGracefulRestart ready flag'

        $realRedisCli = Join-Path $projectRoot $realRedisCliPath
        $redisSetOutput = & $realRedisCli -h 127.0.0.1 -p $realRedisPort SET 'music-dreamer:p0.3:lifecycle' 'persisted' 2>&1
        Assert-Equal $LASTEXITCODE 0 'realRedisLifecyclePersistsAcrossGracefulRestart SET exit code'
        Assert-Equal ($redisSetOutput -join [Environment]::NewLine).Trim() 'OK' 'realRedisLifecyclePersistsAcrossGracefulRestart SET result'

        $realRedisStopOutput = & $stopScript -ManifestPath $realRedisManifestPath -Json 2>&1
        $realRedisStopResult = ($realRedisStopOutput -join [Environment]::NewLine) | ConvertFrom-Json
        Assert-Equal $LASTEXITCODE 0 'realRedisLifecyclePersistsAcrossGracefulRestart first stop exit code'
        Assert-Equal $realRedisStopResult.StopMethod 'Graceful' 'realRedisLifecyclePersistsAcrossGracefulRestart first stop method'
        $realRedisLifecyclePid = $null

        $realRedisRestartOutput = & $startScript -ManifestPath $realRedisManifestPath -Json 2>&1
        $realRedisRestartResult = ($realRedisRestartOutput -join [Environment]::NewLine) | ConvertFrom-Json
        $realRedisLifecyclePid = [int] $realRedisRestartResult.Pid
        Assert-Equal $realRedisRestartResult.Status 'Ready' 'realRedisLifecyclePersistsAcrossGracefulRestart restart status'

        $redisGetOutput = & $realRedisCli -h 127.0.0.1 -p $realRedisPort GET 'music-dreamer:p0.3:lifecycle' 2>&1
        Assert-Equal $LASTEXITCODE 0 'realRedisLifecyclePersistsAcrossGracefulRestart GET exit code'
        Assert-Equal ($redisGetOutput -join [Environment]::NewLine).Trim() 'persisted' 'realRedisLifecyclePersistsAcrossGracefulRestart persisted value'

        $realRedisFinalStopOutput = & $stopScript -ManifestPath $realRedisManifestPath -Json 2>&1
        $realRedisFinalStopResult = ($realRedisFinalStopOutput -join [Environment]::NewLine) | ConvertFrom-Json
        Assert-Equal $LASTEXITCODE 0 'realRedisLifecyclePersistsAcrossGracefulRestart final stop exit code'
        Assert-Equal $realRedisFinalStopResult.StopMethod 'Graceful' 'realRedisLifecyclePersistsAcrossGracefulRestart final stop method'
        $realRedisLifecyclePid = $null
    } catch {
        $realRedisLifecycleError = $_.Exception.Message
        $realRedisStateFilePath = Join-Path $projectRoot $realRedisStatePath
        if (Test-Path -LiteralPath $realRedisStateFilePath -PathType Leaf) {
            $realRedisLifecycleState = Get-Content -Raw -LiteralPath $realRedisStateFilePath | ConvertFrom-Json
            if ($null -ne $realRedisLifecycleState.Processes.RealRedis.Pid) {
                $realRedisLifecyclePid = [int] $realRedisLifecycleState.Processes.RealRedis.Pid
            }
        }
    }
} finally {
    if ($null -ne $realRedisLifecyclePid) {
        Stop-Process -Id $realRedisLifecyclePid -Force -ErrorAction SilentlyContinue
    }
}

Assert-Equal $realRedisLifecycleError $null 'realRedisLifecyclePersistsAcrossGracefulRestart error'

Write-Output 'PASS realRedisLifecyclePersistsAcrossGracefulRestart'

$bytePortProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$bytePortProbe.Start()
$byteContentPort = ([System.Net.IPEndPoint] $bytePortProbe.LocalEndpoint).Port
$bytePortProbe.Stop()
$byteArtifactRoot = "runtime-data/test-artifacts/$PID-http-byte-content"
$byteStatePath = "$byteArtifactRoot/processes.json"
$byteManifestPath = Join-Path $projectRoot "$byteArtifactRoot/runtime.psd1"
New-Item -ItemType Directory -Path (Split-Path -Parent $byteManifestPath) -Force | Out-Null
@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'HttpByteDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-File', 'infra/tests/fixtures/http-lifecycle-server.ps1', '-Port', '$byteContentPort', '-ReadyBody', 'UP_BYTES', '-ContentType', 'application/vnd.spring-boot.actuator.v3+json')
            CommandMarker = 'http-byte-content-dummy'
            Readiness = @{
                Type = 'Http'
                Uri = 'http://127.0.0.1:$byteContentPort/ready'
                ExpectedContent = 'UP_BYTES'
                TimeoutSeconds = 5
                IntervalMilliseconds = 100
            }
        }
    )
    StateFile = '$byteStatePath'
}
"@ | Set-Content -LiteralPath $byteManifestPath -Encoding utf8

$byteContentPid = $null
$byteContentStartError = $null
$byteContentStartResult = $null
try {
    try {
        $byteContentOutput = & $startScript -ManifestPath $byteManifestPath -Json 2>&1
        $byteContentStartResult = ($byteContentOutput -join [Environment]::NewLine) | ConvertFrom-Json
        $byteContentPid = [int] $byteContentStartResult.Pid
    } catch {
        $byteContentStartError = $_.Exception.Message
        $byteStateFilePath = Join-Path $projectRoot $byteStatePath
        if (Test-Path -LiteralPath $byteStateFilePath -PathType Leaf) {
            $byteFailedState = Get-Content -Raw -LiteralPath $byteStateFilePath | ConvertFrom-Json
            $byteContentPid = [int] $byteFailedState.Processes.HttpByteDummy.Pid
        }
    }
} finally {
    if ($null -ne $byteContentPid) {
        Stop-Process -Id $byteContentPid -Force -ErrorAction SilentlyContinue
    }
}

Assert-Equal $byteContentStartError $null 'startDecodesHttpByteContentForReadiness error'
Assert-Equal $byteContentStartResult.Status 'Ready' 'startDecodesHttpByteContentForReadiness status'

Write-Output 'PASS startDecodesHttpByteContentForReadiness'

$jsonStopPortProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$jsonStopPortProbe.Start()
$jsonStopPort = ([System.Net.IPEndPoint] $jsonStopPortProbe.LocalEndpoint).Port
$jsonStopPortProbe.Stop()
$jsonStopArtifactRoot = "runtime-data/test-artifacts/$PID-http-json-stop"
$jsonStopStatePath = "$jsonStopArtifactRoot/processes.json"
$jsonStopManifestPath = Join-Path $projectRoot "$jsonStopArtifactRoot/runtime.psd1"
New-Item -ItemType Directory -Path (Split-Path -Parent $jsonStopManifestPath) -Force | Out-Null
@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'HttpJsonStopDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-File', 'infra/tests/fixtures/http-lifecycle-server.ps1', '-Port', '$jsonStopPort', '-RequiredShutdownContentType', 'application/json', '-RequiredShutdownBody', '{}')
            CommandMarker = 'http-json-stop-dummy'
            Readiness = @{
                Type = 'Http'
                Uri = 'http://127.0.0.1:$jsonStopPort/ready'
                ExpectedContent = 'UP'
                TimeoutSeconds = 5
                IntervalMilliseconds = 100
            }
            Stop = @{
                Type = 'Http'
                Uri = 'http://127.0.0.1:$jsonStopPort/shutdown'
                Method = 'POST'
                ContentType = 'application/json'
                Body = '{}'
                TimeoutSeconds = 5
            }
        }
    )
    StateFile = '$jsonStopStatePath'
}
"@ | Set-Content -LiteralPath $jsonStopManifestPath -Encoding utf8

$jsonStopPid = $null
try {
    $jsonStopStartOutput = & $startScript -ManifestPath $jsonStopManifestPath -Json 2>&1
    $jsonStopStartResult = ($jsonStopStartOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $jsonStopPid = [int] $jsonStopStartResult.Pid

    $jsonStopOutput = & $stopScript -ManifestPath $jsonStopManifestPath -Json 2>&1
    $jsonStopResult = ($jsonStopOutput -join [Environment]::NewLine) | ConvertFrom-Json
    Assert-Equal $LASTEXITCODE 0 'stopSendsDeclaredHttpJsonBody exit code'
    Assert-Equal $jsonStopResult.StopMethod 'Graceful' 'stopSendsDeclaredHttpJsonBody method'
} finally {
    if ($null -ne $jsonStopPid) {
        Stop-Process -Id $jsonStopPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS stopSendsDeclaredHttpJsonBody'

. (Join-Path $projectRoot 'infra\scripts\runtime-common.ps1')
$invalidOwnershipSpec = @{
    Name = 'InvalidOwnershipDummy'
    CommandMarker = 'invalid-ownership-dummy'
}
$invalidOwnershipState = [pscustomobject]@{
    Version = 1
    Processes = [pscustomobject]@{
        InvalidOwnershipDummy = [pscustomobject]@{
            Pid = $PID
            ExecutablePath = ''
            StartedAtUtc = (Get-Process -Id $PID).StartTime.ToUniversalTime().ToString('O')
            CommandMarker = 'invalid-ownership-dummy'
        }
    }
}
$invalidOwnershipError = $null
$invalidOwnershipResult = $null
try {
    $invalidOwnershipResult = Get-ManagedProcessStatus -ProcessSpec $invalidOwnershipSpec -State $invalidOwnershipState
} catch {
    $invalidOwnershipError = $_.Exception.Message
}

Assert-Equal $invalidOwnershipError $null 'statusTreatsInvalidExecutablePathAsUnowned error'
Assert-Equal $invalidOwnershipResult.Status 'Unowned' 'statusTreatsInvalidExecutablePathAsUnowned status'
Assert-Equal $invalidOwnershipResult.Owned $false 'statusTreatsInvalidExecutablePathAsUnowned ownership flag'

Write-Output 'PASS statusTreatsInvalidExecutablePathAsUnowned'
