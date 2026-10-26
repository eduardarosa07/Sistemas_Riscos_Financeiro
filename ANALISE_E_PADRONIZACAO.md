# Análise e padronização

## Direção adotada

O projeto foi consolidado como uma aplicação única e simples. O backend, a interface e a autenticação são entregues pelo mesmo Spring Boot. Isso reduz configuração, evita CORS no desenvolvimento local e elimina a necessidade de Node/npm apenas para executar as telas.

## Padrão Java

O pacote raiz é somente `sr`:

- `sr.config`: infraestrutura e configuração;
- `sr.controller`: entrada HTTP;
- `sr.dto`: contratos da API;
- `sr.exception`: erros de aplicação;
- `sr.model`: entidades persistidas;
- `sr.repository`: Spring Data JPA;
- `sr.service`: regras de negócio.

Não existem pacotes `br.ueg...`, H2 ou Databricks.

## Banco

A persistência está concentrada no PostgreSQL. O pgAdmin é a ferramenta recomendada para administração visual, mas não é uma dependência da aplicação. A conexão do Java é feita pelo driver JDBC PostgreSQL e Spring Data JPA.

## Front-end

A interface foi reconstruída com base nas telas de referência fornecidas. Ela está em `src/main/resources/static` e possui:

- layout lateral responsivo;
- Login;
- Home;
- Clientes;
- Ocorrências;
- telas específicas de PLD, Chargeback, KYC e Fraude;
- Relatórios;
- Usuários.

As telas não usam dados fictícios internos. Elas consultam a API real. Banco vazio significa tela vazia/indicadores zerados.

## Login

O login anterior apenas validava a credencial. Nesta revisão ele também estabelece sessão HTTP. Um interceptor protege as APIs de negócio. Senhas permanecem armazenadas com BCrypt e nunca são retornadas no DTO de usuário.

O usuário administrador inicial é criado somente quando não existe nenhum usuário cadastrado.

## Usuários

A interface já consome os endpoints existentes para:

- listar;
- cadastrar;
- editar nome/e-mail/perfil;
- ativar/inativar.

## Home e relatórios

Nesta etapa, indicadores são calculados no navegador a partir das APIs existentes. Isso mantém a implementação simples e funcional. Quando o volume de dados crescer, recomenda-se criar consultas agregadas no PostgreSQL e DTOs específicos de dashboard para não transferir listas completas apenas para calcular totais.

## Pontos que ainda são evolução futura

1. Criar autorização por perfil (Administrador, Gestor, Analista etc.), e não apenas autenticação.
2. Criar fluxo de troca/redefinição de senha.
3. Criar DTOs para todas as entidades de negócio para não expor entidades JPA diretamente nos controllers.
4. Implementar paginação no backend para clientes e ocorrências.
5. Criar endpoints agregados para dashboard/SLA/relatórios.
6. Adicionar migrations com Flyway quando o modelo de dados estabilizar.
7. Adicionar testes unitários e de integração por domínio.
8. Em publicação real: HTTPS, CSRF, gestão externa de segredos e política de sessão/senha.

## Validações executadas

- JavaScript validado sintaticamente com `node --check`.
- Busca por referências aos pacotes antigos, H2 e Databricks realizada na árvore Java/configuração.
- Estrutura de imports internos revisada na reorganização anterior e preservada.
- O Maven Wrapper não pôde concluir a compilação neste ambiente porque precisa baixar a distribuição Maven e o acesso ao repositório externo foi bloqueado. Portanto, a compilação final deve ser executada localmente com `mvnw.cmd clean test` após abrir o projeto.
