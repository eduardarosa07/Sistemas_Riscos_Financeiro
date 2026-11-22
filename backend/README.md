# Backend SGR

API REST Spring Boot do Sistema de Gerenciamento de Riscos Financeiros.

## Responsabilidade

O backend consulta clientes, contas, transacoes, KYC, PLD, fraude, chargeback,
regras e classificacoes de risco fornecidos pela origem externa. O SGR nao
calcula nem cria scores ou niveis de risco.

As escritas proprias do sistema ficam restritas a usuarios, auditoria,
tratativas e historicos.

## Requisitos

- Java 21;
- PostgreSQL com o banco `sistema_riscos` preparado;
- Maven Wrapper incluido.

## Variaveis de ambiente

| Variavel | Uso | Padrao |
| --- | --- | --- |
| `DB_URL` | URL JDBC | `jdbc:postgresql://localhost:5432/sistema_riscos` |
| `DB_USERNAME` | Usuario PostgreSQL | `postgres` |
| `DB_PASSWORD` | Senha PostgreSQL | vazio |
| `JPA_DDL_AUTO` | Validacao do schema | `validate` |
| `JPA_SHOW_SQL` | Exibir SQL | `false` |
| `SERVER_PORT` | Porta da API | `8080` |
| `FRONTEND_ORIGIN` | Origem CORS | `http://localhost:4200` |
| `SGR_SINCRONIZACAO_AUTOMATICA` | Sincronizacao agendada | `false` |

Credenciais nao devem ser gravadas no repositorio.

## Executar no PowerShell

```powershell
$env:SGR_SINCRONIZACAO_AUTOMATICA = 'false'
.\mvnw.cmd spring-boot:run
```

Defina `DB_PASSWORD` de forma segura no ambiente antes de iniciar. O script
`scripts/iniciar_apresentacao.ps1` da raiz solicita a senha de forma oculta.

A API fica disponivel em `http://localhost:8080`. O status publico pode ser
consultado em `http://localhost:8080/api/status`.

A interface e executada separadamente pelo Angular na porta 4200.

## Garantias de inicializacao

- Hibernate usa `validate` e nao cria nem altera tabelas.
- Scripts SQL nao sao executados automaticamente.
- Bootstrap administrativo permanece desabilitado quando as variaveis
  `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD` estao vazias.
- Sincronizacao automatica permanece desabilitada por padrao.

## Testes seguros

Os testes unitarios e MockMvc usam mocks. Os testes que iniciam o contexto
Spring ou acessam PostgreSQL sao opt-in por variavel de ambiente.

```powershell
.\mvnw.cmd test
```

Para a apresentacao, use preferencialmente o script da raiz:
`scripts/iniciar_apresentacao.ps1`.
