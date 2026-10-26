import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { OcorrenciaView } from '../../core/models';
import { classeRisco, labelStatus, ocorrenciasDe } from '../../core/risco.utils';

@Component({
  selector: 'app-dominio',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './dominio.component.html'
})
export class DominioComponent implements OnInit {
  titulo = ''; categoria = '';
  dados: OcorrenciaView[] = [];
  busca = ''; status = 'TODOS'; risco = 'TODOS';
  carregando = true; erro = '';
  parecer='';decisaoKyc:'ACEITO'|'NAO_ACEITO'='ACEITO';kycSelecionado='';salvando=false;mensagem='';
  get podeDecidirKyc(){return this.categoria==='KYC'&&this.auth.usuario()?.perfil==='ANALISTA';}
  decidirKyc():void{if(!this.kycSelecionado||!this.parecer.trim())return;this.salvando=true;this.erro='';this.api.decidirKyc(this.kycSelecionado,this.decisaoKyc,this.parecer).subscribe({next:()=>{this.salvando=false;this.mensagem='Decisão KYC registrada na análise.';this.kycSelecionado='';this.parecer='';this.carregar();},error:e=>{this.salvando=false;this.erro=e?.error?.erro||e?.error?.detail||'Não foi possível decidir o KYC.';}});}
  labelStatus = labelStatus; classeRisco = classeRisco;

  constructor(private route: ActivatedRoute, private api: ApiService, public auth:AuthService) {}

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      const dominio = data['dominio'] as string;
      const map: Record<string, [string,string]> = {
        pld: ['PLD','PLD'], chargeback: ['Chargeback','Chargeback'], kyc: ['KYC','KYC'], fraude: ['Fraude','Fraude']
      };
      [this.titulo, this.categoria] = map[dominio] || ['Ocorrências',''];
      this.carregar();
    });
  }

  carregar(): void {
    this.carregando = true; this.erro='';
    this.api.dadosRisco().subscribe({
      next: d => {
        this.dados = ocorrenciasDe(d.plds,d.fraudes,d.kycs,d.chargebacks).filter(o => o.categoria === this.categoria);
        this.carregando=false;
      },
      error: () => { this.carregando=false; this.erro='Não foi possível carregar os dados do PostgreSQL.'; }
    });
  }

  get filtrados(): OcorrenciaView[] {
    const q=this.busca.toLowerCase().trim();
    return this.dados.filter(o =>
      (!q || o.id.toLowerCase().includes(q) || o.clienteNome.toLowerCase().includes(q)) &&
      (this.status==='TODOS' || o.status.toUpperCase().replace(/_/g,' ').includes(this.status)) &&
      (this.risco==='TODOS' || (o.risco||'').toUpperCase().includes(this.risco))
    );
  }
}
