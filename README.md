# Sistemas Riscos Financeiros

Aplicação web acadêmica para gerenciamento de riscos financeiros, organizada como uma única aplicação Spring Boot com PostgreSQL.

## Estrutura

```text
Sistemas Riscos Financeiros/
└── backend/
    ├── database/                 # scripts auxiliares do PostgreSQL
    ├── src/main/java/sr/
    │   ├── config/               # configurações, sessão e inicialização
    │   ├── controller/           # endpoints REST e rotas web
    │   ├── dto/                  # contratos de entrada/saída da API
    │   ├── exception/            # tratamento centralizado de erros
    │   ├── model/                # entidades JPA
    │   ├── repository/           # acesso ao PostgreSQL
    │   └── service/              # regras de negócio
    └── src/main/resources/
        ├── static/               # front-end HTML/CSS/JavaScript
        └── application.properties
```

Não existe H2 nem Databricks. O único SGBD da aplicação é PostgreSQL. O pgAdmin pode ser usado para criar, consultar e administrar o banco.

## Banco local

1. Abra o pgAdmin e conecte ao seu PostgreSQL.
2. Execute `backend/database/01-criar-banco.sql` ou crie um banco chamado `sistema_riscos`.
3. Por padrão a aplicação tenta usar:
   - URL: `jdbc:postgresql://localhost:5432/sistema_riscos`
   - usuário: `postgres`
   - senha: `postgres`
4. Caso sua instalação use outra senha, configure `DB_PASSWORD` no terminal antes de iniciar. Também existem `DB_URL` e `DB_USERNAME`.

O Hibernate está configurado com `ddl-auto=update` para facilitar o desenvolvimento acadêmico e cria/atualiza as tabelas a partir das entidades JPA.

## Executar no Windows / VS Code

No terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Depois abra `http://localhost:8080`.

### Primeiro acesso

Quando a tabela `usuario` está vazia, a aplicação cria automaticamente:

- e-mail: `admin@sgr.com`
- senha: `Admin@123`

Essa credencial é somente para o primeiro ambiente local. Troque-a antes de qualquer uso real.

## Telas implementadas

- Login com sessão HTTP
- Home com indicadores calculados a partir das APIs
- Clientes com busca e filtros
- Ocorrências consolidadas
- PLD
- Chargeback
- KYC
- Fraude
- Relatórios com impressão e exportação CSV
- Usuários com cadastro, edição, ativação e inativação

## Arquitetura de autenticação

O login valida e-mail e senha BCrypt no backend. Depois do login, o usuário fica registrado na sessão HTTP. As rotas `/api/**` são protegidas por um interceptor, exceto login, consulta da sessão e status público. O front-end não guarda senha.

> Esta é uma proteção adequada para a etapa acadêmica/local do projeto. Se o sistema for publicado em ambiente real, a próxima evolução deve incluir autorização por perfil, CSRF, política de senha, HTTPS, expiração/revogação de sessão e gestão de segredos.

## Observação sobre os indicadores

Os indicadores da Home e Relatórios são derivados dos registros retornados pelo PostgreSQL. Com o banco vazio, eles aparecem zerados. Não há mais dados fictícios escondidos no front-end.


## Frontend Angular

O frontend separado está em `frontend/`. Execute `npm install` e `npm start`; acesse `http://localhost:4200`.
