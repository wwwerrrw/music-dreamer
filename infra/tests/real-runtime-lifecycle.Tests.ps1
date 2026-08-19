#requires -Version 7.0

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$manifestPath = Join-Path $projectRoot 'infra\config\runtime-manifest.psd1'
$startScript = Join-Path $projectRoot 'infra\scripts\start.ps1'
$statusScript = Join-Path $projectRoot 'infra\scripts\status.ps1'
$stopScript = Join-Path $projectRoot 'infra\scripts\stop.ps1'
$redisCli = Join-Path $projectRoot 'infra\runtime\Redis-8.10.0-Windows-x64-msys2\redis-cli.exe'
$stateFile = Join-Path $projectRoot 'runtime-data\processes.json'
$nacosConfigUri = 'http://127.0.0.1:8848/nacos/v1/cs/configs'
$nacosConfigIdentity = @{
    dataId = 'music-dreamer-p0.3-lifecycle'
    group = 'DEFAULT_GROUP'
}

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

function ConvertTo-ResultMap {
    param(
        [Parameter(Mandatory)]
        [array] $Results
    )

    $map = @{}
    foreach ($result in $Results) {
        $map[$result.Name] = $result
    }
    return $map
}

function Stop-TestRuntime {
    try {
        & $stopScript -ManifestPath $manifestPath -Json 2>&1 | Out-Null
    } catch {
    }

    if (-not (Test-Path -LiteralPath $stateFile -PathType Leaf)) {
        return
    }

    $statusOutput = & $statusScript -ManifestPath $manifestPath -Json 2>&1
    $statusResults = @(($statusOutput -join [Environment]::NewLine) | ConvertFrom-Json)
    foreach ($statusResult in $statusResults) {
        if ($statusResult.Owned -and $statusResult.Status -in @('Running', 'Ready', 'Unready')) {
            Stop-Process -Id ([int] $statusResult.Pid) -Force -ErrorAction SilentlyContinue
        }
    }
}

try {
    $startOutput = & $startScript -ManifestPath $manifestPath -Json 2>&1
    $startExitCode = $LASTEXITCODE
    $startResults = @(($startOutput -join [Environment]::NewLine) | ConvertFrom-Json)
    $started = ConvertTo-ResultMap $startResults

    Assert-Equal $startExitCode 0 'realRuntimeLifecycle start exit code'
    Assert-Equal $started.Redis.Status 'Ready' 'realRuntimeLifecycle Redis start status'
    Assert-Equal $started.Redis.Ready $true 'realRuntimeLifecycle Redis start ready flag'
    Assert-Equal $started.Nacos.Status 'Ready' 'realRuntimeLifecycle Nacos start status'
    Assert-Equal $started.Nacos.Ready $true 'realRuntimeLifecycle Nacos start ready flag'
    Assert-True $started.Nacos.ExecutablePath.EndsWith('\bin\java.exe') 'realRuntimeLifecycle Nacos must track the Java process'

    $statusOutput = & $statusScript -ManifestPath $manifestPath -Json 2>&1
    $statusExitCode = $LASTEXITCODE
    $statusResults = @(($statusOutput -join [Environment]::NewLine) | ConvertFrom-Json)
    $statuses = ConvertTo-ResultMap $statusResults
    Assert-Equal $statusExitCode 0 'realRuntimeLifecycle status exit code'
    Assert-Equal $statuses.Redis.Status 'Ready' 'realRuntimeLifecycle Redis status'
    Assert-Equal $statuses.Nacos.Status 'Ready' 'realRuntimeLifecycle Nacos status'

    $redisSetOutput = & $redisCli -h 127.0.0.1 -p 6380 SET 'music-dreamer:p0.3:lifecycle' 'persisted' 2>&1
    Assert-Equal $LASTEXITCODE 0 'realRuntimeLifecycle Redis SET exit code'
    Assert-Equal ($redisSetOutput -join [Environment]::NewLine).Trim() 'OK' 'realRuntimeLifecycle Redis SET result'

    $nacosPublishBody = @{} + $nacosConfigIdentity
    $nacosPublishBody.content = 'persisted'
    $nacosPublishResult = Invoke-RestMethod -Uri $nacosConfigUri -Method Post -Body $nacosPublishBody -ContentType 'application/x-www-form-urlencoded'
    Assert-Equal $nacosPublishResult $true 'realRuntimeLifecycle Nacos publish result'

    $stopOutput = & $stopScript -ManifestPath $manifestPath -Json 2>&1
    $stopExitCode = $LASTEXITCODE
    $stopResults = @(($stopOutput -join [Environment]::NewLine) | ConvertFrom-Json)
    $stopped = ConvertTo-ResultMap $stopResults
    Assert-Equal $stopExitCode 0 'realRuntimeLifecycle first stop exit code'
    Assert-Equal $stopped.Redis.StopMethod 'Graceful' 'realRuntimeLifecycle Redis first stop method'
    Assert-Equal $stopped.Nacos.StopMethod 'GracefulThenForced' 'realRuntimeLifecycle Nacos first stop method'
    Assert-Equal $stopped.Nacos.GracefulRequestSucceeded $true 'realRuntimeLifecycle Nacos first graceful request'

    $restartOutput = & $startScript -ManifestPath $manifestPath -Json 2>&1
    $restartExitCode = $LASTEXITCODE
    $restartResults = @(($restartOutput -join [Environment]::NewLine) | ConvertFrom-Json)
    $restarted = ConvertTo-ResultMap $restartResults
    Assert-Equal $restartExitCode 0 'realRuntimeLifecycle restart exit code'
    Assert-Equal $restarted.Redis.Status 'Ready' 'realRuntimeLifecycle Redis restart status'
    Assert-Equal $restarted.Nacos.Status 'Ready' 'realRuntimeLifecycle Nacos restart status'

    $redisGetOutput = & $redisCli -h 127.0.0.1 -p 6380 GET 'music-dreamer:p0.3:lifecycle' 2>&1
    Assert-Equal $LASTEXITCODE 0 'realRuntimeLifecycle Redis GET exit code'
    Assert-Equal ($redisGetOutput -join [Environment]::NewLine).Trim() 'persisted' 'realRuntimeLifecycle Redis persisted value'

    $nacosPersisted = Invoke-RestMethod -Uri $nacosConfigUri -Method Get -Body $nacosConfigIdentity
    Assert-Equal $nacosPersisted 'persisted' 'realRuntimeLifecycle Nacos persisted value'

    $finalStopOutput = & $stopScript -ManifestPath $manifestPath -Json 2>&1
    $finalStopExitCode = $LASTEXITCODE
    $finalStopResults = @(($finalStopOutput -join [Environment]::NewLine) | ConvertFrom-Json)
    $finalStopped = ConvertTo-ResultMap $finalStopResults
    Assert-Equal $finalStopExitCode 0 'realRuntimeLifecycle final stop exit code'
    Assert-Equal $finalStopped.Redis.StopMethod 'Graceful' 'realRuntimeLifecycle Redis final stop method'
    Assert-Equal $finalStopped.Nacos.StopMethod 'GracefulThenForced' 'realRuntimeLifecycle Nacos final stop method'
    Assert-Equal $finalStopped.Nacos.GracefulRequestSucceeded $true 'realRuntimeLifecycle Nacos final graceful request'
} finally {
    Stop-TestRuntime
}

Write-Output 'PASS realRuntimeLifecyclePersistsAcrossGracefulRestart'
