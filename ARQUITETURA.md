# Arquitetura do SGR

## Visao geral

O Sistema de Gerenciamento de Riscos Financeiros e composto por tres processos
separados:

```text
Angular (localhost:4200)
        |
        | HTTP/JSON, sessao e CSRF
        v
Spring Boot REST (localhost:8080)
        |
        | JPA e JDBC
        v
PostgreSQL - sistema_riscos (localhost:5432)
```

O Angular nunca acessa o PostgreSQL diretamente. O backend nao entrega a
interface web: a porta 8080 identifica a API e a porta 4200 entrega o frontend.

## Responsabilidades

### Frontend Angular

- autenticar o usuario por meio da API;
- apresentar dashboard, clientes, KYC, PLD, fraude e chargeback;
- apresentar fila, contexto e historico de ocorrencias;
- aplicar permissoes visuais conforme o perfil;
- gerar os arquivos PDF e Excel disponibilizados na interface.

### Backend Spring Boot

- expor endpoints REST em `/api/**`;
- validar sessao, perfil e token CSRF;
- consultar os dados financeiros no PostgreSQL;
- calcular agregacoes gerenciais sem recalcular risco de cliente;
- persistir apenas dados pertencentes ao SGR;
- devolver respostas JSON para o Angular.

### PostgreSQL

- armazenar as tabelas externas consultadas pelo SGR;
- armazenar usuarios, auditoria, tratativas e historicos do proprio sistema;
- aplicar as restricoes e relacionamentos existentes no schema preparado.

## Origem e propriedade dos dados

Clientes, contas, transacoes, KYC, PLD, fraude, chargeback, scores e niveis de
risco sao fornecidos por uma origem externa. O SGR apenas consulta e apresenta
esses dados. Ele nao cadastra clientes e nao gera, recalcula ou reclassifica
scores e niveis de risco.

Quando a origem nao fornece risco ou score, a ausencia deve permanecer visivel,
sem ser convertida em zero ou em uma classificacao inventada. Contagens e somas
de registros existentes para o dashboard nao constituem calculo de risco.

O SGR pode gravar apenas seus dados operacionais:

- usuarios e versao de sessao;
- auditoria administrativa;
- responsabilidade, decisao e justificativa de tratativas;
- historico de eventos das ocorrencias.

## Camadas do backend

O fluxo preferencial e:

```text
Controller -> DTO -> Service -> Repository/JdbcTemplate -> PostgreSQL
```

- `controller`: contratos HTTP e validacao de entrada;
- `dto`: objetos enviados e recebidos pela API;
- `service`: consultas, regras e transacoes;
- `repository`: acesso JPA;
- `model`: entidades persistidas;
- `config`: CORS, sessao, autorizacao e CSRF;
- `exception`: padronizacao de falhas da API.

Consultas agregadas que exigem SQL especifico permanecem na camada de servico e
usam `JdbcTemplate`, evitando SQL diretamente nos componentes Angular.

## Autenticacao e autorizacao

- login por e-mail e senha protegida com BCrypt;
- sessao HTTP com cookie `HttpOnly` e `SameSite=Lax`;
- token CSRF nas operacoes de escrita;
- perfis `USUARIO`, `ANALISTA`, `GESTOR` e `ADMINISTRADOR`;
- revalidacao do status e da versao da sessao;
- administracao de usuarios restrita ao perfil autorizado;
- limite local de tentativas de login mantido em memoria.

## Inicializacao segura

O Hibernate usa `ddl-auto=validate`. A aplicacao valida o schema existente e nao
cria nem altera tabelas durante a inicializacao. A sincronizacao automatica fica
desabilitada por padrao e as credenciais sao recebidas por variaveis de ambiente.

Os SQLs em `backend/database` sao historicos e manuais. Eles nao sao executados
pelo build nem pela aplicacao e nao devem ser aplicados na base da apresentacao.
O schema versionado ainda precisa ser consolidado antes de ser usado para criar
um banco novo.

## Execucao e verificacao

- preparacao da demonstracao: `docs/GUIA_APRESENTACAO.md`;
- validacao consolidada: `docs/VALIDACAO_ATUAL.md`;
- catalogo oficial: `docs/MATRIZ_86_REQUISITOS.csv`;
- rastreabilidade: `docs/RASTREABILIDADE_86.md`.
