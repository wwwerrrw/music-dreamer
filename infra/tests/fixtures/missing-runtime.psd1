@{
    Components = @(
        @{
            Name = 'Node'
            Path = 'infra/runtime/node-v20.19.5-win-x64/node.exe'
            VersionArguments = @('--version')
            VersionPattern = '^v(?<version>\d+\.\d+\.\d+)$'
            ExpectedVersion = '20.19.5'
        }
    )
    Processes = @()
    StateFile = 'runtime-data/test-runtime-state.json'
}
