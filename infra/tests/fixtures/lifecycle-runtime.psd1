@{
    Components = @()
    Processes = @(
        @{
            Name = 'Dummy'
            FilePath = 'pwsh'
            Arguments = @('-NoProfile', '-Command', 'Start-Sleep -Seconds 30')
            CommandMarker = 'Start-Sleep -Seconds 30'
        }
    )
    StateFile = 'runtime-data/test-runtime-state.json'
}
