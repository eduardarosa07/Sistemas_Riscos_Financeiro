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

## Origem dos dados de clientes e risco

Diretriz confirmada pelo responsável pelo projeto em 15/11/2026: os dados de
clientes, scores e níveis de risco são fornecidos por uma fonte externa. O SGR
apenas consulta esses dados e os exibe na interface, por meio da API e das tabelas
de origem. Não cabe ao SGR cadastrar clientes nem gerar, calcular ou reclassificar
seus scores e níveis de risco, seja no frontend, no backend ou no banco de dados.

Quando o risco ou score não for informado pela fonte, a interface deve sinalizar
a ausência do dado, sem inventar uma classificação ou transformar a ausência em
score zero. Contagens e somas gerenciais de registros existentes não constituem
cálculo de risco. Os registros operacionais de usuários e tratativas permanecem
no escopo existente; essa diretriz não autoriza novas alterações nos dados de
origem dos clientes.

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
