#requires -Version 7.0

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$runtimeCommon = Join-Path $projectRoot 'infra\scripts\runtime-common.ps1'
$startScript = Join-Path $projectRoot 'infra\scripts\start.ps1'
$statusScript = Join-Path $projectRoot 'infra\scripts\status.ps1'
$stopScript = Join-Path $projectRoot 'infra\scripts\stop.ps1'
$applicationManifestPath = Join-Path $projectRoot 'infra\config\application-runtime-manifest.psd1'
$probeScript = Join-Path $projectRoot 'infra\tests\fixtures\application-runtime-probe.mjs'
$evidenceRoot = Join-Path $projectRoot 'test-results\p5-20260819-224207'

. $runtimeCommon

function Assert-True {
    param(
        [Parameter(Mandatory)]
        [bool] $Condition,

        [Parameter(Mandatory)]
        [string] $Name
    )

    if (-not $Condition) {
        throw "Assertion failed: $Name"
    }
    Write-Output "PASS $Name"
}

function Assert-Equal {
    param(
        [AllowNull()]
        $Actual,

        [AllowNull()]
        $Expected,

        [Parameter(Mandatory)]
        [string] $Name
    )

    if ($Actual -ne $Expected) {
        throw "Assertion failed: $Name (expected '$Expected', actual '$Actual')"
    }
    Write-Output "PASS $Name"
}

function Assert-RuntimeCommandExists {
    param(
        [Parameter(Mandatory)]
        [string] $Name
    )

    $command = Get-Command $Name -CommandType Function -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "Expected runtime function '$Name' is not implemented."
    }
    Write-Output "PASS functionExists:$Name"
}

Assert-RuntimeCommandExists 'Get-RuntimeProcessEnvironment'
Assert-RuntimeCommandExists 'New-RuntimeLogPaths'

$artifactName = 'lifecycle-contract-{0}-{1}' -f ([DateTimeOffset]::UtcNow.ToString('yyyyMMdd-HHmmssfff')), ([Guid]::NewGuid().ToString('N'))
$artifactRoot = Join-Path $evidenceRoot $artifactName
New-Item -ItemType Directory -Path $artifactRoot | Out-Null

$environmentPath = Join-Path $artifactRoot 'probe.env'
$probeOutputPath = Join-Path $artifactRoot 'probe-output.json'
$statePath = Join-Path $artifactRoot 'processes.json'
$logDirectory = Join-Path $artifactRoot 'logs'
$generatedManifestPath = Join-Path $artifactRoot 'runtime.psd1'
$marker = 'application-runtime-test-marker'

$portProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$portProbe.Start()
$probePort = ([System.Net.IPEndPoint] $portProbe.LocalEndpoint).Port
$portProbe.Stop()

@(
    "SOURCE_TEST_PORT=$probePort"
    "SOURCE_TEST_OUTPUT=$probeOutputPath"
    "SOURCE_TEST_MARKER=$marker"
) | Set-Content -LiteralPath $environmentPath -Encoding utf8

$environmentFiles = @(
    @{
        Path = $environmentPath
        Variables = @{
            MUSIC_DREAMER_TEST_PORT = 'SOURCE_TEST_PORT'
            MUSIC_DREAMER_TEST_OUTPUT = 'SOURCE_TEST_OUTPUT'
            MUSIC_DREAMER_TEST_MARKER = 'SOURCE_TEST_MARKER'
        }
    }
)
$childEnvironment = Get-RuntimeProcessEnvironment -EnvironmentFiles $environmentFiles
Assert-Equal $childEnvironment.MUSIC_DREAMER_TEST_PORT ([string] $probePort) 'environmentMapsPort'
Assert-Equal $childEnvironment.MUSIC_DREAMER_TEST_OUTPUT $probeOutputPath 'environmentMapsOutputPath'
Assert-Equal $childEnvironment.MUSIC_DREAMER_TEST_MARKER $marker 'environmentMapsMarker'

$firstLogPaths = New-RuntimeLogPaths -ProcessName 'Probe' -LogDirectory $logDirectory
$secondLogPaths = New-RuntimeLogPaths -ProcessName 'Probe' -LogDirectory $logDirectory
Assert-True ($firstLogPaths.StandardOutputPath -ne $secondLogPaths.StandardOutputPath) 'logPathsAreUnique'
Assert-True ($firstLogPaths.StandardErrorPath -ne $secondLogPaths.StandardErrorPath) 'errorLogPathsAreUnique'
Assert-Equal (Split-Path -Parent $firstLogPaths.StandardOutputPath) $logDirectory 'logPathsUseDeclaredDirectory'

$readinessListener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$readinessListener.Start()
$readinessPort = ([System.Net.IPEndPoint] $readinessListener.LocalEndpoint).Port
try {
    $tcpReady = Test-RuntimeProcessReadiness @{
        Type = 'Tcp'
        Host = '127.0.0.1'
        Port = $readinessPort
        RequestTimeoutSeconds = 1
    }
    Assert-True $tcpReady 'tcpReadinessAcceptsListeningPort'
} finally {
    $readinessListener.Stop()
}
$tcpStopped = Test-RuntimeProcessReadiness @{
    Type = 'Tcp'
    Host = '127.0.0.1'
    Port = $readinessPort
    RequestTimeoutSeconds = 1
}
Assert-True (-not $tcpStopped) 'tcpReadinessRejectsClosedPort'

Assert-True (Test-Path -LiteralPath $applicationManifestPath -PathType Leaf) 'applicationManifestExists'
$applicationManifest = Import-RuntimeManifest $applicationManifestPath
$actualProcessNames = @($applicationManifest.Processes | ForEach-Object Name)
$expectedPorts = [ordered]@{
    Identity = 8081
    Catalog = 8082
    Media = 8083
    Interaction = 8084
    Gateway = 8080
    MusicWeb = 5173
    AdminWeb = 5174
}
Assert-Equal $actualProcessNames.Count $expectedPorts.Count 'applicationManifestHasSevenProcesses'
foreach ($expectedName in $expectedPorts.Keys) {
    $processSpec = @($applicationManifest.Processes | Where-Object Name -eq $expectedName)
    Assert-Equal $processSpec.Count 1 "applicationManifestHas:$expectedName"
    Assert-Equal ([int] $processSpec[0].Readiness.Port) $expectedPorts[$expectedName] "applicationManifestPort:$expectedName"
    Assert-True (-not [string]::IsNullOrWhiteSpace([string] $processSpec[0].WorkingDirectory)) "applicationManifestWorkingDirectory:$expectedName"
    Assert-True (-not [string]::IsNullOrWhiteSpace([string] $processSpec[0].LogDirectory)) "applicationManifestLogDirectory:$expectedName"
}
foreach ($backendName in @('Identity', 'Catalog', 'Media', 'Interaction', 'Gateway')) {
    $processSpec = @($applicationManifest.Processes | Where-Object Name -eq $backendName)[0]
    Assert-Equal $processSpec.Readiness.Type 'Tcp' "backendUsesTcpReadiness:$backendName"
}
foreach ($frontendName in @('MusicWeb', 'AdminWeb')) {
    $processSpec = @($applicationManifest.Processes | Where-Object Name -eq $frontendName)[0]
    Assert-Equal $processSpec.Readiness.Type 'Http' "frontendUsesHttpReadiness:$frontendName"
}
$passwordTargets = [ordered]@{
    Identity = 'MUSIC_DREAMER_IDENTITY_DB_PASSWORD'
    Catalog = 'MUSIC_DREAMER_CATALOG_DB_PASSWORD'
    Media = 'MUSIC_DREAMER_MEDIA_DB_PASSWORD'
    Interaction = 'MUSIC_DREAMER_INTERACTION_DB_PASSWORD'
}
foreach ($backendName in $passwordTargets.Keys) {
    $processSpec = @($applicationManifest.Processes | Where-Object Name -eq $backendName)[0]
    $files = @($processSpec.EnvironmentFiles)
    Assert-Equal $files.Count 1 "databaseServiceHasOneEnvironmentFile:$backendName"
    Assert-Equal $files[0].Path 'runtime-data/identity-db.env' "databaseServiceUsesApprovedEnvironmentFile:$backendName"
    Assert-Equal $files[0].Variables[$passwordTargets[$backendName]] 'MUSIC_DREAMER_IDENTITY_DB_PASSWORD' "databasePasswordMapsFromApprovedSource:$backendName"
}

$runtimeManifest = Import-RuntimeManifest (Join-Path $projectRoot 'infra\config\runtime-manifest.psd1')
$nodePath = @($runtimeManifest.Components | Where-Object Name -eq 'Node')[0].Path
$escapedNodePath = $nodePath.Replace("'", "''")
$escapedProbeScript = $probeScript.Replace("'", "''")
$escapedArtifactRoot = $artifactRoot.Replace("'", "''")
$escapedEnvironmentPath = $environmentPath.Replace("'", "''")
$escapedLogDirectory = $logDirectory.Replace("'", "''")
$escapedStatePath = $statePath.Replace("'", "''")
@"
@{
    Components = @(
        @{
            Name = 'Node'
            Path = '$escapedNodePath'
            VersionArguments = @('--version')
            VersionPattern = '^v(?<version>\d+\.\d+\.\d+)$'
            ExpectedVersion = '24.18.0'
        }
    )
    Processes = @(
        @{
            Name = 'Probe'
            FilePath = '$escapedNodePath'
            Arguments = @('$escapedProbeScript')
            WorkingDirectory = '$escapedArtifactRoot'
            EnvironmentFiles = @(
                @{
                    Path = '$escapedEnvironmentPath'
                    Variables = @{
                        MUSIC_DREAMER_TEST_PORT = 'SOURCE_TEST_PORT'
                        MUSIC_DREAMER_TEST_OUTPUT = 'SOURCE_TEST_OUTPUT'
                        MUSIC_DREAMER_TEST_MARKER = 'SOURCE_TEST_MARKER'
                    }
                }
            )
            LogDirectory = '$escapedLogDirectory'
            CommandMarker = 'application-runtime-probe'
            Readiness = @{
                Type = 'Tcp'
                Host = '127.0.0.1'
                Port = $probePort
                TimeoutSeconds = 10
                RequestTimeoutSeconds = 1
                IntervalMilliseconds = 100
            }
        }
    )
    StateFile = '$escapedStatePath'
    MutexName = 'Local\MusicDreamerApplicationRuntimeContract-$([Guid]::NewGuid().ToString('N'))'
}
"@ | Set-Content -LiteralPath $generatedManifestPath -Encoding utf8

$startedPid = $null
try {
    $startOutput = & $startScript -ManifestPath $generatedManifestPath -Json 2>&1
    Assert-Equal $LASTEXITCODE 0 'probeStartExitCode'
    $startResult = ($startOutput -join [Environment]::NewLine) | ConvertFrom-Json
    $startedPid = [int] $startResult.Pid
    Assert-Equal $startResult.Status 'Ready' 'probeStartReady'
    Assert-Equal $startResult.WorkingDirectory $artifactRoot 'probeStartRecordsWorkingDirectory'
    Assert-True (Test-Path -LiteralPath $startResult.StandardOutputPath -PathType Leaf) 'probeStartCreatesStdoutLog'
    Assert-True (Test-Path -LiteralPath $startResult.StandardErrorPath -PathType Leaf) 'probeStartCreatesStderrLog'

    $probeResult = Get-Content -Raw -LiteralPath $probeOutputPath | ConvertFrom-Json
    Assert-Equal $probeResult.cwd $artifactRoot 'probeReceivesWorkingDirectory'
    Assert-Equal $probeResult.marker $marker 'probeReceivesMappedEnvironment'

    $stateJson = Get-Content -Raw -LiteralPath $statePath
    Assert-True (-not $stateJson.Contains($marker, [System.StringComparison]::Ordinal)) 'stateDoesNotPersistEnvironmentValue'
    $state = $stateJson | ConvertFrom-Json
    Assert-Equal $state.Processes.Probe.StandardOutputPath $startResult.StandardOutputPath 'stateRecordsStdoutPath'
    Assert-Equal $state.Processes.Probe.StandardErrorPath $startResult.StandardErrorPath 'stateRecordsStderrPath'

    $statusOutput = & $statusScript -ManifestPath $generatedManifestPath -Json 2>&1
    Assert-Equal $LASTEXITCODE 0 'probeStatusExitCode'
    $statusResult = ($statusOutput -join [Environment]::NewLine) | ConvertFrom-Json
    Assert-Equal $statusResult.Status 'Ready' 'probeStatusReady'
    Assert-Equal $statusResult.StandardOutputPath $startResult.StandardOutputPath 'statusReturnsStdoutPath'
    Assert-Equal $statusResult.StandardErrorPath $startResult.StandardErrorPath 'statusReturnsStderrPath'

    $browserReady = Test-RuntimeProcessReadiness @{
        Type = 'Http'
        Uri = "http://127.0.0.1:$probePort/ready"
        ExpectedStatusCode = 200
        Headers = @{ Accept = 'text/html' }
        RequestTimeoutSeconds = 2
    }
    Assert-True $browserReady 'httpReadinessPassesDeclaredHeaders'
} finally {
    if ($null -ne $startedPid) {
        $stopOutput = & $stopScript -ManifestPath $generatedManifestPath -Json 2>&1
        $stopResult = ($stopOutput -join [Environment]::NewLine) | ConvertFrom-Json
        Assert-Equal $LASTEXITCODE 0 'probeStopExitCode'
        Assert-Equal $stopResult.Pid $startedPid 'probeStopsOnlyManagedPid'
        Assert-Equal $stopResult.Status 'Stopped' 'probeStopStatus'
    }
}

$frontendReadiness = [ordered]@{
    MusicWeb = [ordered]@{ Uri = 'http://127.0.0.1:5173/register'; Accept = 'text/html' }
    AdminWeb = [ordered]@{ Uri = 'http://127.0.0.1:5174/login'; Accept = 'text/html' }
}
foreach ($frontendName in $frontendReadiness.Keys) {
    $processSpec = @($applicationManifest.Processes | Where-Object Name -eq $frontendName)[0]
    Assert-Equal $processSpec.Readiness.Uri $frontendReadiness[$frontendName].Uri "frontendUsesBrowserRoute:$frontendName"
    Assert-Equal $processSpec.Readiness.Headers.Accept $frontendReadiness[$frontendName].Accept "frontendUsesBrowserAcceptHeader:$frontendName"
}

Write-Output "PASS applicationRuntimeContractArtifacts:$artifactRoot"
