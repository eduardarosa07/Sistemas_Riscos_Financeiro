import {Component,OnInit} from '@angular/core';
import {ActivatedRoute,Router} from '@angular/router';
import {CurrencyPipe,DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../../core/api.service';
import {AuthService} from '../../core/auth.service';
import {OcorrenciaView,Kyc,TratativaEvento} from '../../core/models';
import {classeRisco,labelStatus,ocorrenciasDe} from '../../core/risco.utils';
@Component({selector:'app-dominio',standalone:true,imports:[FormsModule,CurrencyPipe,DatePipe],templateUrl:'./dominio.component.html'})
export class DominioComponent implements OnInit {
 titulo='';categoria='';dados:OcorrenciaView[]=[];busca='';status='TODOS';risco='TODOS';
 aplicados={busca:'',status:'TODOS',risco:'TODOS'};
 carregando=true;erro='';mensagem='';
 visaoKyc:'PENDENTES'|'HISTORICO'='PENDENTES';
 parecer='';decisaoKyc:'ACEITO'|'NAO_ACEITO'='ACEITO';kycSelecionado='';salvando=false;
 historicoKyc:TratativaEvento[]=[];kycs:Kyc[]=[];
 get podeDecidirKyc(){return this.categoria==='KYC'&&this.auth.usuario()?.perfil==='ANALISTA'&&this.visaoKyc==='PENDENTES';}
 labelStatus=labelStatus;classeRisco=classeRisco;
 constructor(private route:ActivatedRoute,private router:Router,private api:ApiService,public auth:AuthService){}
 ngOnInit():void{
  this.route.data.subscribe(data=>{
   const dominio=data['dominio'] as string;
   const map:Record<string,[string,string]>={pld:['PLD','PLD'],chargeback:['Chargeback','Chargeback'],kyc:['KYC','KYC'],fraude:['Fraude','Fraude']};
   [this.titulo,this.categoria]=map[dominio]||['Ocorrências',''];
   this.kycSelecionado=this.route.snapshot.queryParamMap.get('origemId')||'';
   this.carregar();
  });
 }
 carregar():void{
  this.carregando=true;this.erro='';
  this.api.dadosRisco().subscribe({next:d=>{
   const todos=ocorrenciasDe(d.plds,d.fraudes,d.kycs,d.chargebacks).filter(o=>o.categoria===this.categoria);
   if(this.categoria==='KYC'){
    const concluida=(s:string)=>['ACEITO','NAO_ACEITO'].includes((s||'').toUpperCase());
    this.dados=todos.filter(o=>this.visaoKyc==='HISTORICO'?concluida(o.status):!concluida(o.status));
    this.kycs=d.kycs;
    if(this.kycSelecionado && !this.dados.some(x=>x.id===this.kycSelecionado))this.kycSelecionado='';
   }else this.dados=todos;
   this.carregando=false;
  },error:()=>{this.carregando=false;this.erro='Não foi possível carregar dados reais do PostgreSQL.';}});
 }
 filtrar():void{this.aplicados={busca:this.busca,status:this.status,risco:this.risco};}
 get filtrados():OcorrenciaView[]{const q=this.aplicados.busca.toLowerCase().trim();return this.dados.filter(o=>(!q||o.id.toLowerCase().includes(q)||o.clienteNome.toLowerCase().includes(q))&&
 (this.aplicados.status==='TODOS'||o.status.toUpperCase().replace(/_/g,' ').includes(this.aplicados.status))&&
 (this.aplicados.risco==='TODOS'||(o.risco||'').toUpperCase().includes(this.aplicados.risco)));}
 limpar():void{this.busca='';this.status='TODOS';this.risco='TODOS';this.filtrar();}
 abrirTratativa(o:OcorrenciaView):void{
  const cat=this.categoria.toUpperCase();
  this.router.navigate(['/ocorrencias'],{queryParams:{categoria:cat,origemId:o.id}});
 }
 selecionarKyc(id:string):void{
  this.kycSelecionado=id;this.historicoKyc=[];
  if(id)this.api.historicoKyc(id).subscribe({next:d=>this.historicoKyc=d,error:()=>this.historicoKyc=[]});
 }
 decidirKyc():void{
  if(!this.kycSelecionado||!this.parecer.trim()||this.salvando)return;
  this.salvando=true;this.erro='';
  this.api.decidirKyc(this.kycSelecionado,this.decisaoKyc,this.parecer).subscribe({
   next:()=>{this.salvando=false;this.mensagem='Decisão KYC registrada. O caso foi encerrado e preservado no histórico.';this.kycSelecionado='';this.parecer='';this.carregar();},
   error:e=>{this.salvando=false;this.erro=e?.error?.detail||e?.error?.erro||'Não foi possível registrar a decisão KYC.';}
  });
 }
}
