# Projeto Risco — pacote corrigido (24/10/2026)

## Resumo da entrega

- Dashboard: cinco visualizações dinâmicas (categoria, status, abertas, concluídas, evolução mensal), vinculadas aos filtros e à API do PostgreSQL. Implementação leve em Angular/SVG/CSS, sem dependências externas de gráficos.
- Clientes: botão **Limpar filtros** compacto e centralizado; situação ATIVO verde e INATIVO vermelho.
- Sidebar: removidos ícones decorativos dos links; preservadas as opções e a seta do submenu.
- Fila de tratativas: botão **Limpar filtros**, validação de intervalo de datas, endpoints Java para listagem, assumir, solicitar parecer, concluir e histórico.
- Backend: rota para decisão KYC, persistindo decisão e parecer na análise.
- SQL 06: cria tabelas de tratativas/auditoria e importa as ocorrências existentes das tabelas PLD/Fraude/Chargeback sem duplicação por origem. Não apaga as ocorrências de origem.
- SQL 07: carga **opcional**, separada, com 60 novos clientes e 60 análises KYC sintéticas. Não é executada automaticamente.

## Antes de executar (OBRIGATÓRIO)

1. Faça **backup completo do PostgreSQL** (`pg_dump -Fc -f backup_sistema_riscos.dump -d sistema_riscos`, configurando usuário e acesso conforme seu ambiente).
2. Verifique se o banco já possui as tabelas JPA usadas pela aplicação (`dim_cliente`, `fato_kyc`, `fato_alerta_pld`, `fato_alerta_fraude`, `fato_chargeback`, `usuario`). Se elas não existirem, os scripts 06/07 não substituem o esquema inicial.
3. Execute `backend/database/06-tratativas.sql` **uma vez** no banco `sistema_riscos`, usando pgAdmin/psql. O script insere apenas ocorrências que ainda não têm correspondência em `ocorrencia_tratativa`.
4. Inspecione as tabelas e faça testes com registros reais. O mapeamento de status na migração 06 marca explicitamente os estados de conclusão conhecidos, deixando os demais como PENDENTE para triagem. Ajuste regras específicas do seu banco, se necessário, antes da execução.
5. Para adicionar os 60 clientes fictícios, execute **somente em ambiente de desenvolvimento/homologação** o `backend/database/07-clientes-kyc-teste.sql`. O SQL é idempotente para os identificadores de teste (`KYCTEST0001` etc.), mas insere registros novos; o script **não** deve ser aplicado em produção.
6. No terminal Windows, no diretório `backend`, execute `./mvnw.cmd test` e `./mvnw.cmd spring-boot:run`. Execute com Java e Maven Wrapper configurados e com PostgreSQL disponível.
7. No diretório `frontend`, execute `npm ci` (reinstala as dependências para seu Windows), `npm run build` e `npm start`.
8. Teste: Dashboard sem filtros; gráficos filtrados por categoria/status/período; fila de tratativas e histórico; decidir KYC; página Clientes; navegador mobile. Faça testes com usuário ANALISTA e GESTOR.

## Importante: limites de validação

A verificação de tipos TypeScript (`tsc -p tsconfig.app.json --noEmit`) passou neste ambiente. A compilação Angular completa ficou bloqueada devido às dependências nativas do Windows incluídas no ZIP original (`esbuild` win32-x64 em sistema Linux); reinstalar com `npm ci` no Windows. O ambiente de análise não tem a instância PostgreSQL do usuário e não permitiu executar os testes integrados Java/PostgreSQL; portanto, essas etapas ficam obrigatoriamente pendentes até testar localmente. O SQL 07 precisa ser validado contra eventuais restrições de domínio customizadas do banco.

## Observações funcionais

- A conclusão de tratativas é definitiva e gera evento de auditoria. Solicitar parecer libera a ocorrência para que um analista a assuma novamente. Ações com status/responsável incompatíveis são recusadas.
- A rota de decisão KYC grava `status_kyc` e `motivo_decisao` da análise. Não altera `status_cliente`: a situação cadastral depende de regras comerciais específicas não fornecidas. A mensagem do frontend deverá refletir isso.
- Os gráficos de abertas e concluídas seguem os filtros, inclusive se o usuário filtrar um status específico.
- A importação inicial de tratativas não altera o status dos registros já existentes no destino; registros de origem continuam íntegros.
