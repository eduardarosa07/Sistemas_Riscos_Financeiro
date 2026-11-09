import {Component,OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../../core/api.service';
import {RiskChartComponent} from './risk-chart.component';
import {IndicadoresOcorrencias} from '../../core/models';
import {periodoValido,selecoesCsv} from '../../core/filtros-risco';
@Component({selector:'app-home',standalone:true,imports:[FormsModule,RiskChartComponent],templateUrl:'./home.component.html'})
export class HomeComponent implements OnInit {
 carregando=true;erro='';dados:IndicadoresOcorrencias|null=null;
 inicio='';fim='';statusSelecionados:string[]=[];categoriasSelecionadas:string[]=[];
 produtosSelecionados:string[]=[];produtos:string[]=[];
 readonly categorias=['KYC','PLD','FRAUDE','CHARGEBACK'];
 readonly estados=['PENDENTE','EM_ANALISE','AGUARDANDO_PARECER','CONCLUIDA'];
 constructor(private api:ApiService){}
 ngOnInit():void{this.api.produtos().subscribe({next:lista=>this.produtos=lista,error:()=>this.produtos=[]});this.carregar();}
 carregar():void{if(!periodoValido(this.inicio,this.fim)){this.erro='Data inicial deve ser anterior ou igual à final.';return;}
  this.carregando=true;this.erro='';this.api.indicadoresOcorrencias({inicio:this.inicio,fim:this.fim,status:selecoesCsv(this.statusSelecionados),categoria:selecoesCsv(this.categoriasSelecionadas),produto:selecoesCsv(this.produtosSelecionados)}).subscribe({
   next:d=>{this.dados=d;this.carregando=false;},
   error:e=>{this.erro=e?.error?.detail||e?.error?.erro||'Não foi possível calcular os indicadores. Confira o banco e a migração 08.';this.carregando=false;}
  });}
 limpar():void{this.inicio='';this.fim='';this.statusSelecionados=[];this.categoriasSelecionadas=[];this.produtosSelecionados=[];this.carregar();}
 get categoriaItens(){return (this.dados?.porCategoria||[]).map(c=>({rotulo:c.categoria,valor:Number(c.total)}));}
 get statusItens(){return (this.dados?.porStatus||[]).map(c=>({rotulo:c.status.replaceAll('_',' '),valor:Number(c.total)}));}
 get evolucaoItens(){return (this.dados?.evolucao||[]).map(c=>({rotulo:c.periodo,valor:Number(c.total)}));}
 get abertasPorCategoria(){return this.categorias.map(c=>({rotulo:c,valor:(this.dados?.porCategoriaStatus||[]).filter(r=>r.categoria===c&&r.status!=='CONCLUIDA').reduce((a,r)=>a+Number(r.total),0)}));}
 get concluidasPorCategoria(){return this.categorias.map(c=>({rotulo:c,valor:Number(this.dados?.porCategoriaStatus?.find(r=>r.categoria===c&&r.status==='CONCLUIDA')?.total||0)}));}
 get conclusaoPercentual(){return this.dados?.total?Math.round(this.dados.concluidas/this.dados.total*100):0;}
}
