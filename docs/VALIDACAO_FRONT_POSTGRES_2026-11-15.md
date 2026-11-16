# Validação do frontend e PostgreSQL — 15/11/2026

## Resultados e limites

| Verificação | Ambiente | Resultado |
|---|---|---|
| Navegação, risco, KYC, seleção, exportações e guard | Edge headless, build de produção, API simulada | 8 cenários aprovados, sem erros JavaScript |
| API sem credenciais | Spring Boot/Tomcat reais, banco local | 4 testes HTTP aprovados |
| Schema JPA | PostgreSQL 18.3, sistema_riscos | Aplicação iniciou com ddl-auto=validate |
| Consultas JPA e serviço de histórico | PostgreSQL real, transações readOnly | 5 testes aprovados, nenhuma falha/erro/skip |
| Regressão após correção da consulta de fraude | JUnit/Mockito/MockMvc sem banco | 113 testes aprovados |
| Diagnóstico de schema e consistência | psql, BEGIN READ ONLY e ROLLBACK | Executado sem alterar dados |

Os cenários de navegador usam exclusivamente respostas e credenciais sintéticas
em memória: login de Analista, risco recebido na lista/detalhe, chargeback sem
classificação inventada, troca de parecer KYC, KYC legado no histórico, troca
rápida de ocorrência, downloads Excel/PDF com 5.000 registros e redirecionamento
de Administrador sem permissão para clientes.

Isso não equivale a navegar com uma conta real e API/banco reais de ponta a ponta.
Esse aceite segue pendente da configuração de uma conta de ensaio. Não foi
solicitada nem exposta senha de usuário no relatório.

## PostgreSQL local

- Serviço PostgreSQL ativo, versão 18.3, banco sistema_riscos acessível.
- 14 tabelas públicas; 120 clientes, 120 análises KYC, 120 registros de risco,
  510 ocorrências e 3 eventos em ocorrencia_historico.
- Estados KYC na origem: 5 PENDENTE, 108 APROVADO, 4 REPROVADO, 1 ACEITO e
  2 NAO_ACEITO. As consultas JPA de fila e histórico foram conferidas contra
  os estados persistidos.
- Zero históricos órfãos, zero clientes sem risco, zero riscos sem cliente,
  zero e-mails equivalentes duplicados, zero KYC sem ocorrência e zero KYC
  encerrados indevidamente na fila.
- ocorrencia_historico existe com as colunas usadas pelo código, chaves
  estrangeiras e trigger de imutabilidade. A função foi inspecionada e lança
  exceção para UPDATE/DELETE; não executamos esses comandos para testá-la.
- Há trigger que impede alterar ocorrência concluída e exige decisão e
  justificativa na transição de conclusão. Sua definição foi inspecionada.

A pendência da auditoria sobre a existência de ocorrencia_historico está
resolvida para esta instância local. A documentação/DDL versionados continuam
diferentes do schema local: o script 06 descreve ocorrencia_tratativa_evento,
enquanto esta instância utiliza ocorrencia_historico. Não foi aplicada migração.

Foram encontradas 439 ocorrências concluídas sem decisão registrada: 136 de
fraude, 108 de PLD, 83 de chargeback e 112 de KYC. A consulta constata ausência
de informação; não demonstra corrupção nem permite reconstruir decisões
históricas. Não preencher com valores presumidos. O acervo legado requer aceite
de rastreabilidade separado dos novos fluxos.

## Falha encontrada somente na integração real

Antes da correção, a chamada de AlertaFraudeRepository.filtrar com todos os
parâmetros nulos falhava no PostgreSQL com `function lower(bytea) does not exist`.
Os testes com mocks não executavam esse SQL.

A consulta foi substituída por Specification/Criteria: somente os filtros
informados produzem predicados. A busca por cliente usa LEFT JOIN nos papéis
opcionais e trata os caracteres de LIKE como texto. O endpoint e a assinatura
existentes foram preservados, sem alteração de schema ou dados.

Os testes de integração cobrem: transação efetivamente somente de leitura,
fila/histórico KYC coerentes, consulta de fraude sem filtros com a mesma
quantidade da tabela, busca por cliente preservando relacionamentos opcionais
e leitura do histórico consolidado.

## Repetição das verificações

Diagnóstico SQL sem dados pessoais:

```powershell
psql -X -w -h localhost -U postgres -d sistema_riscos -v ON_ERROR_STOP=1 -f scripts/validar_postgresql_leitura.sql
```

Teste JPA opt-in, após configurar acesso local ao PostgreSQL:

```powershell
cd backend
$env:DB_READONLY_VALIDATION='true'
.\mvnw.cmd -o '-Dtest=sr.integration.ConsultasPostgresLeituraTest' test
```

O teste força `ddl-auto=validate`, desabilita inicialização SQL, bootstrap e
sincronização, configura o pool com default_transaction_read_only=on e usa
transações Spring readOnly. Também verifica `SHOW transaction_read_only`.
Sem a variável de ativação, a classe é ignorada na execução normal.

O script de navegador está em scripts/validar_front_browser.cjs. Requer
Playwright e um build de produção. Nesta execução, Playwright foi instalado em
pasta temporária externa, sem alterar package.json/package-lock.json do projeto.
SGR_PLAYWRIGHT_MODULE pode indicar esse módulo externo; SGR_BROWSER_PATH pode
indicar o executável do navegador. O primeiro argumento indica a pasta browser
do build. O servidor de ensaio escuta somente em 127.0.0.1:14200 e usa API simulada.

Os servidores temporários foram encerrados. Não foram executadas migrações,
decisões KYC, conclusões de ocorrências ou alterações de clientes no banco real.
Os resultados não promovem automaticamente os 86 requisitos a homologados.
