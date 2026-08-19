@{
    Components = @(
        @{
            Name = 'ArgumentFixture'
            Path = 'infra/tests/fixtures/version-with-argument.cmd'
            VersionArguments = @('--version')
            VersionPattern = '^fixture (?<version>\d+\.\d+\.\d+)$'
            ExpectedVersion = '1.2.3'
        }
    )
    Processes = @()
    StateFile = 'runtime-data/test-runtime-state.json'
}
