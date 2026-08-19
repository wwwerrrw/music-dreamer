@{
    Components = @(
        @{
            Name = 'Node'
            Path = 'infra/tests/fixtures/node20.cmd'
            VersionArguments = @()
            VersionPattern = '^v(?<version>\d+\.\d+\.\d+)$'
            ExpectedVersion = '20.19.5'
        }
    )
    Processes = @()
    StateFile = 'runtime-data/test-runtime-state.json'
}
