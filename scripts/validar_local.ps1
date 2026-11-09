# Executar no Windows PowerShell na pasta raiz: .\scripts\validar_local.ps1
# Exige Node.js, Java 21 e Maven Wrapper; PostgreSQL deve estar configurado para integração.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
Push-Location "$raiz\frontend"
try {
  Write-Host 'Instalando dependencias do frontend (npm ci)...'
  npm ci
  if ($LASTEXITCODE -ne 0) { throw 'npm ci falhou' }
  Write-Host 'Executando testes unitarios do frontend...'
  npm run test:unit
  if ($LASTEXITCODE -ne 0) { throw 'Testes do frontend falharam' }
  Write-Host 'Compilando Angular (mantendo layout original)...'
  npm run build
  if ($LASTEXITCODE -ne 0) { throw 'Build Angular falhou' }
} finally { Pop-Location }
Push-Location "$raiz\backend"
try {
  Write-Host 'Executando JUnit 5 e testes de Spring...'
  .\mvnw.cmd test
  if ($LASTEXITCODE -ne 0) { throw 'Testes Maven falharam' }
} finally { Pop-Location }
Write-Host 'Verificacoes automatizadas locais concluidas. Execute adicionalmente os testes HTTP com o backend ativo.'
