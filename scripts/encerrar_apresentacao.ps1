[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
$runtimeDirectory = Join-Path $env:TEMP 'sgr-apresentacao'
$processFile = Join-Path $runtimeDirectory 'processos.json'

if (-not (Test-Path -LiteralPath $processFile -PathType Leaf)) {
    throw "Registro de processos nao encontrado em $processFile. Nenhum processo foi encerrado."
}

$record = Get-Content -LiteralPath $processFile -Raw | ConvertFrom-Json
if ([string]::IsNullOrWhiteSpace($record.projectRoot)) {
    throw 'Registro de processos invalido: raiz do projeto ausente.'
}

$recordedRoot = [System.IO.Path]::GetFullPath([string]$record.projectRoot)
if (-not [string]::Equals($recordedRoot, $projectRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'O registro pertence a outro projeto. Nenhum processo foi encerrado.'
}

$processSnapshot = @(Get-CimInstance Win32_Process)

function Find-ProcessSnapshot {
    param([int]$ProcessId)
    return $processSnapshot | Where-Object { [int]$_.ProcessId -eq $ProcessId } | Select-Object -First 1
}

function Confirm-RecordedProcess {
    param(
        [Parameter(Mandatory)]$Entry,
        [Parameter(Mandatory)][ValidateSet('backend', 'frontend')][string]$Kind
    )

    $processId = 0
    if (-not [int]::TryParse([string]$Entry.pid, [ref]$processId) -or $processId -le 0) {
        throw "PID invalido para $Kind. Nenhum processo foi encerrado."
    }

    $process = Find-ProcessSnapshot -ProcessId $processId
    if (-not $process) {
        Write-Host "$Kind ja esta encerrado (PID $processId)."
        return $null
    }

    $expectedStart = [DateTime]::Parse(
        [string]$Entry.iniciadoEm,
        [Globalization.CultureInfo]::InvariantCulture,
        [Globalization.DateTimeStyles]::RoundtripKind
    ).ToUniversalTime()
    $actualStart = ([DateTime]$process.CreationDate).ToUniversalTime()
    if ([Math]::Abs(($actualStart - $expectedStart).TotalSeconds) -gt 5) {
        throw "O PID $processId foi reutilizado por outro processo. Nenhum processo foi encerrado."
    }

    $commandLine = [string]$process.CommandLine
    $validCommand = if ($Kind -eq 'backend') {
        $commandLine -match '(?i)mvnw\.cmd' -and $commandLine -match '(?i)spring-boot:run'
    } else {
        $commandLine -match '(?i)npm(?:\.cmd)?' -and
        $commandLine -match '(?i)\bstart\b' -and
        $commandLine -match '(?i)4200'
    }
    if (-not $validCommand) {
        throw "O PID $processId nao corresponde ao $Kind iniciado pelo SGR. Nenhum processo foi encerrado."
    }

    return $process
}

$backendRoot = Confirm-RecordedProcess -Entry $record.backend -Kind 'backend'
$frontendRoot = Confirm-RecordedProcess -Entry $record.frontend -Kind 'frontend'

$roots = @()
if ($backendRoot) {
    $roots += [PSCustomObject]@{ Process = $backendRoot; Kind = 'backend' }
}
if ($frontendRoot) {
    $roots += [PSCustomObject]@{ Process = $frontendRoot; Kind = 'frontend' }
}

foreach ($root in $roots) {
    $allowedNames = if ($root.Kind -eq 'backend') {
        @('cmd.exe', 'java.exe', 'conhost.exe')
    } else {
        @('cmd.exe', 'node.exe', 'conhost.exe')
    }

    $tree = @([PSCustomObject]@{ Process = $root.Process; Depth = 0 })
    $knownIds = [Collections.Generic.HashSet[int]]::new()
    [void]$knownIds.Add([int]$root.Process.ProcessId)
    $depthById = @{}
    $depthById[[int]$root.Process.ProcessId] = 0

    do {
        $added = $false
        foreach ($candidate in $processSnapshot) {
            if ($knownIds.Contains([int]$candidate.ParentProcessId) -and
                -not $knownIds.Contains([int]$candidate.ProcessId)) {
                [void]$knownIds.Add([int]$candidate.ProcessId)
                $candidateDepth = [int]$depthById[[int]$candidate.ParentProcessId] + 1
                $depthById[[int]$candidate.ProcessId] = $candidateDepth
                $tree += [PSCustomObject]@{ Process = $candidate; Depth = $candidateDepth }
                $added = $true
            }
        }
    } while ($added)

    $unexpected = $tree | Where-Object {
        ([string]$_.Process.Name).ToLowerInvariant() -notin $allowedNames
    }
    if ($unexpected) {
        $names = ($unexpected.Process.Name | Sort-Object -Unique) -join ', '
        throw "A arvore de $($root.Kind) contem processos inesperados ($names). Nenhum processo dessa arvore foi encerrado."
    }

    foreach ($item in ($tree | Sort-Object Depth -Descending)) {
        $current = Get-CimInstance Win32_Process -Filter "ProcessId = $($item.Process.ProcessId)" -ErrorAction SilentlyContinue
        if (-not $current) { continue }
        if (([DateTime]$current.CreationDate) -ne ([DateTime]$item.Process.CreationDate)) {
            throw "O PID $($item.Process.ProcessId) mudou durante a validacao. Interrupcao cancelada."
        }
        Stop-Process -Id ([int]$current.ProcessId) -Force -ErrorAction Stop
    }

    Write-Host "$($root.Kind) encerrado com seguranca."
}

Remove-Item -LiteralPath $processFile -Force
Write-Host 'Apresentacao encerrada. O registro temporario de processos foi removido.' -ForegroundColor Green
