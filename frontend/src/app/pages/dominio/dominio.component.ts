import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { map, Observable, of, Subscription } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { OcorrenciaView, TratativaEvento } from '../../core/models';
import { paginar } from '../../core/paginacao';
import {
  classeRisco,
  kycEncerrado,
  labelStatus,
  ocorrenciasDe
} from '../../core/risco.utils';
import { PaginacaoComponent } from '../../shared/paginacao.component';

type VisaoKyc = 'PENDENTES' | 'HISTORICO';
type DecisaoKyc = 'ACEITO' | 'NAO_ACEITO';

@Component({
  selector: 'app-dominio',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe, PaginacaoComponent],
  templateUrl: './dominio.component.html'
})
export class DominioComponent implements OnInit, OnDestroy {
  titulo = '';
  categoria = '';
  dados: OcorrenciaView[] = [];
  busca = '';
  status = 'TODOS';
  risco = 'TODOS';
  aplicados = { busca: '', status: 'TODOS', risco: 'TODOS' };
  pagina = 1;
  itensPorPagina = 10;

  carregando = true;
  erro = '';
  mensagem = '';

  visaoKyc: VisaoKyc = 'PENDENTES';
  parecer = '';
  decisaoKyc: DecisaoKyc = 'ACEITO';
  salvando = false;

  historicoKyc: TratativaEvento[] = [];
  erroHistoricoKyc = '';
  carregandoHistoricoKyc = false;

  readonly labelStatus = labelStatus;
  readonly classeRisco = classeRisco;

  private selecionado = '';
  private dadosRequest?: Subscription;
  private historicoRequest?: Subscription;
  private rotaRequest?: Subscription;
  private versaoDados = 0;
  private versaoHistorico = 0;
  private destruido = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly api: ApiService,
    public readonly auth: AuthService
  ) {}

  get kycSelecionado(): string {
    return this.selecionado;
  }

  set kycSelecionado(id: string) {
    this.selecionarKyc(id);
  }

  get podeDecidirKyc(): boolean {
    return (
      this.categoria === 'KYC' &&
      this.auth.usuario()?.perfil === 'ANALISTA' &&
      this.visaoKyc === 'PENDENTES'
    );
  }

  ngOnInit(): void {
    this.rotaRequest = this.route.data.subscribe((data) => {
      const dominios: Record<string, [string, string]> = {
        pld: ['PLD', 'PLD'],
        chargeback: ['Chargeback', 'Chargeback'],
        kyc: ['KYC', 'KYC'],
        fraude: ['Fraude', 'Fraude']
      };

      [this.titulo, this.categoria] = dominios[data['dominio'] as string] ?? [
        'Ocorrências',
        ''
      ];
      this.kycSelecionado = this.route.snapshot.queryParamMap.get('origemId') ?? '';
      this.carregar();
    });
  }

  ngOnDestroy(): void {
    this.destruido = true;
    this.versaoDados++;
    this.versaoHistorico++;
    this.dadosRequest?.unsubscribe();
    this.historicoRequest?.unsubscribe();
    this.rotaRequest?.unsubscribe();
  }

  carregar(): void {
    if (this.salvando || this.destruido) return;

    this.pagina = 1;
    const versao = ++this.versaoDados;
    this.dadosRequest?.unsubscribe();
    this.carregando = true;
    this.erro = '';
    this.dados = [];

    this.dadosRequest = this.consultaDoDominio().subscribe({
      next: (todos) => {
        if (versao !== this.versaoDados) return;

        this.dados =
          this.categoria === 'KYC'
            ? todos.filter((ocorrencia) =>
                this.visaoKyc === 'HISTORICO'
                  ? kycEncerrado(ocorrencia.status)
                  : !kycEncerrado(ocorrencia.status)
              )
            : todos;

        if (
          this.kycSelecionado &&
          !this.dados.some((ocorrencia) => ocorrencia.id === this.kycSelecionado)
        ) {
          this.kycSelecionado = '';
        }
        this.carregando = false;
      },
      error: () => {
        if (versao !== this.versaoDados) return;
        this.carregando = false;
        this.erro = 'Não foi possível carregar os registros. Tente novamente.';
      }
    });
  }

  filtrar(): void {
    this.pagina = 1;
    this.aplicados = {
      busca: this.busca,
      status: this.status,
      risco: this.risco
    };
  }

  get filtrados(): OcorrenciaView[] {
    const busca = this.aplicados.busca.toLowerCase().trim();

    return this.dados.filter(
      (ocorrencia) =>
        (!busca ||
          ocorrencia.id.toLowerCase().includes(busca) ||
          ocorrencia.clienteNome.toLowerCase().includes(busca)) &&
        (this.aplicados.status === 'TODOS' ||
          ocorrencia.status
            .toUpperCase()
            .replace(/_/g, ' ')
            .includes(this.aplicados.status)) &&
        (this.aplicados.risco === 'TODOS' ||
          (ocorrencia.risco ?? '').toUpperCase().includes(this.aplicados.risco))
    );
  }

  get paginados(): OcorrenciaView[] {
    return paginar(this.filtrados, this.pagina, this.itensPorPagina);
  }

  limpar(): void {
    this.busca = '';
    this.status = 'TODOS';
    this.risco = 'TODOS';
    this.filtrar();
  }

  abrirTratativa(ocorrencia: OcorrenciaView): void {
    this.router.navigate(['/ocorrencias'], {
      queryParams: {
        categoria: this.categoria.toUpperCase(),
        origemId: ocorrencia.id
      }
    });
  }

  selecionarKyc(id: string): void {
    if (this.salvando || id === this.selecionado) return;

    const versao = ++this.versaoHistorico;
    this.historicoRequest?.unsubscribe();
    this.selecionado = id;
    this.parecer = '';
    this.decisaoKyc = 'ACEITO';
    this.mensagem = '';
    this.historicoKyc = [];
    this.erroHistoricoKyc = '';
    this.carregandoHistoricoKyc = false;

    if (!id || this.visaoKyc !== 'HISTORICO') return;

    this.carregandoHistoricoKyc = true;
    this.historicoRequest = this.api.historicoKyc(id).subscribe({
      next: (historico) => {
        if (versao !== this.versaoHistorico) return;
        this.historicoKyc = historico;
        this.carregandoHistoricoKyc = false;
      },
      error: () => {
        if (versao !== this.versaoHistorico) return;
        this.carregandoHistoricoKyc = false;
        this.erroHistoricoKyc =
          'Não foi possível consultar o histórico desta análise.';
      }
    });
  }

  decidirKyc(): void {
    const id = this.kycSelecionado;
    const parecer = this.parecer.trim();

    if (
      !this.podeDecidirKyc ||
      !id ||
      !parecer ||
      this.salvando ||
      this.carregando
    ) {
      return;
    }

    if (
      !this.dados.some(
        (ocorrencia) => ocorrencia.id === id && !kycEncerrado(ocorrencia.status)
      )
    ) {
      return;
    }

    this.salvando = true;
    this.erro = '';
    this.api.decidirKyc(id, this.decisaoKyc, parecer).subscribe({
      next: () => {
        this.salvando = false;
        this.kycSelecionado = '';
        this.mensagem =
          'Decisão KYC registrada. O caso foi encerrado e preservado no histórico.';
        this.carregar();
      },
      error: (erro) => {
        this.salvando = false;
        this.erro =
          erro?.error?.detail ??
          erro?.error?.erro ??
          'Não foi possível registrar a decisão KYC.';
      }
    });
  }

  private consultaDoDominio(): Observable<OcorrenciaView[]> {
    switch (this.categoria) {
      case 'PLD':
        return this.api.plds().pipe(map((dados) => ocorrenciasDe(dados, [], [], [])));
      case 'Fraude':
        return this.api.fraudes().pipe(map((dados) => ocorrenciasDe([], dados, [], [])));
      case 'Chargeback':
        return this.api
          .chargebacks()
          .pipe(map((dados) => ocorrenciasDe([], [], [], dados)));
      case 'KYC':
        return this.api
          .filaKyc(this.visaoKyc)
          .pipe(map((dados) => ocorrenciasDe([], [], dados, [])));
      default:
        return of([]);
    }
  }
}
