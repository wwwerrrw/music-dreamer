#requires -Version 7.0

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:MusicDreamerProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

function Resolve-MusicDreamerPath {
    param(
        [Parameter(Mandatory)]
        [string] $Path
    )

    if ([System.IO.Path]::IsPathFullyQualified($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }

    return [System.IO.Path]::GetFullPath((Join-Path $script:MusicDreamerProjectRoot $Path))
}

function Import-RuntimeManifest {
    param(
        [Parameter(Mandatory)]
        [string] $Path
    )

    $resolvedPath = Resolve-MusicDreamerPath $Path
    return Import-PowerShellDataFile -LiteralPath $resolvedPath
}

function Get-RuntimeProcessEnvironment {
    param(
        [Parameter(Mandatory)]
        [array] $EnvironmentFiles
    )

    $environment = @{}
    foreach ($environmentFile in $EnvironmentFiles) {
        if (-not $environmentFile.ContainsKey('Path') -or -not $environmentFile.ContainsKey('Variables')) {
            throw 'Each environment file declaration must contain Path and Variables.'
        }

        $resolvedPath = Resolve-MusicDreamerPath $environmentFile.Path
        if (-not (Test-Path -LiteralPath $resolvedPath -PathType Leaf)) {
            throw "Runtime environment file '$resolvedPath' does not exist."
        }

        $sourceValues = @{}
        foreach ($line in Get-Content -LiteralPath $resolvedPath) {
            $trimmedLine = $line.Trim()
            if ([string]::IsNullOrWhiteSpace($trimmedLine) -or $trimmedLine.StartsWith('#', [System.StringComparison]::Ordinal)) {
                continue
            }

            $separatorIndex = $line.IndexOf('=')
            if ($separatorIndex -lt 1) {
                throw "Runtime environment file '$resolvedPath' contains a malformed entry."
            }

            $sourceName = $line.Substring(0, $separatorIndex).Trim()
            if ($sourceName -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
                throw "Runtime environment file '$resolvedPath' contains an invalid variable name."
            }
            if ($sourceValues.ContainsKey($sourceName)) {
                throw "Runtime environment file '$resolvedPath' contains a duplicate variable name."
            }
            $sourceValues[$sourceName] = $line.Substring($separatorIndex + 1)
        }

        foreach ($targetName in $environmentFile.Variables.Keys) {
            $sourceName = [string] $environmentFile.Variables[$targetName]
            if ($targetName -notmatch '^[A-Za-z_][A-Za-z0-9_]*$' -or $sourceName -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
                throw 'Runtime environment mappings must use valid variable names.'
            }
            if (-not $sourceValues.ContainsKey($sourceName)) {
                throw "Runtime environment file '$resolvedPath' does not define a required source variable."
            }
            if ($environment.ContainsKey($targetName)) {
                throw "Runtime environment mapping contains a duplicate target variable '$targetName'."
            }
            $environment[$targetName] = [string] $sourceValues[$sourceName]
        }
    }

    return $environment
}

function New-RuntimeLogPaths {
    param(
        [Parameter(Mandatory)]
        [string] $ProcessName,

        [Parameter(Mandatory)]
        [string] $LogDirectory
    )

    $resolvedDirectory = Resolve-MusicDreamerPath $LogDirectory
    if (Test-Path -LiteralPath $resolvedDirectory) {
        if (-not (Test-Path -LiteralPath $resolvedDirectory -PathType Container)) {
            throw "Runtime log path '$resolvedDirectory' exists but is not a directory."
        }
    } else {
        New-Item -ItemType Directory -Path $resolvedDirectory -Force | Out-Null
    }

    $safeName = $ProcessName -replace '[^A-Za-z0-9._-]', '-'
    $uniqueStem = '{0}-{1}-{2}' -f $safeName, ([DateTimeOffset]::UtcNow.ToString('yyyyMMdd-HHmmssfff')), ([Guid]::NewGuid().ToString('N'))
    return [pscustomobject]@{
        StandardOutputPath = Join-Path $resolvedDirectory "$uniqueStem.stdout.log"
        StandardErrorPath = Join-Path $resolvedDirectory "$uniqueStem.stderr.log"
    }
}

function Enter-RuntimeLifecycleLock {
    param(
        [Parameter(Mandatory)]
        [hashtable] $Manifest
    )

    if ($Manifest.ContainsKey('MutexName')) {
        $mutexName = $Manifest.MutexName
    } else {
        $rootBytes = [System.Text.Encoding]::UTF8.GetBytes($script:MusicDreamerProjectRoot.ToLowerInvariant())
        $rootHash = [Convert]::ToHexString([System.Security.Cryptography.SHA256]::HashData($rootBytes)).Substring(0, 16)
        $mutexName = "Local\MusicDreamerRuntime-$rootHash"
    }
    $timeoutSeconds = if ($Manifest.ContainsKey('LockTimeoutSeconds')) { [int] $Manifest.LockTimeoutSeconds } else { 15 }
    $mutex = [System.Threading.Mutex]::new($false, $mutexName)

    try {
        try {
            $acquired = $mutex.WaitOne([TimeSpan]::FromSeconds($timeoutSeconds))
        } catch [System.Threading.AbandonedMutexException] {
            $acquired = $true
        }

        if (-not $acquired) {
            throw "Timed out after $timeoutSeconds seconds waiting for runtime lifecycle lock '$mutexName'."
        }

        return $mutex
    } catch {
        $mutex.Dispose()
        throw
    }
}

function Exit-RuntimeLifecycleLock {
    param(
        [Parameter(Mandatory)]
        [System.Threading.Mutex] $Mutex
    )

    try {
        $Mutex.ReleaseMutex()
    } finally {
        $Mutex.Dispose()
    }
}

function Resolve-MusicDreamerCommandPath {
    param(
        [Parameter(Mandatory)]
        [string] $FilePath
    )

    if ([System.IO.Path]::IsPathFullyQualified($FilePath) -or $FilePath.Contains('/') -or $FilePath.Contains('\')) {
        return Resolve-MusicDreamerPath $FilePath
    }

    $command = Get-Command $FilePath -CommandType Application, ExternalScript -ErrorAction Stop | Select-Object -First 1
    if (-not [string]::IsNullOrWhiteSpace($command.Path)) {
        return $command.Path
    }

    return $command.Definition
}

function Get-RuntimeComponentStatus {
    param(
        [Parameter(Mandatory)]
        [hashtable] $Component
    )

    $resolvedPath = Resolve-MusicDreamerPath $Component.Path
    if (-not (Test-Path -LiteralPath $resolvedPath -PathType Leaf)) {
        return [pscustomobject]@{
            Name = $Component.Name
            Status = 'Missing'
            ExpectedVersion = $Component.ExpectedVersion
            DetectedVersion = $null
            Path = $resolvedPath
            Message = "Expected version $($Component.ExpectedVersion) at '$resolvedPath', but the runtime is missing."
        }
    }

    $probe = if ($Component.ContainsKey('Probe')) { $Component.Probe } else { 'Command' }
    if ($probe -eq 'FileContent') {
        $versionOutput = Get-Content -Raw -LiteralPath $resolvedPath
    } elseif ($probe -eq 'Command') {
        if ($Component.ContainsKey('VersionArguments')) {
            [array] $versionArguments = @($Component.VersionArguments)
        } else {
            [array] $versionArguments = @()
        }
        $versionOutput = & $resolvedPath @versionArguments 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Runtime component '$($Component.Name)' version command failed with exit code $LASTEXITCODE."
        }
    } else {
        throw "Runtime component '$($Component.Name)' uses unsupported probe '$probe'."
    }

    $versionText = ($versionOutput -join [Environment]::NewLine).Trim()
    $match = [regex]::Match($versionText, $Component.VersionPattern)
    if (-not $match.Success) {
        throw "Runtime component '$($Component.Name)' returned an unrecognized version: '$versionText'."
    }

    $detectedVersion = $match.Groups['version'].Value
    if ($detectedVersion -ne $Component.ExpectedVersion) {
        return [pscustomobject]@{
            Name = $Component.Name
            Status = 'VersionMismatch'
            ExpectedVersion = $Component.ExpectedVersion
            DetectedVersion = $detectedVersion
            Path = $resolvedPath
            Message = "Detected version $detectedVersion at '$resolvedPath'; expected $($Component.ExpectedVersion)."
        }
    }

    return [pscustomobject]@{
        Name = $Component.Name
        Status = 'Ready'
        ExpectedVersion = $Component.ExpectedVersion
        DetectedVersion = $detectedVersion
        Path = $resolvedPath
        Message = 'Runtime is ready.'
    }
}

function Write-RuntimeState {
    param(
        [Parameter(Mandatory)]
        [string] $Path,

        [Parameter(Mandatory)]
        [hashtable] $Processes
    )

    $resolvedPath = Resolve-MusicDreamerPath $Path
    $parentPath = Split-Path -Parent $resolvedPath
    if (-not (Test-Path -LiteralPath $parentPath -PathType Container)) {
        New-Item -ItemType Directory -Path $parentPath -Force | Out-Null
    }

    [ordered]@{
        Version = 1
        UpdatedAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
        Processes = $Processes
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $resolvedPath -Encoding utf8

    return $resolvedPath
}

function Read-RuntimeState {
    param(
        [Parameter(Mandatory)]
        [string] $Path
    )

    $resolvedPath = Resolve-MusicDreamerPath $Path
    if (-not (Test-Path -LiteralPath $resolvedPath -PathType Leaf)) {
        return [pscustomobject]@{
            Version = 1
            Processes = [pscustomobject]@{}
        }
    }

    return Get-Content -Raw -LiteralPath $resolvedPath | ConvertFrom-Json
}

function ConvertFrom-RuntimeHttpContent {
    param(
        [AllowNull()]
        $Content
    )

    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }
    return [string] $Content
}

function Test-RuntimeProcessReadiness {
    param(
        [Parameter(Mandatory)]
        [hashtable] $Readiness
    )

    try {
        if ($Readiness.Type -eq 'Http') {
            $requestTimeoutSeconds = if ($Readiness.ContainsKey('RequestTimeoutSeconds')) { [int] $Readiness.RequestTimeoutSeconds } else { 2 }
            $requestArguments = @{
                Uri = $Readiness.Uri
                Method = 'Get'
                TimeoutSec = $requestTimeoutSeconds
            }
            if ($Readiness.ContainsKey('Headers')) {
                $requestArguments.Headers = $Readiness.Headers
            }
            $response = Invoke-WebRequest @requestArguments
            $expectedStatusCode = if ($Readiness.ContainsKey('ExpectedStatusCode')) { [int] $Readiness.ExpectedStatusCode } else { 200 }
            if ([int] $response.StatusCode -ne $expectedStatusCode) {
                return $false
            }
            $responseContent = (ConvertFrom-RuntimeHttpContent $response.Content).Trim()
            if ($Readiness.ContainsKey('ExpectedContent') -and $responseContent -ne $Readiness.ExpectedContent) {
                return $false
            }
            return $true
        }
        if ($Readiness.Type -eq 'Redis') {
            $clientPath = Resolve-MusicDreamerCommandPath $Readiness.ClientPath
            $redisOutput = & $clientPath -h $Readiness.Host -p $Readiness.Port PING 2>$null
            return $LASTEXITCODE -eq 0 -and ($redisOutput -join [Environment]::NewLine).Trim() -eq 'PONG'
        }
        if ($Readiness.Type -eq 'Tcp') {
            $requestTimeoutSeconds = if ($Readiness.ContainsKey('RequestTimeoutSeconds')) { [double] $Readiness.RequestTimeoutSeconds } else { 2 }
            $timeoutMilliseconds = [Math]::Max(1, [int] ($requestTimeoutSeconds * 1000))
            $client = [System.Net.Sockets.TcpClient]::new()
            try {
                $connectTask = $client.ConnectAsync([string] $Readiness.Host, [int] $Readiness.Port)
                if (-not $connectTask.Wait($timeoutMilliseconds)) {
                    return $false
                }
                return $client.Connected
            } finally {
                $client.Dispose()
            }
        }

        throw "Unsupported readiness type '$($Readiness.Type)'."
    } catch {
        return $false
    }
}

function Wait-RuntimeProcessReadiness {
    param(
        [Parameter(Mandatory)]
        [hashtable] $ProcessSpec,

        [Parameter(Mandatory)]
        [int] $ProcessId
    )

    $readiness = $ProcessSpec.Readiness
    $timeoutSeconds = if ($readiness.ContainsKey('TimeoutSeconds')) { [int] $readiness.TimeoutSeconds } else { 30 }
    $intervalMilliseconds = if ($readiness.ContainsKey('IntervalMilliseconds')) { [int] $readiness.IntervalMilliseconds } else { 250 }
    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($timeoutSeconds)

    while ([DateTimeOffset]::UtcNow -lt $deadline) {
        if ($null -eq (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            throw "Runtime process '$($ProcessSpec.Name)' exited before becoming ready."
        }
        if (Test-RuntimeProcessReadiness $readiness) {
            return
        }
        Start-Sleep -Milliseconds $intervalMilliseconds
    }

    throw "Runtime process '$($ProcessSpec.Name)' did not become ready within $timeoutSeconds seconds."
}

function Invoke-RuntimeGracefulStop {
    param(
        [Parameter(Mandatory)]
        [hashtable] $Stop
    )

    if ($Stop.Type -notin @('Http', 'Redis')) {
        throw "Unsupported graceful stop type '$($Stop.Type)'."
    }

    try {
        if ($Stop.Type -eq 'Http') {
            $method = if ($Stop.ContainsKey('Method')) { $Stop.Method } else { 'POST' }
            $requestTimeoutSeconds = if ($Stop.ContainsKey('RequestTimeoutSeconds')) { [int] $Stop.RequestTimeoutSeconds } else { 2 }
            $requestArguments = @{
                Uri = $Stop.Uri
                Method = $method
                TimeoutSec = $requestTimeoutSeconds
            }
            if ($Stop.ContainsKey('ContentType')) {
                $requestArguments.ContentType = $Stop.ContentType
            }
            if ($Stop.ContainsKey('Body')) {
                $requestArguments.Body = $Stop.Body
            }
            $response = Invoke-WebRequest @requestArguments
            $expectedStatusCode = if ($Stop.ContainsKey('ExpectedStatusCode')) { [int] $Stop.ExpectedStatusCode } else { 200 }
            if ([int] $response.StatusCode -ne $expectedStatusCode) {
                return $false
            }
            $responseContent = (ConvertFrom-RuntimeHttpContent $response.Content).Trim()
            if ($Stop.ContainsKey('ExpectedContent') -and $responseContent -ne $Stop.ExpectedContent) {
                return $false
            }
            return $true
        }
        if ($Stop.Type -eq 'Redis') {
            $clientPath = Resolve-MusicDreamerCommandPath $Stop.ClientPath
            & $clientPath -h $Stop.Host -p $Stop.Port SHUTDOWN SAVE 2>$null | Out-Null
            return $LASTEXITCODE -eq 0
        }
    } catch {
        return $false
    }
}

function Wait-RuntimeProcessExit {
    param(
        [Parameter(Mandatory)]
        [int] $ProcessId,

        [Parameter(Mandatory)]
        [int] $TimeoutSeconds
    )

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    while ($null -ne (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) -and [DateTimeOffset]::UtcNow -lt $deadline) {
        Start-Sleep -Milliseconds 100
    }

    return $null -eq (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)
}

function Get-ManagedProcessStatus {
    param(
        [Parameter(Mandatory)]
        [hashtable] $ProcessSpec,

        [Parameter(Mandatory)]
        [psobject] $State
    )

    $stateProperty = $State.Processes.PSObject.Properties[$ProcessSpec.Name]
    if ($null -eq $stateProperty) {
        return [pscustomobject]@{
            Name = $ProcessSpec.Name
            Status = 'Stopped'
            Pid = $null
            Owned = $false
            ExecutablePath = $null
            Message = 'No managed process state exists.'
        }
    }

    $processState = $stateProperty.Value
    $workingDirectoryProperty = $processState.PSObject.Properties['WorkingDirectory']
    $standardOutputProperty = $processState.PSObject.Properties['StandardOutputPath']
    $standardErrorProperty = $processState.PSObject.Properties['StandardErrorPath']
    $workingDirectory = if ($null -ne $workingDirectoryProperty) { $workingDirectoryProperty.Value } else { $null }
    $standardOutputPath = if ($null -ne $standardOutputProperty) { $standardOutputProperty.Value } else { $null }
    $standardErrorPath = if ($null -ne $standardErrorProperty) { $standardErrorProperty.Value } else { $null }
    $process = Get-Process -Id $processState.Pid -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return [pscustomobject]@{
            Name = $ProcessSpec.Name
            Status = 'Stopped'
            Pid = $processState.Pid
            Owned = $false
            ExecutablePath = $processState.ExecutablePath
            WorkingDirectory = $workingDirectory
            StandardOutputPath = $standardOutputPath
            StandardErrorPath = $standardErrorPath
            Message = 'The recorded process is not running.'
        }
    }

    try {
        $actualPath = $process.Path
    } catch {
        $actualPath = $null
    }
    $recordedPath = [string] $processState.ExecutablePath
    if ([string]::IsNullOrWhiteSpace($actualPath) -or [string]::IsNullOrWhiteSpace($recordedPath)) {
        return [pscustomobject]@{
            Name = $ProcessSpec.Name
            Status = 'Unowned'
            Pid = $processState.Pid
            Owned = $false
            ExecutablePath = $actualPath
            WorkingDirectory = $workingDirectory
            StandardOutputPath = $standardOutputPath
            StandardErrorPath = $standardErrorPath
            Message = 'PID exists, but its executable path cannot be verified against managed process state.'
        }
    }
    $pathMatches = [string]::Equals(
        [System.IO.Path]::GetFullPath($actualPath),
        [System.IO.Path]::GetFullPath($recordedPath),
        [System.StringComparison]::OrdinalIgnoreCase)
    $expectedStart = [DateTimeOffset] $processState.StartedAtUtc
    $actualStart = [DateTimeOffset] $process.StartTime.ToUniversalTime()
    $startMatches = [Math]::Abs(($actualStart - $expectedStart).TotalSeconds) -lt 1
    $commandMarkerMatches = [string]::Equals(
        $processState.CommandMarker,
        $ProcessSpec.CommandMarker,
        [System.StringComparison]::Ordinal)
    $owned = $pathMatches -and $startMatches -and $commandMarkerMatches

    return [pscustomobject]@{
        Name = $ProcessSpec.Name
        Status = if ($owned) { 'Running' } else { 'Unowned' }
        Pid = $processState.Pid
        Owned = $owned
        ExecutablePath = $actualPath
        WorkingDirectory = $workingDirectory
        StandardOutputPath = $standardOutputPath
        StandardErrorPath = $standardErrorPath
        Message = if ($owned) { 'Managed process is running.' } else { 'PID exists but does not match managed process state.' }
    }
}
