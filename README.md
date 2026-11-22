# Sistema de Gerenciamento de Riscos Financeiros (SGR)

Aplicacao academica web para consulta e tratamento de ocorrencias de risco.

O SGR **nao cria clientes, scores ou niveis de risco**. Esses dados sao
fornecidos por uma fonte externa, persistidos no PostgreSQL e apenas
consultados pelo sistema. O SGR grava somente dados proprios, como usuarios,
tratativas e historicos.

## Arquitetura

```text
Angular (localhost:4200)
        |
        | HTTP/JSON + sessao
        v
Spring Boot REST (localhost:8080)
        |
        v
PostgreSQL - sistema_riscos (localhost:5432)
```

- `frontend/`: aplicacao Angular.
- `backend/`: API Spring Boot.
- `backend/database/`: scripts SQL historicos e auxiliares. Nao sao executados
  automaticamente.
- `scripts/`: validacao e inicializacao local.
- `docs/`: requisitos, rastreabilidade e evidencias.

O backend nao entrega paginas da interface. Abra sempre o Angular em
`http://localhost:4200`.

## Requisitos locais

- Java 21;
- Node.js e npm;
- PostgreSQL;
- banco `sistema_riscos` previamente preparado.

## Apresentacao

O modo recomendado desabilita sincronizacao automatica, usa Hibernate apenas
para validar o schema e nao executa migracoes.

No PowerShell, a partir da raiz:

```powershell
.\scripts\iniciar_apresentacao.ps1
```

O script pede a senha do PostgreSQL sem exibi-la, inicia backend e frontend e
verifica as portas locais.

Ao terminar a demonstracao:

```powershell
.\scripts\encerrar_apresentacao.ps1
```

Consulte o roteiro completo em
[`docs/GUIA_APRESENTACAO.md`](docs/GUIA_APRESENTACAO.md).

## Execucao manual

Backend:

```powershell
$env:DB_USERNAME = 'postgres'
$env:SGR_SINCRONIZACAO_AUTOMATICA = 'false'
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Antes do comando acima, defina `DB_PASSWORD` de forma segura no ambiente. Para
evitar que a senha apareca no terminal ou no historico, prefira o script de
apresentacao, que faz a leitura oculta.

Frontend, em outro terminal:

```powershell
Set-Location frontend
npm start
```

Depois acesse `http://localhost:4200/login`.

## Configuracao segura

As credenciais do banco nao possuem valor fixo no codigo. Use:

- `DB_URL` (opcional; padrao local `sistema_riscos`);
- `DB_USERNAME`;
- `DB_PASSWORD`;
- `SGR_SINCRONIZACAO_AUTOMATICA=false` para demonstracao;
- `JPA_DDL_AUTO=validate`.

Nao inclua credenciais, `node_modules`, `target`, `.angular`, `dist`, logs ou
arquivos ZIP locais no pacote de entrega.

## Validacao

```powershell
.\scripts\validar_codigo.ps1
```

Testes de integracao PostgreSQL sao opt-in e devem ser executados somente em
ambiente controlado. A matriz oficial esta em
[`docs/MATRIZ_86_REQUISITOS.csv`](docs/MATRIZ_86_REQUISITOS.csv).

O resultado consolidado das verificacoes realizadas esta em
[`docs/VALIDACAO_ATUAL.md`](docs/VALIDACAO_ATUAL.md).
