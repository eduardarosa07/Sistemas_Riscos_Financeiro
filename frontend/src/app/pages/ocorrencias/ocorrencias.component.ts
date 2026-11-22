import {Component, OnDestroy, OnInit} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {ApiService} from '../../core/api.service';
import {AuthService} from '../../core/auth.service';
import {Tratativa, TratativaEvento, ContextoTratativa} from '../../core/models';
import {periodoValido} from '../../core/filtros-risco';
import {paginar} from '../../core/paginacao';
import {PaginacaoComponent} from '../../shared/paginacao.component';

@Component({selector:'app-ocorrencias',standalone:true,imports:[FormsModule,DatePipe,PaginacaoComponent],templateUrl:'./ocorrencias.component.html'})
export class OcorrenciasComponent implements OnInit, OnDestroy {
  todas:Tratativa[]=[]; selecionada:Tratativa|null=null; historico:TratativaEvento[]=[];
  contexto:ContextoTratativa|null=null;
  visao:'FILA'|'HISTORICO'='FILA'; carregando=true; executando=false; erro=''; aviso='';
  erroHistorico=''; erroContexto=''; carregandoHistorico=false;
  categoria=''; status=''; inicio=''; fim=''; busca=''; justificativa=''; decisao='PROCEDENTE';
  pagina=1; itensPorPagina=10;
  private listaRequest?:Subscription;
  private historicoRequest?:Subscription;
  private contextoRequest?:Subscription;
  private origemRequest?:Subscription;
  private versaoLista=0;
  private versaoSelecao=0;
  private destruido=false;

  constructor(private api:ApiService,public auth:AuthService,private route:ActivatedRoute,private router:Router){}
  ngOnInit():void {
    const origemId=this.route.snapshot.queryParamMap.get('origemId');
    const categoria=this.route.snapshot.queryParamMap.get('categoria');
    if(origemId&&categoria){
      this.origemRequest=this.api.tratativaPorOrigem(categoria.toUpperCase(),origemId).subscribe({
        next:o=>{this.visao=o.status==='CONCLUIDA'?'HISTORICO':'FILA';this.categoria=o.categoria;this.carregar();this.abrir(o);},
        error:e=>{this.erro=e?.error?.erro||e?.error?.detail||'Ocorrência de origem não encontrada.';this.carregar(false);}
      });
    }else this.carregar();
  }
  ngOnDestroy():void {
    this.destruido=true;
    this.versaoLista++; this.fechar(); this.listaRequest?.unsubscribe(); this.origemRequest?.unsubscribe();
  }
  get analista(){return this.auth.usuario()?.perfil==='ANALISTA';}
  get filtradas(){const q=this.busca.trim().toLowerCase();return this.todas.filter(o=>!q||o.origemId.toLowerCase().includes(q)||String(o.id).includes(q)||String(o.clienteId||'').toLowerCase().includes(q));}
  get paginadas(){return paginar(this.filtradas,this.pagina,this.itensPorPagina);}
  limpar():void {this.categoria='';this.status='';this.inicio='';this.fim='';this.busca='';this.carregar();}
  carregar(limparErro=true):void {
    if(this.destruido)return;
    if(!periodoValido(this.inicio,this.fim)){this.erro='Data inicial não pode ser posterior à data final.';return;}
    this.pagina=1;
    const versao=++this.versaoLista;
    this.listaRequest?.unsubscribe();
    this.carregando=true; if(limparErro)this.erro=''; this.todas=[];
    this.listaRequest=this.api.tratativas({visao:this.visao,categoria:this.categoria,status:this.status,inicio:this.inicio,fim:this.fim}).subscribe({
      next:d=>{
        if(versao!==this.versaoLista)return;
        this.todas=d;this.carregando=false;
        if(this.selecionada)this.selecionada=d.find(x=>x.id===this.selecionada?.id)||this.selecionada;
      },
      error:e=>{
        if(versao!==this.versaoLista)return;
        this.carregando=false;
        if(!this.erro)this.erro=e?.error?.erro||e?.error?.detail||'Não foi possível carregar a fila. Tente novamente.';
      }
    });
  }
  abrir(o:Tratativa):void {
    this.fechar();
    const versao=this.versaoSelecao;
    this.selecionada=o;this.justificativa='';this.decisao='PROCEDENTE';this.aviso='';
    this.carregarHistorico(o.id);
    this.contextoRequest=this.api.contextoTratativa(o.id).subscribe({
      next:d=>{if(versao===this.versaoSelecao && this.selecionada?.id===o.id)this.contexto=d;},
      error:()=>{if(versao===this.versaoSelecao)this.erroContexto='Não foi possível consultar o contexto desta ocorrência.';}
    });
  }
  carregarHistorico(id:number):void {
    const versao=this.versaoSelecao;
    this.historicoRequest?.unsubscribe();
    this.historico=[];this.erroHistorico='';this.carregandoHistorico=true;
    this.historicoRequest=this.api.historicoTratativa(id).subscribe({
      next:d=>{if(versao===this.versaoSelecao && this.selecionada?.id===id){this.historico=d;this.carregandoHistorico=false;}},
      error:()=>{if(versao===this.versaoSelecao){this.carregandoHistorico=false;this.erroHistorico='Não foi possível consultar o histórico desta ocorrência.';}}
    });
  }
  fechar():void {
    this.versaoSelecao++; this.historicoRequest?.unsubscribe(); this.contextoRequest?.unsubscribe();
    this.selecionada=null;this.historico=[];this.contexto=null;this.erroHistorico='';this.erroContexto='';this.carregandoHistorico=false;
  }
  podeAssumir(o:Tratativa){
    return this.analista && ((o.status==='PENDENTE' && o.responsavelId==null) ||
      (o.status==='AGUARDANDO_PARECER' && o.responsavelId===this.auth.usuario()?.id));
  }
  responsavel(o:Tratativa){return this.analista&&o.responsavelId===this.auth.usuario()?.id&&o.status==='EM_ANALISE';}
  abrirKyc(o:Tratativa):void {this.router.navigate(['/ocorrencias/kyc'],{queryParams:{origemId:o.origemId}});}
  executar(acao:'assumir'|'parecer'|'concluir'):void {
    if(!this.selecionada||this.executando)return;
    const id=this.selecionada.id, versao=this.versaoSelecao;
    if(acao==='assumir' ? !this.podeAssumir(this.selecionada) : !this.responsavel(this.selecionada))return;
    if(acao!=='assumir'&&!this.justificativa.trim()){this.erro='Informe uma justificativa para registrar a tratativa.';return;}
    if(acao==='concluir'&&this.selecionada.categoria==='KYC'){this.erro='Registre a decisão no módulo KYC.';return;}
    this.executando=true;this.erro='';
    const req=acao==='assumir'?this.api.assumirTratativa(id):acao==='parecer'?this.api.solicitarParecer(id,this.justificativa):this.api.concluirTratativa(id,this.decisao,this.justificativa);
    req.subscribe({
      next:resultado=>{
        this.executando=false;
        if(versao===this.versaoSelecao && this.selecionada?.id===id){
          this.selecionada=resultado;this.aviso='Operação registrada no histórico.';this.justificativa='';this.carregarHistorico(id);
        }
        this.carregar(false);
      },
      error:e=>{
        this.executando=false;
        this.erro=`Ocorrência #${id}: `+(e?.error?.erro||e?.error?.detail||e?.error?.message||'Operação recusada. Confira o responsável e o estado atual.');
        this.carregar(false);
      }
    });
  }
}
