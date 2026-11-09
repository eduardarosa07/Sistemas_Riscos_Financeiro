/** Regras de filtros independentes do Angular, verificáveis por testes unitários. */
export function periodoValido(inicio?:string,fim?:string):boolean {
  const formato=/^\d{4}-\d{2}-\d{2}$/;
  return (!inicio || formato.test(inicio)) && (!fim || formato.test(fim)) && (!inicio || !fim || inicio<=fim);
}
export function selecoesCsv(valores:string[]):string {
  return [...new Set((valores||[]).map(v=>v.trim()).filter(Boolean))].join(',');
}
export interface FiltroCliente {busca:string;tipo:string;situacao:string;risco:string;}
export interface ClienteFiltravel {idCliente:string;nomeRazaoSocial:string;documentoFicticio:string;tipoPessoa:string;statusCliente:string;riscoGeral:string;}
export function filtrarClientes<T extends ClienteFiltravel>(clientes:T[], filtro:FiltroCliente):T[] {
  const q=filtro.busca.trim().toLowerCase();
  return clientes.filter(c=>(!q || c.idCliente.toLowerCase().includes(q) || c.nomeRazaoSocial.toLowerCase().includes(q) || c.documentoFicticio.toLowerCase().includes(q)) &&
    (filtro.tipo==='TODOS'||c.tipoPessoa===filtro.tipo) &&
    (filtro.situacao==='TODAS'||c.statusCliente.toUpperCase()===filtro.situacao) &&
    (filtro.risco==='TODOS'||c.riscoGeral.toUpperCase()===filtro.risco));
}
