# Guia de apresentacao do SGR - 23/11/2026

Este guia prepara uma demonstracao academica local e controlada. Ele nao
executa migracoes e nao habilita sincronizacao automatica.

## Arquitetura apresentada

- Angular: `http://localhost:4200`;
- Spring Boot API: `http://localhost:8080`;
- PostgreSQL: banco `sistema_riscos`.

Clientes, scores e niveis de risco sao dados externos. O SGR apenas consulta
e exibe esses dados. A demonstracao com a base real deve priorizar consultas.

## Preparacao anterior

1. Confirme Java 21, Node.js, npm e PostgreSQL.
2. Confirme que o banco `sistema_riscos` esta acessivel.
3. Na pasta `frontend`, execute `npm ci` somente se `node_modules` estiver
   ausente ou desatualizado.
4. Tenha uma conta ativa do SGR sem registrar a senha em documentos.
5. Feche processos antigos nas portas 8080 e 4200.

## Inicializacao recomendada

Na raiz do repositorio:

```powershell
.\scripts\iniciar_apresentacao.ps1
```

O script:

- solicita a senha do PostgreSQL de forma oculta;
- usa `ddl-auto=validate`;
- deixa o bootstrap administrativo desabilitado;
- desabilita a sincronizacao automatica;
- inicia backend e Angular em processos separados;
- grava logs somente na pasta temporaria do Windows;
- verifica os endpoints antes de informar que o sistema esta pronto.

## Encerramento seguro

Ao terminar, execute na raiz:

```powershell
.\scripts\encerrar_apresentacao.ps1
```

O script le o registro criado na pasta temporaria, valida raiz do projeto,
PID, horario de inicio, linha de comando e arvore de processos antes de
encerrar. Se um PID tiver sido reutilizado ou nao corresponder ao SGR, nenhum
processo arbitrario sera encerrado.

## Verificacoes rapidas

1. API: `http://localhost:8080/` deve identificar o backend e
   `http://localhost:8080/api/status` deve responder.
2. Interface: `http://localhost:4200/login` deve abrir.
3. Entre com uma conta ativa do SGR.
4. Confirme Dashboard, Clientes e Ocorrencias antes da banca.

## Massa previamente conferida

Consulta somente de leitura realizada em 22/11/2026:

- cliente `CLI00022`: ativo, risco geral alto, com dados em KYC, PLD, fraude,
  chargeback e 13 ocorrencias;
- ocorrencia `340`: chargeback concluido, com dois eventos no historico.

Use esses identificadores para evitar procurar um caso durante a apresentacao.
Se a base for recarregada, repita a conferencia antes da banca.

## Roteiro sugerido

1. Explique a arquitetura Angular, Spring Boot e PostgreSQL.
2. Informe que cliente e risco vem de fonte externa.
3. Realize login.
4. Mostre o Dashboard e aplique um filtro.
5. Abra Clientes e selecione um cliente.
6. Mostre risco geral, scores e secoes relacionadas.
7. Consulte KYC, PLD, Fraude e Chargeback.
8. Abra a fila de ocorrencias e um historico existente.
9. Mostre Relatorios.
10. Encerre a sessao.

## Acoes evitadas na base real

- nao executar sincronizacao manual;
- nao habilitar sincronizacao automatica;
- nao executar scripts SQL ou migracoes;
- nao usar `ddl-auto=update`;
- nao cadastrar nem alterar clientes, riscos ou dados de origem;
- nao demonstrar tratativa, decisao KYC ou gestao de usuarios sem uma copia
  descartavel do banco.

## Arquivos que nao pertencem ao pacote final

- `backend.zip` local;
- `backend/target`;
- `frontend/node_modules`;
- `frontend/.angular`;
- `frontend/.unit-test`;
- `frontend/dist`;
- logs e arquivos de credenciais.

## Em caso de falha

Os logs ficam em `%TEMP%\sgr-apresentacao`. Verifique primeiro:

- `backend-error.log`;
- `frontend-error.log`;
- se PostgreSQL esta iniciado;
- se as portas 8080 e 4200 estao livres;
- se a credencial do banco foi informada corretamente.

Nao execute migracoes ou altere o schema durante a apresentacao para tentar
corrigir uma falha de inicializacao.
