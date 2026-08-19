#requires -Version 7.0

param(
    [Parameter(Mandatory)]
    [int] $Port,

    [string] $ShutdownMarker,

    [string] $ReadyBody = 'UP',

    [string] $ContentType = 'text/plain',

    [string] $RequiredShutdownContentType,

    [string] $RequiredShutdownBody
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
$listener.Start()
try {
    $running = $true
    while ($running) {
        $client = $listener.AcceptTcpClient()
        try {
            $stream = $client.GetStream()
            $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::ASCII, $false, 1024, $true)
            $requestLine = $reader.ReadLine()
            $headers = @{}
            while ($true) {
                $headerLine = $reader.ReadLine()
                if ([string]::IsNullOrEmpty($headerLine)) {
                    break
                }
                $headerParts = $headerLine.Split(':', 2)
                if ($headerParts.Count -eq 2) {
                    $headers[$headerParts[0]] = $headerParts[1].Trim()
                }
            }
            $requestBody = ''
            if ($headers.ContainsKey('Content-Length')) {
                $contentLength = [int] $headers['Content-Length']
                if ($contentLength -gt 0) {
                    $bodyBuffer = [char[]]::new($contentLength)
                    $charactersRead = $reader.ReadBlock($bodyBuffer, 0, $contentLength)
                    $requestBody = [string]::new($bodyBuffer, 0, $charactersRead)
                }
            }

            $status = '404 Not Found'
            $body = 'NOT_FOUND'
            if ($requestLine -eq 'GET /ready HTTP/1.1') {
                $status = '200 OK'
                $body = $ReadyBody
            } elseif ($requestLine -eq 'POST /shutdown HTTP/1.1') {
                if (-not [string]::IsNullOrWhiteSpace($RequiredShutdownContentType) -and $headers['Content-Type'] -ne $RequiredShutdownContentType) {
                    $status = '415 Unsupported Media Type'
                    $body = 'UNSUPPORTED_MEDIA_TYPE'
                } elseif (-not [string]::IsNullOrWhiteSpace($RequiredShutdownBody) -and $requestBody -ne $RequiredShutdownBody) {
                    $status = '400 Bad Request'
                    $body = 'INVALID_BODY'
                } else {
                    $status = '200 OK'
                    $body = 'SHUTTING_DOWN'
                    if (-not [string]::IsNullOrWhiteSpace($ShutdownMarker)) {
                        Set-Content -LiteralPath ([System.IO.Path]::GetFullPath($ShutdownMarker)) -Value 'graceful' -Encoding ascii
                    }
                    $running = $false
                }
            }

            $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
            $header = "HTTP/1.1 $status`r`nContent-Type: $ContentType`r`nContent-Length: $($bodyBytes.Length)`r`nConnection: close`r`n`r`n"
            $headerBytes = [System.Text.Encoding]::ASCII.GetBytes($header)
            $stream.Write($headerBytes, 0, $headerBytes.Length)
            $stream.Write($bodyBytes, 0, $bodyBytes.Length)
            $stream.Flush()
        } finally {
            $client.Dispose()
        }
    }
} finally {
    $listener.Stop()
}
