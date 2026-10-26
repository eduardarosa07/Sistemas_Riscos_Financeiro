# Frontend Angular — Sistemas Riscos Financeiros

Frontend web separado do Spring Boot e alinhado às telas de referência do projeto.

## Arquitetura

`Angular -> HTTP/JSON -> Spring Boot (localhost:8080) -> JPA/Hibernate -> PostgreSQL`

O frontend **não possui dados fictícios de fallback**. Se o backend estiver indisponível, a tela exibe erro e não inventa registros.

## Telas incluídas

- Login
- Home
- Clientes
- Visão consolidada do cliente
- Ocorrências
- PLD
- Chargeback
- KYC
- Fraude
- Relatórios
- Usuários

## Executar

1. Inicie PostgreSQL.
2. Inicie o backend Spring Boot na porta `8080`.
3. Nesta pasta:
   - `npm install`
   - `npm start`
4. Abra `http://localhost:4200`.

O backend já está configurado para aceitar `http://localhost:4200` por CORS.

## Observação

A Home e os Relatórios agregam dados usando os endpoints existentes enquanto o backend ainda não possui endpoints próprios de dashboard/relatórios. Todos os números continuam vindo do PostgreSQL por meio da API.

A tela futura de tratativa de ocorrências ainda não grava decisões, pois a persistência/histórico da tratativa ainda precisa ser modelada no backend.
