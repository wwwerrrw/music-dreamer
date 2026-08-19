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
            Name = 'Maven'
            Path = 'C:\Users\LENOVO\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin\mvn.cmd'
            VersionArguments = @('--version')
            VersionPattern = '(?m)^Apache Maven (?<version>\d+\.\d+\.\d+)'
            ExpectedVersion = '3.9.9'
        }
        @{
            Name = 'MySQL'
            Path = 'E:\MySQL\MySQL Server 8.0\bin\mysql.exe'
            VersionArguments = @('--version')
            VersionPattern = '\bVer (?<version>\d+\.\d+\.\d+)\b'
            ExpectedVersion = '8.0.39'
        }
        @{
            Name = 'Node'
            Path = 'E:\nodejs-24.18.0\node.exe'
            VersionArguments = @('--version')
            VersionPattern = '^v(?<version>\d+\.\d+\.\d+)$'
            ExpectedVersion = '24.18.0'
        }
        @{
            Name = 'Redis'
            Path = 'infra/runtime/Redis-8.10.0-Windows-x64-msys2/redis-server.exe'
            VersionArguments = @('--version')
            VersionPattern = '\bv=(?<version>\d+\.\d+\.\d+)\b'
            ExpectedVersion = '8.10.0'
        }
        @{
            Name = 'Nacos'
            Path = 'infra/runtime/nacos-server-2.3.2/VERSION'
            Probe = 'FileContent'
            VersionPattern = '^(?<version>\d+\.\d+\.\d+)$'
            ExpectedVersion = '2.3.2'
        }
    )
    Processes = @(
        @{
            Name = 'Redis'
            FilePath = 'infra/runtime/Redis-8.10.0-Windows-x64-msys2/redis-server.exe'
            Arguments = @('infra/config/redis.conf')
            CommandMarker = 'redis-windows-8.10.0'
            RequiredDirectories = @('runtime-data/redis', 'runtime-data/logs')
            Readiness = @{
                Type = 'Redis'
                ClientPath = 'infra/runtime/Redis-8.10.0-Windows-x64-msys2/redis-cli.exe'
                Host = '127.0.0.1'
                Port = 6380
                TimeoutSeconds = 15
                IntervalMilliseconds = 100
            }
            Stop = @{
                Type = 'Redis'
                ClientPath = 'infra/runtime/Redis-8.10.0-Windows-x64-msys2/redis-cli.exe'
                Host = '127.0.0.1'
                Port = 6380
                TimeoutSeconds = 10
            }
        }
        @{
            Name = 'Nacos'
            FilePath = 'E:\.minecraft\runtime\java-runtime-delta\bin\java.exe'
            Arguments = @(
                '-Xms512m'
                '-Xmx512m'
                '-Xmn256m'
                '-Dnacos.standalone=true'
                '-Dloader.path=infra/runtime/nacos-server-2.3.2/nacos/plugins,infra/runtime/nacos-server-2.3.2/nacos/plugins/health,infra/runtime/nacos-server-2.3.2/nacos/plugins/cmdb,infra/runtime/nacos-server-2.3.2/nacos/plugins/selector'
                '-Dnacos.home=infra/runtime/nacos-server-2.3.2/nacos'
                '-jar'
                'infra/runtime/nacos-server-2.3.2/nacos/target/nacos-server.jar'
                '--spring.config.additional-location=file:infra/runtime/nacos-server-2.3.2/nacos/conf/'
                '--logging.config=infra/runtime/nacos-server-2.3.2/nacos/conf/nacos-logback.xml'
                '--server.address=127.0.0.1'
                '--management.endpoints.web.exposure.include=health,shutdown'
                '--management.endpoint.shutdown.enabled=true'
                'nacos.nacos'
                '-m'
                'standalone'
            )
            CommandMarker = 'nacos-server-2.3.2'
            RequiredDirectories = @(
                'infra/runtime/nacos-server-2.3.2/nacos/data'
                'infra/runtime/nacos-server-2.3.2/nacos/logs'
            )
            Readiness = @{
                Type = 'Http'
                Uri = 'http://127.0.0.1:8848/nacos/actuator/health'
                ExpectedContent = '{"status":"UP"}'
                TimeoutSeconds = 60
                IntervalMilliseconds = 250
            }
            Stop = @{
                Type = 'Http'
                Uri = 'http://127.0.0.1:8848/nacos/actuator/shutdown'
                Method = 'POST'
                ContentType = 'application/json'
                Body = '{}'
                TimeoutSeconds = 30
                RequestTimeoutSeconds = 5
            }
        }
    )
    StateFile = 'runtime-data/processes.json'
}
