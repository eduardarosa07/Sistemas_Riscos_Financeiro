# Rastreabilidade — 86 requisitos do SGR

**Fonte:** catálogo de requisitos da Sprint 7, confrontado com o código do ZIP `Sistemas_Riscos_Financeiros(2).zip`.

- Total: **86 IDs únicos** (RF=45, BD=12, BE=12, RN=7, RNF=10).
- Histórico original: **46 ATENDIDO, 35 PARCIALMENTE ATENDIDO, 5 NÃO ATENDIDO**. Essa classificação é documental e **não corresponde a 100% homologado**.
- A base de código alterada inclui correções em KYC, tratativas, cliente, KPIs, relatórios e filtros, mas ainda requer execução com banco real.
- O CSV contém um registro por requisito, sua descrição original, arquivo de código, resultado parcial dos testes e o aceite obrigatório.
- **Homologação integral: nenhum dos 86 requisitos recebeu comprovação end-to-end neste ambiente.** O campo permanece NÃO COMPROVADA até a execução em PostgreSQL com dados consistentes.
- Os requisitos vinculados/redundantes não foram excluídos. A rastreabilidade preserva os 86 IDs originais.

## Escopo dos testes entregues

- **35 verificações unitárias independentes executadas com sucesso** (20 regras Java, 15 regras frontend).
- **44 métodos JUnit preparados**, não executados neste ambiente (dependências Maven não instaláveis sem acesso a repositórios).
- **Angular TypeScript + templates verificados por ngc**, sem erros; bundle de produção não compilado aqui devido a dependência nativa esbuild originalmente instalada para Windows.
- **Testes de integração HTTP fornecidos**, mas não executados por indisponibilidade do PostgreSQL e da API em execução.

Para dar aceite a um requisito, registre: ID, caso de teste, massa de dados, resposta obtida, consulta SQL correspondente, data e responsável pela homologação. Não substitua essa evidência por testes unitários simples ou presença de código.
