# Arquitetura — Sistemas Riscos Financeiros

## Stack
- Frontend web: Angular (porta 4200 em desenvolvimento)
- Backend: Java + Spring Boot (porta 8080)
- Persistência: Spring Data JPA / Hibernate
- Banco: PostgreSQL `sistema_riscos`
- pgAdmin: somente administração do PostgreSQL

## Camadas do backend
`Controller -> DTO -> Service -> Repository -> Model/Entity -> PostgreSQL`

O Angular nunca acessa o PostgreSQL diretamente. Ele consome `/api/**` em JSON.

## Usuários e segurança
A tabela `usuario` contém nome, email, `senha_hash`, perfil e status.
A senha original nunca é gravada no banco. O backend usa BCrypt.
As respostas da API usam `UsuarioResponse`, que não contém o hash.
O email é único e normalizado em minúsculas.
Perfis ficam como texto por enquanto, permitindo definir os perfis posteriormente.
O status é restrito no banco a `ATIVO` ou `INATIVO`.

Não existe administrador/senha padrão no código. Para criar o primeiro administrador
automaticamente em ambiente local, defina `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD`
antes de iniciar o backend.

## Banco existente
`spring.jpa.hibernate.ddl-auto=validate`: o Hibernate valida os Models contra as
tabelas existentes e não cria/altera silenciosamente o schema.

Antes de iniciar esta versão, execute:
`backend/database/02-criar-tabela-usuario.sql`
no banco `sistema_riscos`.
