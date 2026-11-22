import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  normalizarPagina,
  normalizarTamanhoPagina,
  TAMANHOS_PAGINA,
  totalPaginas
} from '../core/paginacao';

@Component({
  selector: 'app-paginacao',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="sgr-pagination" aria-label="Paginação da tabela">
      <span class="sgr-pagination-summary">
        Exibindo {{ inicio }}–{{ fim }} de {{ total }}
      </span>

      <div class="sgr-pagination-controls">
        <label>
          Por página
          <select
            aria-label="Registros por página"
            [ngModel]="porPagina"
            (ngModelChange)="alterarTamanho($event)"
          >
            @for (opcao of opcoes; track opcao) {
              <option [ngValue]="opcao">{{ opcao }}</option>
            }
          </select>
        </label>

        <button
          class="btn sgr-page-button"
          type="button"
          aria-label="Primeira página"
          [disabled]="paginaAtual === 1"
          (click)="irPara(1)"
        >
          «
        </button>
        <button
          class="btn sgr-page-button"
          type="button"
          [disabled]="paginaAtual === 1"
          (click)="irPara(paginaAtual - 1)"
        >
          Anterior
        </button>

        <span class="sgr-page-current">
          Página {{ paginaAtual }} de {{ quantidadePaginas }}
        </span>

        <button
          class="btn sgr-page-button"
          type="button"
          [disabled]="paginaAtual === quantidadePaginas"
          (click)="irPara(paginaAtual + 1)"
        >
          Próxima
        </button>
        <button
          class="btn sgr-page-button"
          type="button"
          aria-label="Última página"
          [disabled]="paginaAtual === quantidadePaginas"
          (click)="irPara(quantidadePaginas)"
        >
          »
        </button>
      </div>
    </div>
  `
})
export class PaginacaoComponent {
  @Input() total = 0;
  @Input() pagina = 1;
  @Input() porPagina = 10;
  @Output() paginaChange = new EventEmitter<number>();
  @Output() porPaginaChange = new EventEmitter<number>();

  readonly opcoes = TAMANHOS_PAGINA;

  get quantidadePaginas(): number {
    return totalPaginas(this.total, this.porPagina);
  }

  get paginaAtual(): number {
    return normalizarPagina(this.pagina, this.total, this.porPagina);
  }

  get inicio(): number {
    return this.total === 0 ? 0 : (this.paginaAtual - 1) * this.porPagina + 1;
  }

  get fim(): number {
    return Math.min(this.paginaAtual * this.porPagina, this.total);
  }

  alterarTamanho(valor: number): void {
    const tamanho = normalizarTamanhoPagina(Number(valor));
    this.porPaginaChange.emit(tamanho);
    this.paginaChange.emit(1);
  }

  irPara(pagina: number): void {
    this.paginaChange.emit(
      normalizarPagina(pagina, this.total, this.porPagina)
    );
  }
}
