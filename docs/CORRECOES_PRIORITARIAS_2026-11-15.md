# Correções prioritárias — 15/11/2026

## Escopo

Correções autorizadas após as auditorias do backend e do frontend. A premissa é
consumir clientes, scores e níveis de risco fornecidos externamente. Não foram
criados cálculos de risco, clientes ou dados financeiros. Não houve conexão ao
PostgreSQL, execução de migrações, commit ou push.

O layout e o CSS da aplicação foram preservados. Os caminhos das APIs e os campos
existentes foram mantidos. A ausência de score agora permanece ausente/nula na
visão de clientes, em vez de ser substituída por zero; o modelo Angular aceita
essa ausência.

## Alterações

| Área | Resultado |
|---|---|
| Autorização | O interceptor usa o padrão de rota reconhecido pelo Spring MVC, em vez da URI bruta. Sem mapeamento, nega acesso. Escritas genéricas permanecem bloqueadas, inclusive por caminhos codificados e com parâmetros de matriz. |
| Consulta por origem | GET de tratativa não executa mais sincronização nem escrita. A sincronização permanece no POST existente e no agendamento configurável. |
| Dados de risco | Chargeback não recebe mais classificação ALTO por fraude declarada. Se a origem não fornece risco, a tela informa a ausência. O score geral ausente não recebe valor zero. |
| KYC | Domínio, fila, sincronização e script 08 reconhecem ACEITO, NAO_ACEITO, APROVADO e REPROVADO, com normalização de espaços/capitalização. |
| Sincronização | Removida a classe duplicada em src/test que substituía a implementação de produção. O método agendado possui transação e respeita sgr.sincronizacao.automatica, com padrão true para preservar o agendamento existente. Datas de análise não são usadas para inventar datas de conclusão histórica. |
| Seleção KYC | Trocar a análise limpa parecer e decisão. Durante envio, a seleção fica bloqueada. O envio valida que a análise pertence ao conjunto pendente exibido. |
| Ocorrências e histórico | Consultas anteriores são canceladas e respostas são vinculadas à seleção correspondente. Respostas de gravações antigas não reabrem outro painel. Erro de consulta é distinto de histórico vazio. |
| Filtros e mensagens | Atualizações de fila, dashboard e relatórios descartam respostas antigas. Indicadores anteriores não são apresentados após falha da nova consulta. Recusas de tratativa permanecem visíveis após atualizar a fila. |
| Consultas de domínio | Cada módulo consulta apenas sua própria categoria, em vez de carregar seis conjuntos completos. |
| Excel | Exportação deixa de expandir o conteúdo da planilha como argumentos de uma chamada. Testado com 5.000 registros, conteúdo final e CRCs das entradas ZIP. |
| PDF | Campos e colunas são quebrados em linhas e páginas, sem corte em 28/155 caracteres. Acentos portugueses são preservados. Caracteres fora de WinAnsi são representados por escapes Unicode legíveis. |

Os arquivos SQL 06 e 08 tiveram os mapeamentos de importação alinhados, mas **não
foram executados**. Essas alterações não corrigem retroativamente ocorrências já
persistidas, pois o ON CONFLICT DO NOTHING continua preservando registros e
decisões existentes. Nenhuma estrutura de tabela foi alterada nesta etapa.

## Validação executada

| Verificação | Resultado |
|---|---|
| Maven offline, compilação limpa e suítes selecionadas sem banco | 113 testes aprovados, zero falhas/erros |
| Testes de sincronização sobre a implementação de produção | 7 aprovados; nenhuma classe homônima em target/test-classes após clean |
| Testes frontend: filtros, permissões, seleção, concorrência e exportações | 33 aprovados |
| Compilador Angular, tipos e templates, sem emissão | Aprovado |
| Build Angular de produção com cache desativado e saída temporária | Aprovado; pacote inicial de 464,52 kB, dentro do orçamento |
| Matriz de requisitos | 86 IDs únicos preservados; sem promoção automática de homologação |
| git diff --check | Sem erros |

Comandos principais:

```powershell
# Diretório backend: exclui explicitamente o teste de contexto com PostgreSQL.
.\mvnw.cmd -o -q clean "-Dtest=sr.config.*Test,sr.controller.*Test,sr.domain.*Test,sr.service.*Test" test

# Diretório frontend.
npm run test:unit
node node_modules/@angular/compiler-cli/bundles/src/bin/ngc.js -p tsconfig.app.json --noEmit
# Build executado por subprocesso com CI=true e NG_CLI_ANALYTICS=false;
# --output-path apontou para uma pasta temporária fora do repositório.
node node_modules/@angular/cli/bin/ng.js build --configuration production --output-path <pasta-temporaria>/dist --no-progress
```

Os testes de componentes Node exercitam lógica TypeScript com RxJS real e
dependências Angular simuladas. Os testes de autorização exercitam MockMvc e
interceptores reais, com serviços/repositories simulados. Nenhum deles substitui
homologação em navegador com o servidor e o PostgreSQL reais.

## Pendências da auditoria

Atualização: a validação posterior com PostgreSQL local e navegador está em
[Validação do frontend e PostgreSQL](VALIDACAO_FRONT_POSTGRES_2026-11-15.md).
Ela confirma a existência do histórico nesta instância e registra as pendências
que continuam abertas. As limitações abaixo descrevem o pacote inicial.

- Conferir o schema real de ocorrencia_historico, seus relacionamentos e o trigger
  de imutabilidade. A divergência com ocorrencia_tratativa_evento nos scripts não
  foi resolvida por uma alteração estrutural sem essa conferência.
- Revisar ocorrências legadas já importadas como pendentes e datas históricas
  anteriormente preenchidas. Não há atualização retroativa automática.
- Homologar os fluxos em navegador e numa base isolada, inclusive atomicidade,
  bloqueios concorrentes e acesso através do servidor/proxy implantado.
- Definir se a inativação cadastral feita pelo fluxo KYC existente é compatível
  com a política de dados de clientes somente consultivos. Essa regra preexistente
  foi preservada nesta etapa, sem executar decisões reais.
- Permanecem fora deste pacote: concorrência administrativa e de revogação de
  sessões, capacidade do cache de login, atualização de dependências, padronização
  global de erros/validações e a divergência de política dos indicadores USUARIO.
- A consulta de tratativas ainda limita o resultado a 5.000 registros sem
  paginação/totalização. Exportações preservam o conjunto recebido; não comprovam
  completude de uma base maior que esse limite.

O resultado é um pacote de correções verificadas isoladamente, sem declaração
de homologação integral do sistema ou dos 86 requisitos.
