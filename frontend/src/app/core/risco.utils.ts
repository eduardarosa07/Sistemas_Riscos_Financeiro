import {
  AlertaFraude, AlertaPld, Chargeback, Kyc, OcorrenciaView
} from './models';

export function statusNormalizado(v?: string | null): string {
  return (v || '').trim().toUpperCase().replace(/_/g, ' ');
}

export function estaAberta(status: string): boolean {
  const s = statusNormalizado(status);
  return ['PENDENTE', 'EM ANALISE', 'AGUARDANDO PARECER', 'EM ABERTO', 'ABERTO'].includes(s);
}

export function estaConcluida(status: string): boolean {
  const s = statusNormalizado(status);
  return ['CONCLUIDA', 'FECHADO', 'ENCERRADO', 'APROVADO', 'REPROVADO', 'CONCLUIDO', 'RESOLVIDO', 'FINALIZADO'].includes(s);
}

export function ocorrenciasDe(
  plds: AlertaPld[], fraudes: AlertaFraude[], kycs: Kyc[], chargebacks: Chargeback[]
): OcorrenciaView[] {
  const a: OcorrenciaView[] = [
    ...plds.map(x => ({
      id: x.idAlertaPld,
      clienteId: x.clienteAnalisado?.idCliente || '',
      clienteNome: x.clienteAnalisado?.nomeRazaoSocial || '—',
      categoria: 'PLD' as const,
      tipo: x.tipoAlerta,
      valor: Number(x.valorRelacionado || 0),
      risco: x.severidade,
      data: x.dataAlerta,
      status: x.statusAnalise,
      responsavel: x.analistaResponsavel,
      resultado: x.resultado
    })),
    ...fraudes.map(x => ({
      id: x.idAlertaFraude,
      clienteId: x.clienteOrigem?.idCliente || '',
      clienteNome: x.clienteOrigem?.nomeRazaoSocial || '—',
      categoria: 'Fraude' as const,
      tipo: x.tipoFraude,
      valor: Number(x.valorExposto || 0),
      risco: x.severidade,
      data: x.dataAlerta,
      status: x.statusAnalise,
      responsavel: x.analistaResponsavel,
      resultado: x.resultado
    })),
    ...kycs.map(x => ({
      id: x.idKyc,
      clienteId: x.cliente?.idCliente || '',
      clienteNome: x.cliente?.nomeRazaoSocial || '—',
      categoria: 'KYC' as const,
      tipo: 'Análise cadastral',
      valor: 0,
      risco: x.nivelRiscoKyc,
      data: x.dataAnalise,
      status: x.statusKyc,
      resultado: x.motivoDecisao || ''
    })),
    ...chargebacks.map(x => ({
      id: x.idChargeback,
      clienteId: x.cliente?.idCliente || '',
      clienteNome: x.cliente?.nomeRazaoSocial || '—',
      categoria: 'Chargeback' as const,
      tipo: x.motivoChargeback || x.modalidade,
      valor: Number(x.valorContestado || 0),
      risco: x.fraudeDeclarada ? 'ALTO' : 'NÃO CLASSIFICADO',
      data: x.dataContestacao,
      status: x.statusChargeback,
      resultado: x.resultadoInvestigacao
    }))
  ];
  return a.sort((x, y) => new Date(y.data).getTime() - new Date(x.data).getTime());
}

export function labelStatus(v: string): string {
  const s = statusNormalizado(v);
  if (s === 'EM ANALISE') return 'Em análise';
  if (s === 'PENDENTE') return 'Pendente';
  if (s === 'FECHADO') return 'Fechado';
  if (s === 'ENCERRADO') return 'Encerrado';
  if (s === 'APROVADO') return 'Aprovado';
  if (s === 'REPROVADO') return 'Reprovado';
  return v ? v.replace(/_/g, ' ') : '—';
}

export function classeRisco(v: string): string {
  const s = statusNormalizado(v);
  if (s.includes('CRIT')) return 'critico';
  if (s.includes('ALT')) return 'alto';
  if (s.includes('MED')) return 'medio';
  if (s.includes('BAIX')) return 'baixo';
  return 'neutro';
}
