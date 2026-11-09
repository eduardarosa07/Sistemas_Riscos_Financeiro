# Relatório de validação — revisão do código do SGR (08/11/2026)

## Arquivo-base e escopo

**Entrada:** `Sistemas_Riscos_Financeiros(2).zip` — SHA256 `ac134c1c21cbf03af98854904fa94e7a87cde4bb57cb1e625f83c433bbd00dec`.

A revisão usa o projeto do ZIP fornecido nesta etapa, com os módulos Spring Boot + PostgreSQL e Angular existentes. Mantém o layout CSS e componentes originais; foram acrescentadas opções de filtro, dados na listagem e ações contextuais.

## Resultado realmente executado

| Procedimento | Resultado | Evidência |
|---|---|---|
| Compilação das regras puras Java (`javac`, Java 21) | **APROVADO** | 20 cenários executados, todos passaram |
| Testes de frontend (`npm run test:unit`) | **APROVADO** | 16 cenários Node/TypeScript executados, todos passaram |
| Verificação do Angular/TypeScript + templates (`ngc --noEmit`) | **APROVADO** | Código e templates compilados sem erros |
| JUnit 5 / Mockito via Maven (`.\mvnw.cmd test`) | **NÃO EXECUTADO** | Maven não pôde ser baixado: ambiente sem acesso DNS ao repositório de dependências |
| `ng build` (bundle Angular) | **NÃO COMPROVADO** | Dependências `node_modules` vieram do Windows (esbuild win32) e não rodam nativamente neste Linux; recomenda-se `npm ci` no Windows |
| Testes com PostgreSQL conectado | **NÃO EXECUTADO** | Não havia servidor/dump operacional disponível para testar dados reais |
| Testes HTTP autenticados | **NÃO EXECUTADO** | Exigem API e PostgreSQL ativos e contas de ensaio |
| Homologação visual por perfil e fluxos completos | **NÃO EXECUTADO** | Exige navegação real nas telas, com base de teste |

## Testes adicionados

- Regras de domínio KYC (decisões válidas, impossibilidade de reabrir, restrição por analista, recusa com inativação).
- Regras de tratativas (assumir, não assumir caso alheio, justificativa obrigatória, estados e decisão sem evidências).
- JUnit para indicadores e filtros, fila/histórico, autorização por perfil, CSRF, KYC e sincronização incremental (44 métodos disponíveis, **não executados neste ambiente**).
- Testes HTTP não destrutivos (ver `scripts/teste_integracao_http.py`), opcionais com credenciais separadas.

## Alterações de maior impacto

1. Migração SQL 08 e sincronização incremental das quatro categorias (KYC, PLD, FRAUDE, CHARGEBACK), preservando registros existentes.
2. Decisão KYC transacional, autor responsável, parecer e inativação de cliente recusado.
3. Fila operacional separada de histórico concluído e painel com contexto do cliente, mantendo a estrutura visual existente.
4. Risco geral na listagem de clientes, ao lado da situação do cadastro.
5. KPIs e gráficos com agregações SQL, filtros combináveis, período validado e destaque para mês/dia.
6. Relatórios sincronizados com a consulta de todas as ocorrências, inclusive concluídas.
7. Testes Java/JUnit/Node e arquivos de rastreabilidade dos 86 requisitos.

## Bloqueios reais para declarar “86 de 86 homologados”

Apenas a **presença de código e de testes** não garante que todos os requisitos estejam funcionando: os 86 dependem de rodar com a estrutura e dados corretos do PostgreSQL, de confirmação das migrações, de compilar o Maven completo, de executar cada JUnit, testar os endpoints e homologar as telas. Não seria correto converter as verificações unitárias executadas em aprovação integral dos 86 requisitos.

O catálogo de 86 itens é preservado em `docs/MATRIZ_86_REQUISITOS.csv`. **Nenhum requisito foi declarado integralmente homologado** nesta revisão por falta dessas evidências. Os testes adicionais foram incluídos para permitir essa validação no seu ambiente Windows.

## Próxima execução local

Siga `docs/GUIA_EXECUCAO_WINDOWS.md`. Execute `scripts/validar_local.ps1` após configurar Java, Node.js e PostgreSQL; em seguida, rode os testes HTTP e documente resultado de cada requisito.
