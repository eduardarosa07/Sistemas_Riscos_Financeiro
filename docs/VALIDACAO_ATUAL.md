# Validacao atual do SGR

Data da consolidacao: 22/11/2026.

Este documento substitui os relatorios e logs parciais de etapas anteriores. Os
resultados abaixo distinguem verificacao automatizada, integracao local e o que
ainda depende de homologacao manual.

## Resultado consolidado

| Verificacao | Resultado atual |
| --- | --- |
| Backend Maven | 122 testes: 116 aprovados, 6 ignorados, nenhuma falha |
| Frontend unitario | 37 testes aprovados |
| Angular typecheck | Aprovado |
| Angular build de producao | Aprovado; pacote inicial de 469,85 kB |
| Inicializacao Spring Boot | Aprovada com PostgreSQL e `ddl-auto=validate` |
| Inicializacao Angular | Aprovada em `localhost:4200` |
| Raiz da API | HTTP 200, identificando o backend REST |
| Status da API | HTTP 200 |
| CORS local | Origem `http://localhost:4200` autorizada |
| Login invalido | HTTP 401 com campo JSON `erro` |
| Endpoint protegido sem sessao | HTTP 401 |
| PostgreSQL | Consultas executadas em transacao somente leitura |

Os seis testes ignorados pertencem a verificacoes opt-in que podem iniciar
contexto ou depender de PostgreSQL preparado. Eles nao foram forçados durante a
suite comum.

## PostgreSQL verificado

A instancia local `sistema_riscos`, PostgreSQL 18.3, foi consultada sem executar
INSERT, UPDATE, DELETE ou migracao.

- 14 tabelas publicas;
- 120 clientes;
- 120 analises KYC;
- 120 registros de risco;
- 510 ocorrencias;
- 3 eventos de historico;
- nenhum historico orfao identificado;
- nenhum cliente sem risco identificado;
- nenhum risco sem cliente identificado;
- nenhum e-mail normalizado duplicado identificado.

Foram observadas 439 ocorrencias legadas concluidas sem decisao registrada. O
sistema nao deve inventar essas decisoes nem preencher retroativamente o acervo
sem uma regra formal e uma base isolada.

## Integracao HTTP verificada

Com backend, frontend e PostgreSQL locais:

- a raiz da API deixou de responder 500 e passou a identificar o servico;
- a pagina Angular de login respondeu com sucesso;
- o token CSRF foi obtido;
- credencial invalida retornou 401 e o campo `erro` consumido pelo Angular;
- chamadas protegidas sem sessao foram recusadas;
- a sincronizacao automatica permaneceu desabilitada.

O login valido e a navegacao completa com uma conta real ainda devem ser
confirmados manualmente pelo responsavel, sem compartilhar senha. O roteiro esta
em `GUIA_APRESENTACAO.md`.

## Limites da comprovacao

Os resultados nao significam homologacao automatica dos 86 requisitos. Ainda e
necessario registrar evidencia funcional por requisito quando o aceite academico
exigir demonstracao de tela, perfil ou persistencia.

Nao foram executados na base real:

- conclusao de ocorrencia;
- decisao KYC;
- cadastro ou alteracao de usuario;
- sincronizacao manual ou automatica;
- scripts de criacao ou migracao;
- alteracao de cliente, score ou nivel de risco.

## Pendencias conhecidas

1. O schema SQL versionado nao representa integralmente o banco local: o script
   06 cria `ocorrencia_tratativa_evento`, enquanto o codigo e a instancia atual
   utilizam `ocorrencia_historico`.
2. O script 07 cria clientes e KYC sinteticos e nao pertence ao fluxo da
   apresentacao nem a uma base real.
3. As ocorrencias legadas sem decisao exigem avaliacao separada de
   rastreabilidade.
4. Login valido e navegacao por perfil precisam de aceite manual final.
5. Testes de escrita devem usar exclusivamente uma copia descartavel do banco.

## Repeticao segura

Validar o codigo:

```powershell
.\scripts\validar_codigo.ps1
```

Diagnosticar o PostgreSQL somente por leitura:

```powershell
psql -X -w -h localhost -U postgres -d sistema_riscos `
  -v ON_ERROR_STOP=1 -f scripts/validar_postgresql_leitura.sql
```

Iniciar a demonstracao:

```powershell
.\scripts\iniciar_apresentacao.ps1
```
