import {Component,OnInit} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute,Router} from '@angular/router';
import {ApiService} from '../../core/api.service';
import {AuthService} from '../../core/auth.service';
import {Tratativa,TratativaEvento,ContextoTratativa} from '../../core/models';
import {periodoValido} from '../../core/filtros-risco';

@Component({selector:'app-ocorrencias',standalone:true,imports:[FormsModule,DatePipe],templateUrl:'./ocorrencias.component.html'})
export class OcorrenciasComponent implements OnInit {
 todas:Tratativa[]=[];selecionada:Tratativa|null=null;historico:TratativaEvento[]=[];
 contexto:ContextoTratativa|null=null;
 visao:'FILA'|'HISTORICO'='FILA';carregando=true;executando=false;erro='';aviso='';
 categoria='';status='';inicio='';fim='';busca='';justificativa='';decisao='PROCEDENTE';
 constructor(private api:ApiService,public auth:AuthService,private route:ActivatedRoute,private router:Router){}
 ngOnInit():void{
  const origemId=this.route.snapshot.queryParamMap.get('origemId');
  const categoria=this.route.snapshot.queryParamMap.get('categoria');
  if(origemId&&categoria){
   this.api.tratativaPorOrigem(categoria.toUpperCase(),origemId).subscribe({
    next:o=>{this.visao=o.status==='CONCLUIDA'?'HISTORICO':'FILA';this.categoria=o.categoria;this.carregar();this.abrir(o);},
    error:e=>{this.erro=e?.error?.detail||'Ocorrência de origem não encontrada. Confira a sincronização.';this.carregar();}
   });
  }else this.carregar();
 }
 get analista(){return this.auth.usuario()?.perfil==='ANALISTA';}
 get filtradas(){const q=this.busca.trim().toLowerCase();return this.todas.filter(o=>!q||o.origemId.toLowerCase().includes(q)||String(o.id).includes(q)||String(o.clienteId||'').toLowerCase().includes(q));}
 limpar():void{this.categoria='';this.status='';this.inicio='';this.fim='';this.busca='';this.carregar();}
 carregar():void{
  if(!periodoValido(this.inicio,this.fim)){this.erro='Data inicial não pode ser posterior à data final.';return;}
  this.carregando=true;this.erro='';this.api.tratativas({visao:this.visao,categoria:this.categoria,status:this.status,inicio:this.inicio,fim:this.fim}).subscribe({
   next:d=>{this.todas=d;this.carregando=false;if(this.selecionada){this.selecionada=d.find(x=>x.id===this.selecionada?.id)||this.selecionada;}},
   error:e=>{this.carregando=false;this.erro=e?.error?.detail||e?.error?.erro||'Não foi possível carregar a fila. Confira o PostgreSQL e a migração 08.';}
  });
 }
 abrir(o:Tratativa):void{this.selecionada=o;this.justificativa='';this.aviso='';this.contexto=null;this.carregarHistorico(o.id);
  this.api.contextoTratativa(o.id).subscribe({next:d=>this.contexto=d,error:()=>this.contexto=null});
 }
 carregarHistorico(id:number):void{this.api.historicoTratativa(id).subscribe({next:d=>this.historico=d,error:()=>this.historico=[]});}
 fechar():void{this.selecionada=null;this.historico=[];this.contexto=null;}
 podeAssumir(o:Tratativa){return this.analista&&(o.status==='PENDENTE'||o.status==='AGUARDANDO_PARECER');}
 responsavel(o:Tratativa){return this.analista&&o.responsavelId===this.auth.usuario()?.id&&o.status==='EM_ANALISE';}
 abrirKyc(o:Tratativa):void{this.router.navigate(['/ocorrencias/kyc'],{queryParams:{origemId:o.origemId}});}
 executar(acao:'assumir'|'parecer'|'concluir'):void{
  if(!this.selecionada||this.executando)return;
  const id=this.selecionada.id;
  if(acao!=='assumir'&&!this.justificativa.trim()){this.erro='Informe uma justificativa para registrar a tratativa.';return;}
  if(acao==='concluir'&&this.selecionada.categoria==='KYC'){
   this.erro='Para KYC, registre a decisão ACEITO ou NÃO ACEITO no módulo cadastral.';return;
  }
  this.executando=true;this.erro='';
  const req=acao==='assumir'?this.api.assumirTratativa(id):acao==='parecer'?this.api.solicitarParecer(id,this.justificativa):this.api.concluirTratativa(id,this.decisao,this.justificativa);
  req.subscribe({next:resultado=>{this.executando=false;this.selecionada=resultado;this.aviso='Operação registrada no histórico.';this.justificativa='';this.carregarHistorico(id);this.carregar();},
   error:e=>{this.executando=false;this.erro=e?.error?.detail||e?.error?.erro||e?.error?.message||'Operação recusada. Confira o responsável e o estado atual.';this.carregar();}});
 }
}
