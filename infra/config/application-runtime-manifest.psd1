@{
    Components = @(
        @{
            Name = 'Java'
            Path = 'E:\.minecraft\runtime\java-runtime-delta\bin\java.exe'
            VersionArguments = @('-version')
            VersionPattern = '(?m)^openjdk version "(?<version>\d+\.\d+\.\d+)'
            ExpectedVersion = '21.0.3'
        }
        @{
            Name = 'Node'
            Path = 'E:\nodejs-24.18.0\node.exe'
            VersionArguments = @('--version')
            VersionPattern = '^v(?<version>\d+\.\d+\.\d+)$'
            ExpectedVersion = '24.18.0'
        }
    )
    Processes = @(
        @{
            Name = 'Identity'
            FilePath = 'E:\.minecraft\runtime\java-runtime-delta\bin\java.exe'
            Arguments = @('-jar', 'backend/identity-service/target/identity-service-0.1.0.jar')
            WorkingDirectory = '.'
            EnvironmentFiles = @(
                @{
                    Path = 'runtime-data/identity-db.env'
                    Variables = @{
                        MUSIC_DREAMER_IDENTITY_DB_PASSWORD = 'MUSIC_DREAMER_IDENTITY_DB_PASSWORD'
                    }
                }
            )
            LogDirectory = 'runtime-data/logs/applications'
            CommandMarker = 'identity-service-0.1.0.jar'
            Readiness = @{ Type = 'Tcp'; Host = '127.0.0.1'; Port = 8081; TimeoutSeconds = 90; RequestTimeoutSeconds = 1; IntervalMilliseconds = 250 }
        }
        @{
            Name = 'Catalog'
            FilePath = 'E:\.minecraft\runtime\java-runtime-delta\bin\java.exe'
            Arguments = @('-jar', 'backend/catalog-service/target/catalog-service-0.1.0.jar')
            WorkingDirectory = '.'
            EnvironmentFiles = @(
                @{
                    Path = 'runtime-data/identity-db.env'
                    Variables = @{
                        MUSIC_DREAMER_CATALOG_DB_PASSWORD = 'MUSIC_DREAMER_IDENTITY_DB_PASSWORD'
                    }
                }
            )
            LogDirectory = 'runtime-data/logs/applications'
            CommandMarker = 'catalog-service-0.1.0.jar'
            Readiness = @{ Type = 'Tcp'; Host = '127.0.0.1'; Port = 8082; TimeoutSeconds = 90; RequestTimeoutSeconds = 1; IntervalMilliseconds = 250 }
        }
        @{
            Name = 'Media'
            FilePath = 'E:\.minecraft\runtime\java-runtime-delta\bin\java.exe'
            Arguments = @('-jar', 'backend/media-service/target/media-service-0.1.0.jar')
            WorkingDirectory = '.'
            EnvironmentFiles = @(
                @{
                    Path = 'runtime-data/identity-db.env'
                    Variables = @{
                        MUSIC_DREAMER_MEDIA_DB_PASSWORD = 'MUSIC_DREAMER_IDENTITY_DB_PASSWORD'
                    }
                }
            )
            LogDirectory = 'runtime-data/logs/applications'
            CommandMarker = 'media-service-0.1.0.jar'
            Readiness = @{ Type = 'Tcp'; Host = '127.0.0.1'; Port = 8083; TimeoutSeconds = 90; RequestTimeoutSeconds = 1; IntervalMilliseconds = 250 }
        }
        @{
            Name = 'Interaction'
            FilePath = 'E:\.minecraft\runtime\java-runtime-delta\bin\java.exe'
            Arguments = @('-jar', 'backend/interaction-service/target/interaction-service-0.1.0.jar')
            WorkingDirectory = '.'
            EnvironmentFiles = @(
                @{
                    Path = 'runtime-data/identity-db.env'
                    Variables = @{
                        MUSIC_DREAMER_INTERACTION_DB_PASSWORD = 'MUSIC_DREAMER_IDENTITY_DB_PASSWORD'
                    }
                }
            )
            LogDirectory = 'runtime-data/logs/applications'
            CommandMarker = 'interaction-service-0.1.0.jar'
            Readiness = @{ Type = 'Tcp'; Host = '127.0.0.1'; Port = 8084; TimeoutSeconds = 90; RequestTimeoutSeconds = 1; IntervalMilliseconds = 250 }
        }
        @{
            Name = 'Gateway'
            FilePath = 'E:\.minecraft\runtime\java-runtime-delta\bin\java.exe'
            Arguments = @('-jar', 'backend/gateway-service/target/gateway-service-0.1.0.jar')
            WorkingDirectory = '.'
            LogDirectory = 'runtime-data/logs/applications'
            CommandMarker = 'gateway-service-0.1.0.jar'
            Readiness = @{ Type = 'Tcp'; Host = '127.0.0.1'; Port = 8080; TimeoutSeconds = 90; RequestTimeoutSeconds = 1; IntervalMilliseconds = 250 }
        }
        @{
            Name = 'MusicWeb'
            FilePath = 'E:\nodejs-24.18.0\node.exe'
            Arguments = @('../node_modules/vite/bin/vite.js', '--host', '127.0.0.1', '--port', '5173', '--strictPort')
            WorkingDirectory = 'frontend/music-web'
            LogDirectory = 'runtime-data/logs/applications'
            CommandMarker = 'music-web-vite-5173'
            Readiness = @{ Type = 'Http'; Uri = 'http://127.0.0.1:5173/register'; Port = 5173; Headers = @{ Accept = 'text/html' }; ExpectedStatusCode = 200; TimeoutSeconds = 60; RequestTimeoutSeconds = 2; IntervalMilliseconds = 250 }
        }
        @{
            Name = 'AdminWeb'
            FilePath = 'E:\nodejs-24.18.0\node.exe'
            Arguments = @('../node_modules/@vue/cli-service/bin/vue-cli-service.js', 'serve', '--host', '127.0.0.1', '--port', '5174')
            WorkingDirectory = 'frontend/admin-web'
            LogDirectory = 'runtime-data/logs/applications'
            CommandMarker = 'admin-web-vue-cli-5174'
            Readiness = @{ Type = 'Http'; Uri = 'http://127.0.0.1:5174/login'; Port = 5174; Headers = @{ Accept = 'text/html' }; ExpectedStatusCode = 200; TimeoutSeconds = 90; RequestTimeoutSeconds = 2; IntervalMilliseconds = 250 }
        }
    )
    StateFile = 'runtime-data/application-processes.json'
    MutexName = 'Local\MusicDreamerApplicationRuntime'
    LockTimeoutSeconds = 15
}
