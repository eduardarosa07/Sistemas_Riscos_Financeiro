[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDirectory = Join-Path $projectRoot 'backend'
$frontendDirectory = Join-Path $projectRoot 'frontend'

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory)][string]$Description,
        [Parameter(Mandatory)][string]$Directory,
        [Parameter(Mandatory)][scriptblock]$Command
    )

    Write-Host ''
    Write-Host $Description -ForegroundColor Cyan
    Push-Location $Directory
    try {
        & $Command
        if ($LASTEXITCODE -ne 0) {
            throw "$Description falhou com codigo $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path (Join-Path $backendDirectory 'mvnw.cmd'))) {
    throw 'Maven Wrapper do backend nao encontrado.'
}
if (-not (Test-Path (Join-Path $frontendDirectory 'package.json'))) {
    throw 'package.json do frontend nao encontrado.'
}
if (-not (Test-Path (Join-Path $frontendDirectory 'node_modules'))) {
    throw 'Dependencias do Angular ausentes. Execute npm ci na pasta frontend uma vez.'
}

$npmCommand = (Get-Command npm.cmd -ErrorAction Stop).Source
$mavenWrapper = Join-Path $backendDirectory 'mvnw.cmd'
$previousCi = $env:CI

try {
    $env:CI = 'true'

    Invoke-CheckedCommand `
        -Description 'Verificando tipos e templates Angular...' `
        -Directory $frontendDirectory `
        -Command { & $npmCommand run typecheck }

    Invoke-CheckedCommand `
        -Description 'Executando testes unitarios do frontend...' `
        -Directory $frontendDirectory `
        -Command { & $npmCommand run test:unit }

    Invoke-CheckedCommand `
        -Description 'Gerando build de producao do Angular...' `
        -Directory $frontendDirectory `
        -Command { & $npmCommand run build }

    Invoke-CheckedCommand `
        -Description 'Executando testes Maven do backend...' `
        -Directory $backendDirectory `
        -Command { & $mavenWrapper test }
} finally {
    $env:CI = $previousCi
}

Write-Host ''
Write-Host 'Validacao do codigo concluida com sucesso.' -ForegroundColor Green
Write-Host 'Os testes opt-in de PostgreSQL permanecem desabilitados na suite comum.'
