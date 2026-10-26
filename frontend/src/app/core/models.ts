export interface Cliente {
  idCliente: string;
  tipoPessoa: 'PF' | 'PJ' | string;
  nomeRazaoSocial: string;
  documentoFicticio: string;
  dataNascimentoAbertura: string;
  cidade: string;
  uf: string;
  dataCadastro: string;
  statusCliente: string;
  segmento: string;
  rendaFaturamentoMensal: number;
  profissaoAtividade: string;
  origemCadastro: string;
}

export interface Conta {
  idConta: string;
  cliente: Cliente;
  tipoConta: string;
  dataAbertura: string;
  statusConta: string;
  limiteTransacionalDiario: number;
  saldoMedio30d: number;
}

export interface Transacao {
  idTransacao: string;
  dataHoraTransacao: string;
  tipoTransacao: string;
  valor: number;
  moeda: string;
  canal: string;
  clienteOrigem: Cliente;
  contaOrigem: Conta;
  clienteDestino?: Cliente | null;
  contaDestino?: Conta | null;
  nomeContraparte: string;
  documentoContraparteFicticio: string;
  bancoContraparte: string;
  ufIp: string;
  idDispositivo: string;
  reputacaoDispositivo: string;
  horarioAtipico: boolean;
  novoFavorecido: boolean;
  foraPerfil: boolean;
  scoreTransacao: number;
  statusTransacao: string;
}

export interface Kyc {
  idKyc: string;
  cliente: Cliente;
  dataAnalise: string;
  documentoValidado: boolean;
  biometriaValidada: boolean;
  enderecoValidado: boolean;
  telefoneValidado: boolean;
  emailValidado: boolean;
  pep: boolean;
  listaRestritiva: boolean;
  midiaAdversa: boolean;
  beneficiarioFinalIdentificado: boolean;
  scoreKyc: number;
  nivelRiscoKyc: string;
  statusKyc: string;
  motivoDecisao?: string | null;
}

export interface RegraRisco {
  idRegra: string;
  categoria: string;
  nomeRegra: string;
  descricao: string;
  nivelSeveridade: string;
  pesoScore: number;
  statusRegra: string;
}

export interface AlertaPld {
  idAlertaPld: string;
  clienteAnalisado: Cliente;
  transacaoReferencia?: Transacao | null;
  dataAlerta: string;
  regra: RegraRisco;
  tipoAlerta: string;
  contraparteRelacionada?: string | null;
  valorRelacionado: number;
  scorePld: number;
  severidade: string;
  statusAnalise: string;
  resultado: string;
  necessitaEscalonamento: boolean;
  analistaResponsavel: string;
  dataEncerramento?: string | null;
  observacao?: string | null;
}

export interface AlertaFraude {
  idAlertaFraude: string;
  transacao: Transacao;
  dataAlerta: string;
  clienteOrigem: Cliente;
  clienteDestino?: Cliente | null;
  clienteVitima?: Cliente | null;
  clienteSuspeito?: Cliente | null;
  nomeSuspeitoExterno?: string | null;
  regra: RegraRisco;
  tipoFraude: string;
  scoreFraude: number;
  severidade: string;
  statusAnalise: string;
  resultado: string;
  valorExposto: number;
  valorPerda: number;
  analistaResponsavel: string;
  dataEncerramento?: string | null;
  observacao?: string | null;
}

export interface Chargeback {
  idChargeback: string;
  cliente: Cliente;
  transacao: Transacao;
  modalidade: string;
  dataTransacao: string;
  dataContestacao: string;
  valorTransacao: number;
  valorContestado: number;
  motivoChargeback: string;
  canalOrigem: string;
  contraparte: string;
  statusChargeback: string;
  fraudeDeclarada: boolean;
  resultadoInvestigacao: string;
  valorRecuperado: number;
  perdaFinanceira: number;
  dataResolucao?: string | null;
}

export interface RiscoCliente {
  idCliente: string;
  scoreKyc: number;
  scoreFraude: number;
  scorePld: number;
  scoreChargeback: number;
  scoreRiscoGeral: number;
  nivelRisco: string;
  qtdAlertasFraude: number;
  qtdAlertasPld: number;
  qtdChargebacks: number;
  qtdFraudesConfirmadas: number;
  valorPerdas: number;
  dataUltimaAvaliacao: string;
}

export interface ClienteHistorico {
  cliente: Cliente;
  contas: Conta[];
  transacoes: Transacao[];
  kycs: Kyc[];
  alertasFraude: AlertaFraude[];
  alertasPld: AlertaPld[];
  chargebacks: Chargeback[];
  riscoCliente?: RiscoCliente | null;
}

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginResponse {
  id: number;
  nome: string;
  email: string;
  perfil: string;
}

export type CategoriaOcorrencia = 'PLD' | 'Fraude' | 'KYC' | 'Chargeback';

export interface OcorrenciaView {
  id: string;
  clienteId: string;
  clienteNome: string;
  categoria: CategoriaOcorrencia;
  tipo: string;
  valor: number;
  risco: string;
  data: string;
  status: string;
  responsavel?: string;
  resultado?: string;
}

export interface Tratativa {id:number;categoria:'PLD'|'FRAUDE'|'CHARGEBACK';origemId:string;status:'PENDENTE'|'EM_ANALISE'|'AGUARDANDO_PARECER'|'CONCLUIDA';responsavelId:number|null;responsavelNome:string|null;produto:string|null;clienteId:string|null;dataOcorrencia:string|null;valor:number|null;risco:string|null;decisao:string|null;justificativa:string|null;criadoEm:string;atualizadoEm:string;concluidoEm:string|null;}
export interface TratativaEvento {id:number;ocorrencia_id:number;executor_id:number;executor_nome:string;status_anterior:string|null;status_novo:string;acao:string;decisao:string|null;justificativa:string|null;registrado_em:string;}

export interface IndicadoresOcorrencias{total:number;concluidas:number;abertas:number;mesAtual:number;porCategoria:{categoria:string;total:number}[];porStatus:{status:string;total:number}[];porCategoriaStatus:{categoria:string;status:string;total:number}[];evolucao:{periodo:string;total:number}[];}
