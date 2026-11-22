# Guia de apresentacao do SGR

Este guia prepara uma demonstracao academica local e controlada. O procedimento
nao executa migracoes, nao altera o schema e nao habilita sincronizacao
automatica.

## Arquitetura apresentada

- Angular: `http://localhost:4200`;
- Spring Boot API: `http://localhost:8080`;
- PostgreSQL: banco `sistema_riscos`.

Clientes, scores e niveis de risco sao fornecidos externamente. O SGR apenas
consulta e apresenta esses dados. A demonstracao com a base real deve priorizar
consultas.

## Preparacao

1. Confirme Java 21, Node.js, npm e PostgreSQL.
2. Confirme que o banco `sistema_riscos` esta acessivel.
3. Execute `npm ci` na pasta `frontend` somente se `node_modules` estiver
   ausente ou precisar ser reconstruido.
4. Tenha uma conta ativa do SGR sem registrar a senha em documentos.
5. Feche processos antigos nas portas 8080 e 4200.

Para validar o codigo antes da demonstracao:

```powershell
.\scripts\validar_codigo.ps1
```

## Inicializacao

Na raiz do repositorio:

```powershell
.\scripts\iniciar_apresentacao.ps1
```

O script solicita a senha do PostgreSQL de forma oculta, configura
`ddl-auto=validate`, desabilita bootstrap e sincronizacao automatica, inicia os
dois servidores e verifica seus endpoints.

O sistema esta pronto quando o terminal informar:

```text
SGR pronto para apresentacao.
```

## Verificacoes rapidas

1. `http://localhost:8080/` identifica o backend.
2. `http://localhost:8080/api/status` responde com sucesso.
3. `http://localhost:4200/login` abre a interface Angular.
4. O login com uma conta ativa redireciona para a tela autorizada pelo perfil.
5. Dashboard, Clientes e Ocorrencias carregam sem erro.

## Massa previamente conferida

Consulta somente de leitura realizada em 22/11/2026:

- cliente `CLI00022`: ativo, risco geral alto, com KYC, PLD, fraude,
  chargeback e 13 ocorrencias;
- ocorrencia `340`: chargeback concluido, com dois eventos no historico.

Use esses identificadores para evitar procurar um caso durante a apresentacao.
Se a base for recarregada, repita a conferencia antes da banca.

## Roteiro sugerido

1. Apresente Angular, Spring Boot e PostgreSQL.
2. Explique que cliente, score e risco vem da fonte externa.
3. Realize login.
4. Mostre o Dashboard e aplique um filtro.
5. Abra o cliente `CLI00022`.
6. Mostre risco geral, scores e secoes relacionadas.
7. Consulte KYC, PLD, Fraude e Chargeback.
8. Abra a ocorrencia `340` e seu historico.
9. Mostre Relatorios.
10. Encerre a sessao.

## Acoes que devem ser evitadas na base real

- executar sincronizacao manual ou automatica;
- executar scripts SQL ou migracoes;
- usar `ddl-auto=update`;
- cadastrar ou alterar clientes, riscos ou dados de origem;
- concluir tratativas ou decidir KYC;
- alterar usuarios durante a demonstracao;
- preencher dados historicos ausentes com valores presumidos.

## Encerramento

Ao terminar:

```powershell
.\scripts\encerrar_apresentacao.ps1
```

O script valida os processos registrados antes de encerrar suas arvores. Os
logs temporarios ficam em `%TEMP%\sgr-apresentacao` e nao pertencem ao Git.

## Diagnostico rapido

Se houver falha, confira:

- se o PostgreSQL esta iniciado;
- se as portas 8080 e 4200 estao livres;
- se a credencial do banco foi informada corretamente;
- `%TEMP%\sgr-apresentacao\backend-error.log`;
- `%TEMP%\sgr-apresentacao\frontend-error.log`.

Nao execute migracoes nem altere o schema durante a apresentacao para corrigir
uma falha de inicializacao.
