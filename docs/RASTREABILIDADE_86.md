# Rastreabilidade — 86 requisitos do SGR

**Fonte:** catálogo de requisitos da Sprint 7, confrontado com o código do ZIP `Sistemas_Riscos_Financeiros(2).zip`.

- Total: **86 IDs únicos** (RF=45, BD=12, BE=12, RN=7, RNF=10).
- Histórico original: **46 ATENDIDO, 35 PARCIALMENTE ATENDIDO, 5 NÃO ATENDIDO**. Essa classificação é documental e **não corresponde a 100% homologado**.
- A base de código alterada inclui correções em KYC, tratativas, cliente, KPIs, relatórios e filtros, mas ainda requer execução com banco real.
- O CSV contém um registro por requisito, sua descrição, arquivo de código, resultado parcial dos testes e o aceite obrigatório. A revisão de escopo abaixo registra a atualização autorizada do RF16.
- **Homologação integral: nenhum dos 86 requisitos recebeu comprovação end-to-end neste ambiente.** O campo permanece NÃO COMPROVADA até a execução em PostgreSQL com dados consistentes.
- Os requisitos vinculados/redundantes não foram excluídos. A rastreabilidade preserva os 86 IDs originais.

## Revisão de escopo autorizada em 15/11/2026

- O RF16 foi ajustado por orientação do responsável pelo projeto: foi retirada a exigência de usar uma regra de cálculo de risco documentada no SGR.
- Clientes, scores e níveis de risco são fornecidos externamente. O SGR apenas consulta e exibe esses dados, sem criá-los ou recalculá-los no frontend, no backend ou no banco.
- O aceite do RF16 passa a comparar a exibição com os dados de origem e a verificar a distinção entre score zero e dado não informado.
- RF39 continua exigindo a exibição do risco recebido na listagem. Contagens e somas gerenciais de registros existentes não são novos cálculos de risco.
- Os 86 IDs e os estados de homologação foram preservados. A atualização documental não comprova que o código já atende à diretriz; classificações locais, endpoints de escrita e alterações dos dados de origem continuam sujeitos à auditoria.

## Escopo dos testes entregues

Validação atualizada em 15/11/2026: consultar [Correções prioritárias](CORRECOES_PRIORITARIAS_2026-11-15.md). Os registros abaixo descrevem a entrega anterior e permanecem como histórico, sem substituir os resultados atuais.

- **35 verificações unitárias independentes executadas com sucesso** (20 regras Java, 15 regras frontend).
- **44 métodos JUnit preparados**, não executados neste ambiente (dependências Maven não instaláveis sem acesso a repositórios).
- **Angular TypeScript + templates verificados por ngc**, sem erros; bundle de produção não compilado aqui devido a dependência nativa esbuild originalmente instalada para Windows.
- **Testes de integração HTTP fornecidos**, mas não executados por indisponibilidade do PostgreSQL e da API em execução.

Para dar aceite a um requisito, registre: ID, caso de teste, massa de dados, resposta obtida, consulta SQL correspondente, data e responsável pela homologação. Não substitua essa evidência por testes unitários simples ou presença de código.
