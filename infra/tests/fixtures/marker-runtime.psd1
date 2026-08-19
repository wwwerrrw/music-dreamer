@{
    Components = @(
        @{
            Name = 'Nacos'
            Path = 'infra/tests/fixtures/nacos.version'
            Probe = 'FileContent'
            VersionPattern = '^(?<version>\d+\.\d+\.\d+)$'
            ExpectedVersion = '2.3.2'
        }
    )
    Processes = @()
    StateFile = 'runtime-data/test-runtime-state.json'
}
