import {Component,OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../../core/api.service';
import {RiskChartComponent} from './risk-chart.component';
import {IndicadoresOcorrencias} from '../../core/models';
@Component({selector:'app-home',standalone:true,imports:[FormsModule,RiskChartComponent],templateUrl:'./home.component.html'})
export class HomeComponent implements OnInit {
 carregando=true;erro='';dados:IndicadoresOcorrencias|null=null;
 inicio='';fim='';status='';categoria='';produto='';produtos:string[]=[];
 constructor(private api:ApiService){}
 ngOnInit():void{this.api.produtos().subscribe({next:lista=>this.produtos=lista,error:()=>this.produtos=[]});this.carregar();}
 carregar():void{if(this.inicio&&this.fim&&this.inicio>this.fim){this.erro='Data inicial deve ser anterior ou igual à final.';return;}
  this.carregando=true;this.erro='';this.api.indicadoresOcorrencias({inicio:this.inicio,fim:this.fim,status:this.status,categoria:this.categoria,produto:this.produto}).subscribe({next:d=>{this.dados=d;this.carregando=false;},error:e=>{this.erro=e?.error?.erro||e?.error?.detail||'Não foi possível carregar indicadores. Confira o PostgreSQL e a migração 06.';this.carregando=false;}});}
 limpar():void{this.inicio='';this.fim='';this.status='';this.categoria='';this.produto='';this.carregar();}
 categoriaValor(c:string):number{return Number(this.dados?.porCategoria.find(x=>x.categoria===c)?.total||0);}
 statusValor(s:string):number{return Number(this.dados?.porStatus.find(x=>x.status===s)?.total||0);}
 get categoriaItens(){return (this.dados?.porCategoria||[]).map(c=>({rotulo:c.categoria,valor:Number(c.total)}));}
 get statusItens(){return (this.dados?.porStatus||[]).map(c=>({rotulo:c.status.replaceAll('_',' '),valor:Number(c.total)}));}
 get evolucaoItens(){return (this.dados?.evolucao||[]).map(c=>({rotulo:c.periodo,valor:Number(c.total)}));}
 get abertasPorCategoria(){return ['PLD','FRAUDE','CHARGEBACK'].map(c=>({rotulo:c,valor:(this.dados?.porCategoriaStatus||[]).filter(r=>r.categoria===c&&r.status!=='CONCLUIDA').reduce((a,r)=>a+Number(r.total),0)}));}
 get concluidasPorCategoria(){return ['PLD','FRAUDE','CHARGEBACK'].map(c=>({rotulo:c,valor:Number(this.dados?.porCategoriaStatus?.find(r=>r.categoria===c&&r.status==='CONCLUIDA')?.total||0)}));}
 get conclusaoPercentual(){return this.dados?.total?Math.round(this.dados.concluidas/this.dados.total*100):0;}
}
