# SGR — Guia de execução e verificação (Windows / PowerShell)

## Base preservada

Este pacote parte exclusivamente de `Sistemas_Riscos_Financeiros(2).zip`. Mantém o layout, os estilos e os arquivos CSS existentes; as alterações HTML se limitam a filtros, colunas e ações necessárias para os requisitos.

## Pré-requisitos

- Java **21** (`java -version`), Node.js compatível com Angular 20, PostgreSQL local.
- Internet na instalação inicial para baixar dependências Maven e `npm ci`.
- Banco `sistema_riscos` e dados das tabelas operacionais; o ZIP original **não** contém dump completo dos dados/schemas de negócio.
- Faça backup do PostgreSQL antes de qualquer migração.

## Banco de dados

1. Se o banco ainda não existir, crie-o no pgAdmin conforme `backend/database/01-criar-banco.sql`.
2. Garanta que as tabelas `dim_cliente`, `fato_kyc`, `fato_alerta_pld`, `fato_alerta_fraude`, `fato_chargeback`, `fato_transacao` e `fato_risco_cliente` existam no banco (importação dos dados da atividade/projeto). **Não** execute as migrações em banco de produção sem backup.
3. Execute, no banco certo e nessa ordem, os arquivos `02-criar-tabela-usuario.sql`, `03-migrar-perfis-usuarios.sql`, `04-auditoria-e-datas.sql`, `05-restricoes-usuarios.sql`, `06-tratativas.sql` e `08-consolidacao-sprint-8.sql`.
4. O script 07 contém dados sintéticos/opcionais de teste: **não** execute indiscriminadamente na base original.
5. Quando for necessário criar entidades JPA de uma base vazia, o Spring pode usar `JPA_DDL_AUTO=update` **somente para inicialização de ambiente de desenvolvimento**. Depois disso, retorne para `validate` e confira as migrações. `update` não substitui migrações em produção.

## Backend

```powershell
cd backend
$env:DB_URL='jdbc:postgresql://localhost:5432/sistema_riscos'
$env:DB_USERNAME='postgres'
$env:DB_PASSWORD='SUA_SENHA_LOCAL'
$env:JPA_DDL_AUTO='validate'
$env:APP_ADMIN_EMAIL='admin@exemplo.local'
$env:APP_ADMIN_PASSWORD='ESCOLHA_UMA_SENHA_FORTE'
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

A conta bootstrap só é criada se **não houver nenhum usuário** no banco. Não salve senhas reais no repositório.

## Frontend

Em outro PowerShell:

```powershell
cd frontend
npm ci
npm run test:unit
npm run build
npm start
```

Abra `http://localhost:4200`. A API estará em `http://localhost:8080`. A configuração padrão assume esses endereços; ajuste `FRONTEND_ORIGIN` no backend se mudar a origem Angular.

## Testes em ordem

1. `frontend/npm run test:unit`: filtros e regras puras de frontend.
2. `frontend/npm run build`: compilação completa Angular e verificação dos templates.
3. `backend/.\mvnw.cmd test`: testes unitários JUnit com Mockito. O teste de contexto real do banco só é habilitado com `DB_INTEGRATION=true`.
4. Com backend/PostgreSQL ativos, na raiz: `python -m unittest -v scripts.teste_integracao_http`.
5. Para cenários autenticados, defina `SGR_ANALISTA_EMAIL`, `SGR_ANALISTA_SENHA`, `SGR_USUARIO_EMAIL`, `SGR_USUARIO_SENHA` antes de executar o comando Python.
6. Homologação manual de telas: login, perfil, Home, Clientes, detalhe, KYC, PLD, Fraude, Chargeback, fila, histórico, relatório PDF e Excel.

**Não rode testes de decisão KYC ou conclusão de ocorrências em dados reais**: para isso, utilize uma cópia sintética de homologação, pois o encerramento é definitivo.

## Critério para atestar 100%

`docs/MATRIZ_86_REQUISITOS.csv` contém o catálogo de todos os IDs. Para declarar homologação total, cada requisito deve ter um teste específico (ou conjunto de testes), evidência executada e resultado aprovado. A existência do teste no código **não** comprova, por si só, que ele passou.
