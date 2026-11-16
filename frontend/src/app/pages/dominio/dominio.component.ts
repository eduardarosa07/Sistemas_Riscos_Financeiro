import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {CurrencyPipe, DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {map, Observable, of, Subscription} from 'rxjs';
import {ApiService} from '../../core/api.service';
import {AuthService} from '../../core/auth.service';
import {OcorrenciaView, TratativaEvento} from '../../core/models';
import {classeRisco, kycEncerrado, labelStatus, ocorrenciasDe} from '../../core/risco.utils';

@Component({selector:'app-dominio', standalone:true, imports:[FormsModule, CurrencyPipe, DatePipe], templateUrl:'./dominio.component.html'})
export class DominioComponent implements OnInit, OnDestroy {
  titulo=''; categoria=''; dados:OcorrenciaView[]=[]; busca=''; status='TODOS'; risco='TODOS';
  aplicados={busca:'', status:'TODOS', risco:'TODOS'};
  carregando=true; erro=''; mensagem='';
  visaoKyc:'PENDENTES'|'HISTORICO'='PENDENTES';
  parecer=''; decisaoKyc:'ACEITO'|'NAO_ACEITO'='ACEITO'; salvando=false;
  historicoKyc:TratativaEvento[]=[]; erroHistoricoKyc=''; carregandoHistoricoKyc=false;
  private selecionado='';
  private dadosRequest?:Subscription;
  private historicoRequest?:Subscription;
  private rotaRequest?:Subscription;
  private versaoDados=0;
  private versaoHistorico=0;
  private destruido=false;
  get kycSelecionado(){return this.selecionado;}
  set kycSelecionado(id:string){this.selecionarKyc(id);}
  get podeDecidirKyc(){return this.categoria==='KYC' && this.auth.usuario()?.perfil==='ANALISTA' && this.visaoKyc==='PENDENTES';}
  labelStatus=labelStatus; classeRisco=classeRisco;

  constructor(private route:ActivatedRoute, private router:Router, private api:ApiService, public auth:AuthService){}

  ngOnInit():void {
    this.rotaRequest=this.route.data.subscribe(data=>{
      const dominios:Record<string,[string,string]>={pld:['PLD','PLD'], chargeback:['Chargeback','Chargeback'], kyc:['KYC','KYC'], fraude:['Fraude','Fraude']};
      [this.titulo,this.categoria]=dominios[data['dominio'] as string]||['Ocorrências',''];
      this.kycSelecionado=this.route.snapshot.queryParamMap.get('origemId')||'';
      this.carregar();
    });
  }
  ngOnDestroy():void {
    this.destruido=true;
    this.versaoDados++; this.versaoHistorico++;
    this.dadosRequest?.unsubscribe(); this.historicoRequest?.unsubscribe(); this.rotaRequest?.unsubscribe();
  }
  carregar():void {
    if(this.salvando || this.destruido)return;
    const versao=++this.versaoDados;
    this.dadosRequest?.unsubscribe();
    this.carregando=true; this.erro=''; this.dados=[];
    let consulta:Observable<OcorrenciaView[]>;
    switch(this.categoria){
      case 'PLD': consulta=this.api.plds().pipe(map(d=>ocorrenciasDe(d,[],[],[]))); break;
      case 'Fraude': consulta=this.api.fraudes().pipe(map(d=>ocorrenciasDe([],d,[],[]))); break;
      case 'Chargeback': consulta=this.api.chargebacks().pipe(map(d=>ocorrenciasDe([],[],[],d))); break;
      case 'KYC': consulta=this.api.filaKyc(this.visaoKyc).pipe(map(d=>ocorrenciasDe([],[],d,[]))); break;
      default: consulta=of([]);
    }
    this.dadosRequest=consulta.subscribe({
      next:todos=>{
        if(versao!==this.versaoDados)return;
        this.dados=this.categoria==='KYC'
          ? todos.filter(o=>this.visaoKyc==='HISTORICO' ? kycEncerrado(o.status) : !kycEncerrado(o.status))
          : todos;
        if(this.kycSelecionado && !this.dados.some(x=>x.id===this.kycSelecionado))this.kycSelecionado='';
        this.carregando=false;
      },
      error:()=>{if(versao===this.versaoDados){this.carregando=false;this.erro='Não foi possível carregar os registros. Tente novamente.';}}
    });
  }
  filtrar():void {this.aplicados={busca:this.busca,status:this.status,risco:this.risco};}
  get filtrados():OcorrenciaView[] {
    const q=this.aplicados.busca.toLowerCase().trim();
    return this.dados.filter(o=>(!q||o.id.toLowerCase().includes(q)||o.clienteNome.toLowerCase().includes(q)) &&
      (this.aplicados.status==='TODOS'||o.status.toUpperCase().replace(/_/g,' ').includes(this.aplicados.status)) &&
      (this.aplicados.risco==='TODOS'||(o.risco||'').toUpperCase().includes(this.aplicados.risco)));
  }
  limpar():void {this.busca='';this.status='TODOS';this.risco='TODOS';this.filtrar();}
  abrirTratativa(o:OcorrenciaView):void {
    this.router.navigate(['/ocorrencias'],{queryParams:{categoria:this.categoria.toUpperCase(),origemId:o.id}});
  }
  selecionarKyc(id:string):void {
    if(this.salvando || id===this.selecionado)return;
    const versao=++this.versaoHistorico;
    this.historicoRequest?.unsubscribe();
    this.selecionado=id; this.parecer=''; this.decisaoKyc='ACEITO'; this.mensagem='';
    this.historicoKyc=[]; this.erroHistoricoKyc=''; this.carregandoHistoricoKyc=false;
    if(!id || this.visaoKyc!=='HISTORICO')return;
    this.carregandoHistoricoKyc=true;
    this.historicoRequest=this.api.historicoKyc(id).subscribe({
      next:d=>{if(versao===this.versaoHistorico){this.historicoKyc=d;this.carregandoHistoricoKyc=false;}},
      error:()=>{if(versao===this.versaoHistorico){this.carregandoHistoricoKyc=false;this.erroHistoricoKyc='Não foi possível consultar o histórico desta análise.';}}
    });
  }
  decidirKyc():void {
    const id=this.kycSelecionado;
    if(!this.podeDecidirKyc || !id || !this.parecer.trim() || this.salvando || this.carregando)return;
    if(!this.dados.some(o=>o.id===id && !kycEncerrado(o.status)))return;
    this.salvando=true; this.erro='';
    this.api.decidirKyc(id,this.decisaoKyc,this.parecer.trim()).subscribe({
      next:()=>{
        this.salvando=false; this.kycSelecionado='';
        this.mensagem='Decisão KYC registrada. O caso foi encerrado e preservado no histórico.';
        this.carregar();
      },
      error:e=>{this.salvando=false;this.erro=e?.error?.detail||e?.error?.erro||'Não foi possível registrar a decisão KYC.';}
    });
  }
}
