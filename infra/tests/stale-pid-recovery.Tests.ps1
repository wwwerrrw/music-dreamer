#requires -Version 7.0

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
. (Join-Path $projectRoot 'infra\scripts\runtime-common.ps1')

$portProbe = [System.Net.Sockets.TcpListener]::new(
    [System.Net.IPAddress]::Loopback,
    0)
$portProbe.Start()
$unusedPort = ([System.Net.IPEndPoint] $portProbe.LocalEndpoint).Port
$portProbe.Stop()

$processSpec = @{
    Name = 'StaleReadyProcess'
    CommandMarker = 'stale-ready-process'
    Readiness = @{
        Type = 'Tcp'
        Host = '127.0.0.1'
        Port = $unusedPort
        RequestTimeoutSeconds = 0.1
    }
}
$existingStatus = [pscustomobject]@{
    Name = 'StaleReadyProcess'
    Status = 'Unowned'
    Pid = $PID
    Owned = $false
    ExecutablePath = (Get-Process -Id $PID).Path
}
$recordedState = [pscustomobject]@{
    Status = 'Ready'
}

$allowed = Test-RuntimeProcessReplacementAllowed `
    -ProcessSpec $processSpec `
    -ExistingStatus $existingStatus `
    -RecordedState $recordedState

if (-not $allowed) {
    throw 'A stale Ready PID with a failed readiness check must allow a replacement process.'
}
if ($null -eq (Get-Process -Id $PID -ErrorAction SilentlyContinue)) {
    throw 'The reused PID process must not be terminated.'
}

Write-Output 'PASS staleReadyPidAllowsSafeReplacement'

$artifactRoot = "runtime-data/test-artifacts/$PID-state-preservation"
$statePath = "$artifactRoot/processes.json"
$manifestPath = Join-Path $projectRoot "$artifactRoot/runtime.psd1"
$resolvedStatePath = Join-Path $projectRoot $statePath
New-Item -ItemType Directory -Path (Split-Path -Parent $manifestPath) -Force | Out-Null

@"
@{
    Components = @()
    Processes = @(
        @{
            Name = 'FirstDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-Command', 'Start-Sleep -Seconds 30')
            CommandMarker = 'first-dummy'
        }
        @{
            Name = 'UnownedLaterDummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-Command', 'Start-Sleep -Seconds 30')
            CommandMarker = 'unowned-later-dummy'
        }
    )
    StateFile = '$statePath'
}
"@ | Set-Content -LiteralPath $manifestPath -Encoding utf8

$initialState = [ordered]@{
    Version = 1
    UpdatedAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
    Processes = [ordered]@{
        UnownedLaterDummy = [ordered]@{
            Name = 'UnownedLaterDummy'
            Pid = $PID
            Status = 'Running'
            ExecutablePath = ''
            StartedAtUtc = (Get-Process -Id $PID).StartTime.ToUniversalTime().ToString('O')
            CommandMarker = 'unowned-later-dummy'
        }
    }
}
$initialState | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $resolvedStatePath -Encoding utf8

$firstDummyPid = $null
try {
    & (Join-Path $projectRoot 'infra\scripts\start.ps1') -ManifestPath $manifestPath -Json 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 1) {
        throw "Expected the later unowned process to refuse startup, got exit code '$LASTEXITCODE'."
    }

    $persistedState = Get-Content -Raw -LiteralPath $resolvedStatePath | ConvertFrom-Json
    $firstDummyPid = [int] $persistedState.Processes.FirstDummy.Pid
    if ($null -eq $persistedState.Processes.UnownedLaterDummy) {
        throw 'A later unprocessed process state must survive an earlier successful launch.'
    }
    if ($null -eq (Get-Process -Id $PID -ErrorAction SilentlyContinue)) {
        throw 'The unowned reused PID process must not be terminated.'
    }
} finally {
    if ($null -ne $firstDummyPid) {
        Stop-Process -Id $firstDummyPid -Force -ErrorAction SilentlyContinue
    }
}

Write-Output 'PASS partialStartPreservesUnprocessedState'
