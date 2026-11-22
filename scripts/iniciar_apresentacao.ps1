[CmdletBinding()]
param(
    [string]$DatabaseUrl = 'jdbc:postgresql://localhost:5432/sistema_riscos',
    [string]$DatabaseUser = 'postgres'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDirectory = Join-Path $projectRoot 'backend'
$frontendDirectory = Join-Path $projectRoot 'frontend'
$runtimeDirectory = Join-Path $env:TEMP 'sgr-apresentacao'

function Test-TcpPort {
    param([int]$Port)
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $result = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne(500)) { return $false }
        $client.EndConnect($result)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-HttpEndpoint {
    param(
        [string]$Url,
        [int]$TimeoutSeconds
    )
    $limit = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $limit) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                return $true
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Stop-StartedProcessTree {
    param([System.Diagnostics.Process]$RootProcess)

    if (-not $RootProcess) { return }

    $rootId = $RootProcess.Id
    $snapshot = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    $knownIds = [Collections.Generic.HashSet[int]]::new()
    [void]$knownIds.Add($rootId)
    $depthById = @{ $rootId = 0 }
    $tree = @()

    do {
        $added = $false
        foreach ($candidate in $snapshot) {
            $candidateId = [int]$candidate.ProcessId
            $parentId = [int]$candidate.ParentProcessId
            if ($knownIds.Contains($parentId) -and -not $knownIds.Contains($candidateId)) {
                [void]$knownIds.Add($candidateId)
                $depth = [int]$depthById[$parentId] + 1
                $depthById[$candidateId] = $depth
                $tree += [PSCustomObject]@{ Id = $candidateId; Depth = $depth }
                $added = $true
            }
        }
    } while ($added)

    $tree += [PSCustomObject]@{ Id = $rootId; Depth = 0 }
    foreach ($entry in ($tree | Sort-Object Depth -Descending)) {
        Stop-Process -Id $entry.Id -Force -ErrorAction SilentlyContinue
    }
}

if (-not (Test-Path (Join-Path $backendDirectory 'mvnw.cmd'))) {
    throw 'Maven Wrapper do backend nao encontrado.'
}
if (-not (Test-Path (Join-Path $frontendDirectory 'node_modules'))) {
    throw 'Dependencias do Angular ausentes. Execute npm ci na pasta frontend.'
}
if (Test-TcpPort 8080) {
    throw 'A porta 8080 ja esta em uso. Encerre o processo existente antes de iniciar o modo de apresentacao.'
}
if (Test-TcpPort 4200) {
    throw 'A porta 4200 ja esta em uso. Encerre o processo existente antes de iniciar o modo de apresentacao.'
}

New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null

$passwordFromEnvironment = -not [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)
$plainPassword = $null
$passwordPointer = [IntPtr]::Zero

if ($passwordFromEnvironment) {
    $plainPassword = $env:DB_PASSWORD
} else {
    $securePassword = Read-Host 'Senha do PostgreSQL' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
}

$managedVariables = @(
    'DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JPA_DDL_AUTO',
    'SGR_SINCRONIZACAO_AUTOMATICA', 'APP_ADMIN_EMAIL',
    'APP_ADMIN_PASSWORD', 'FRONTEND_ORIGIN', 'DEBUG'
)
$previousEnvironment = @{}
foreach ($name in $managedVariables) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$backendProcess = $null
$frontendProcess = $null

try {
    $env:DB_URL = $DatabaseUrl
    $env:DB_USERNAME = $DatabaseUser
    $env:DB_PASSWORD = $plainPassword
    $env:JPA_DDL_AUTO = 'validate'
    $env:SGR_SINCRONIZACAO_AUTOMATICA = 'false'
    $env:APP_ADMIN_EMAIL = ''
    $env:APP_ADMIN_PASSWORD = ''
    $env:FRONTEND_ORIGIN = 'http://localhost:4200'
    $env:DEBUG = 'false'

    Write-Host 'Iniciando backend seguro para apresentacao...'
    $backendProcess = Start-Process `
        -FilePath (Join-Path $backendDirectory 'mvnw.cmd') `
        -ArgumentList 'spring-boot:run' `
        -WorkingDirectory $backendDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimeDirectory 'backend.log') `
        -RedirectStandardError (Join-Path $runtimeDirectory 'backend-error.log') `
        -PassThru

    if (-not (Wait-HttpEndpoint -Url 'http://127.0.0.1:8080/api/status' -TimeoutSeconds 180)) {
        throw "Backend nao respondeu. Consulte $runtimeDirectory\backend-error.log."
    }

    Write-Host 'Iniciando Angular...'
    $npmCommand = (Get-Command npm.cmd -ErrorAction Stop).Source
    $frontendProcess = Start-Process `
        -FilePath $npmCommand `
        -ArgumentList @('start', '--', '--host', '127.0.0.1', '--port', '4200') `
        -WorkingDirectory $frontendDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimeDirectory 'frontend.log') `
        -RedirectStandardError (Join-Path $runtimeDirectory 'frontend-error.log') `
        -PassThru

    if (-not (Wait-HttpEndpoint -Url 'http://127.0.0.1:4200/login' -TimeoutSeconds 180)) {
        throw "Angular nao respondeu. Consulte $runtimeDirectory\frontend-error.log."
    }

    [PSCustomObject]@{
        projectRoot = (Resolve-Path $projectRoot).Path
        iniciadoEm = (Get-Date).ToString('o')
        backend = [PSCustomObject]@{
            pid = $backendProcess.Id
            iniciadoEm = $backendProcess.StartTime.ToUniversalTime().ToString('o')
        }
        frontend = [PSCustomObject]@{
            pid = $frontendProcess.Id
            iniciadoEm = $frontendProcess.StartTime.ToUniversalTime().ToString('o')
        }
    } | ConvertTo-Json -Depth 3 | Set-Content -Encoding UTF8 (Join-Path $runtimeDirectory 'processos.json')

    Write-Host ''
    Write-Host 'SGR pronto para apresentacao.' -ForegroundColor Green
    Write-Host 'Interface: http://localhost:4200/login'
    Write-Host 'API:       http://localhost:8080/api/status'
    Write-Host "Logs:      $runtimeDirectory"
    Write-Host 'Sincronizacao automatica: DESABILITADA'
    Write-Host 'Para encerrar: .\scripts\encerrar_apresentacao.ps1'
} catch {
    Stop-StartedProcessTree -RootProcess $frontendProcess
    Stop-StartedProcessTree -RootProcess $backendProcess
    throw
} finally {
    foreach ($name in $managedVariables) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    $plainPassword = $null
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}
