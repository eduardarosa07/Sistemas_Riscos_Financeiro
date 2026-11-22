export const TAMANHOS_PAGINA = [10, 50, 100] as const;

export function normalizarTamanhoPagina(valor: number): number {
  return TAMANHOS_PAGINA.includes(valor as (typeof TAMANHOS_PAGINA)[number])
    ? valor
    : TAMANHOS_PAGINA[0];
}

export function totalPaginas(total: number, porPagina: number): number {
  const tamanho = normalizarTamanhoPagina(porPagina);
  return Math.max(1, Math.ceil(Math.max(0, total) / tamanho));
}

export function normalizarPagina(
  pagina: number,
  total: number,
  porPagina: number
): number {
  const numero = Number.isFinite(pagina) ? Math.trunc(pagina) : 1;
  return Math.min(Math.max(1, numero), totalPaginas(total, porPagina));
}

export function paginar<T>(
  itens: readonly T[],
  pagina: number,
  porPagina: number
): T[] {
  const tamanho = normalizarTamanhoPagina(porPagina);
  const atual = normalizarPagina(pagina, itens.length, tamanho);
  const inicio = (atual - 1) * tamanho;
  return itens.slice(inicio, inicio + tamanho);
}
