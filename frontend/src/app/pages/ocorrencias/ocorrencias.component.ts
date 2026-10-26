import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Tratativa, TratativaEvento } from '../../core/models';
@Component({selector:'app-ocorrencias',standalone:true,imports:[FormsModule,DatePipe],templateUrl:'./ocorrencias.component.html'})
export class OcorrenciasComponent implements OnInit {
 todas:Tratativa[]=[]; selecionada:Tratativa|null=null; historico:TratativaEvento[]=[];
 carregando=true;executando=false;erro='';aviso='';categoria='';status='';inicio='';fim='';busca='';
 justificativa='';decisao='PROCEDENTE';
 constructor(private api:ApiService,public auth:AuthService){}
 ngOnInit():void{this.carregar();}
 get analista(){return this.auth.usuario()?.perfil==='ANALISTA';}
 get filtradas(){let q=this.busca.trim().toLowerCase();return this.todas.filter(o=>!q||o.origemId.toLowerCase().includes(q)||String(o.id).includes(q));}
 limpar():void{this.categoria='';this.status='';this.inicio='';this.fim='';this.busca='';this.carregar();}
 carregar():void {if(this.inicio&&this.fim&&this.inicio>this.fim){this.erro='Data inicial não pode ser posterior à data final.';return;}this.carregando=true;this.erro='';this.api.tratativas({categoria:this.categoria,status:this.status,inicio:this.inicio,fim:this.fim}).subscribe({next:d=>{this.todas=d;this.carregando=false;if(this.selecionada)this.selecionada=d.find(x=>x.id===this.selecionada?.id)||null;},error:e=>{this.carregando=false;this.erro=e?.error?.erro||e?.error?.detail||'Não foi possível carregar a fila. Confira a migração SQL 06 e o backend.';}});}
 abrir(o:Tratativa):void{this.selecionada=o;this.justificativa='';this.aviso='';this.carregarHistorico(o.id);}
 carregarHistorico(id:number):void{this.api.historicoTratativa(id).subscribe({next:d=>this.historico=d,error:()=>this.historico=[]});}
 fechar():void{this.selecionada=null;this.historico=[];}
 podeAssumir(o:Tratativa){return this.analista&&(o.status==='PENDENTE'||o.status==='AGUARDANDO_PARECER');}
 responsavel(o:Tratativa){return this.analista&&o.responsavelId===this.auth.usuario()?.id&&o.status==='EM_ANALISE';}
 executar(acao:'assumir'|'parecer'|'concluir'):void{
  if(!this.selecionada||this.executando)return;
  const id=this.selecionada.id;
  if(acao!=='assumir'&&!this.justificativa.trim()){this.erro='Informe uma justificativa para registrar a tratativa.';return;}
  this.executando=true;this.erro='';
  const req=acao==='assumir'?this.api.assumirTratativa(id):acao==='parecer'?this.api.solicitarParecer(id,this.justificativa):this.api.concluirTratativa(id,this.decisao,this.justificativa);
  req.subscribe({next:resultado=>{this.executando=false;this.selecionada=resultado;this.aviso='Operação registrada no histórico.';this.justificativa='';this.carregarHistorico(id);this.carregar();},error:e=>{this.executando=false;this.erro=e?.error?.erro||e?.error?.detail||e?.error?.message||'Operação recusada. Atualize a fila e confira o responsável.';this.carregar();}});
 }
}
