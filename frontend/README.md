# Frontend Angular — Sistema de Gerenciamento de Riscos

Interface web do SGR, executada separadamente do backend Spring Boot.

## Arquitetura

```text
Angular (localhost:4200)
    ↓ HTTP/JSON com sessão e CSRF
Spring Boot (localhost:8080/api)
    ↓ JPA/JDBC
PostgreSQL
```

O frontend não possui dados fictícios de fallback. Se a API estiver indisponível,
as telas informam a falha sem inventar clientes, scores ou níveis de risco.

Clientes, scores e classificações de risco são recebidos da fonte externa por
meio da API. O Angular apenas consulta e apresenta esses valores.

## Funcionalidades

- autenticação por sessão;
- dashboard e indicadores filtráveis;
- consulta e detalhe consolidado de clientes;
- consultas de KYC, PLD, fraude e chargeback;
- fila, contexto e histórico de ocorrências;
- paginação padronizada em 10, 50 ou 100 registros nas listas operacionais;
- tratativas operacionais autorizadas por perfil;
- relatórios com exportação PDF e Excel;
- gestão de usuários para administradores.

## Pré-requisitos

- Node.js instalado;
- dependências instaladas com `npm install` ou `npm ci`;
- backend disponível em `http://localhost:8080`;
- PostgreSQL e backend iniciados antes do Angular.

## Executar localmente

No diretório `frontend`:

```powershell
npm install
npm start
```

Abra `http://localhost:4200/login`.

## Validar

```powershell
npm run test:unit
npm run build
```

O teste unitário utiliza regras reais dos componentes e utilitários. O build de
produção também valida TypeScript e templates Angular.

## Diretórios gerados

As pastas abaixo são locais, regeneráveis e não devem ser incluídas no Git ou no
pacote de entrega:

- `node_modules/` — dependências instaladas;
- `.angular/` — cache persistente do compilador, desabilitado neste projeto;
- `.unit-test/` — saída temporária dos testes;
- `dist/` — bundle gerado.

Elas não são funcionalidades do SGR e não armazenam dados de clientes. Esses
diretórios também ficam ocultos no explorador do VS Code para manter visíveis
somente os arquivos úteis durante a apresentação.
