# Rastreabilidade dos 86 requisitos

## Fonte oficial

O catalogo recebido no projeto possui 86 IDs unicos:

- 45 requisitos funcionais (`RF`);
- 12 requisitos de banco (`BD`);
- 12 requisitos de backend (`BE`);
- 7 regras de negocio (`RN`);
- 10 requisitos nao funcionais (`RNF`).

A fonte versionada e `MATRIZ_86_REQUISITOS.csv`. As classificacoes 46 atendidos,
35 parcialmente atendidos e 5 nao atendidos sao o status historico da planilha
de origem. Elas nao equivalem a homologacao da versao atual.

## Diretriz de dados externos

Clientes, contas, transacoes, KYC, PLD, fraude, chargeback, scores e niveis de
risco sao recebidos de uma fonte externa. O SGR consulta e apresenta esses dados,
sem cadastrar clientes ou calcular e reclassificar risco.

O aceite relacionado a risco deve comparar o valor exibido com a origem e
confirmar que ausencia de informacao nao foi transformada em zero ou em uma
classificacao inventada. Agregacoes de quantidade e valor para indicadores nao
constituem novo calculo de risco.

## Niveis de evidencia

Cada requisito pode ter evidencias diferentes:

1. `Codigo`: implementacao identificada por inspecao.
2. `Teste unitario`: regra isolada executada sem banco real.
3. `Integracao`: backend, frontend ou PostgreSQL executado em conjunto.
4. `Homologacao`: fluxo aceito manualmente com massa, perfil e resultado
   registrados.

Um requisito nao deve ser promovido a homologado apenas porque existe codigo ou
um teste unitario. O resultado consolidado das execucoes esta em
`VALIDACAO_ATUAL.md`.

## Evidencias atuais gerais

- backend: 122 testes, 116 aprovados e 6 ignorados;
- frontend: 37 testes aprovados;
- typecheck e build Angular aprovados;
- Spring Boot iniciado com PostgreSQL e schema validado;
- consultas de diagnostico PostgreSQL executadas somente por leitura;
- raiz, status, CORS, CSRF, login invalido e protecao sem sessao verificados;
- login valido e navegacao completa por perfil ainda dependem de aceite manual.

Essas evidencias cobrem componentes compartilhados por varios requisitos, mas
nao substituem o registro de aceite individual dos 86 IDs.

## Registro de aceite

Para concluir um requisito, registre:

- ID e texto do requisito;
- perfil utilizado;
- massa de dados sem informacao pessoal;
- passos executados;
- resultado esperado e obtido;
- teste, endpoint ou consulta SQL correspondente;
- data e responsavel pelo aceite;
- classificacao final: aprovado, parcial, reprovado ou nao executado.

Operacoes de escrita, como decisao KYC e conclusao de ocorrencia, devem ser
homologadas somente em uma copia descartavel do banco.
