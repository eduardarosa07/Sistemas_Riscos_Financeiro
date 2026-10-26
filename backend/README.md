# Sistema de Riscos Financeiros - Backend

Backend Spring Boot organizado sob o pacote raiz `sr`, usando PostgreSQL como unico banco de dados da aplicacao.
O pgAdmin pode ser usado para criar, consultar e administrar o banco PostgreSQL.

## Requisitos

- Java 25 (LTS)
- PostgreSQL
- pgAdmin (opcional, recomendado para administracao visual)
- Maven Wrapper incluido no projeto

## Banco local

1. No pgAdmin, conecte-se ao seu servidor PostgreSQL.
2. Crie o banco `sistema_riscos` (o script `database/01-criar-banco.sql` pode ser usado como referencia).
3. Ajuste usuario e senha do PostgreSQL por variaveis de ambiente ou em uma configuracao local que nao seja versionada.

Valores padrao de desenvolvimento:

- URL: `jdbc:postgresql://localhost:5432/sistema_riscos`
- usuario: `postgres`
- senha: `postgres`

Variaveis suportadas: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JPA_DDL_AUTO`, `JPA_SHOW_SQL` e `SERVER_PORT`.

> Para desenvolvimento, `JPA_DDL_AUTO=update` permite ao Hibernate criar/ajustar as tabelas mapeadas. Para ambientes controlados, prefira migracoes versionadas e `validate`.

## Executar

Windows PowerShell:

```powershell
cd backend
$env:DB_PASSWORD="SUA_SENHA_DO_POSTGRES"
.\mvnw.cmd spring-boot:run
```

A API inicia, por padrao, em `http://localhost:8080`.

## Estrutura

- `sr.config`: configuracoes compartilhadas
- `sr.controller`: endpoints REST
- `sr.dto`: contratos de entrada/saida da API
- `sr.exception`: excecoes e tratamento global
- `sr.model`: entidades JPA
- `sr.repository`: acesso a dados com Spring Data JPA
- `sr.service`: regras de negocio e orquestracao

## Bancos removidos nesta revisao

Esta versao nao utiliza H2 nem Databricks. A persistencia foi consolidada exclusivamente em PostgreSQL.
