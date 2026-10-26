import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { ClienteHistorico } from '../../core/models';

@Component({
  selector: 'app-cliente-detalhe',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './cliente-detalhe.component.html'
})
export class ClienteDetalheComponent implements OnInit {
  dados?: ClienteHistorico;
  carregando = true;
  erro = '';
  aba = 'resumo';

  constructor(private route: ActivatedRoute, private api: ApiService) {}
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.api.clienteHistorico(id).subscribe({
      next: x => { this.dados = x; this.carregando = false; },
      error: () => { this.carregando = false; this.erro = 'Não foi possível carregar a visão consolidada do cliente.'; }
    });
  }
}
