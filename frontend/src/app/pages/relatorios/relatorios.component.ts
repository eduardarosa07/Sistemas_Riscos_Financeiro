import {Component,OnInit} from '@angular/core';
import {CurrencyPipe,DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../../core/api.service';
import {Tratativa} from '../../core/models';
import {exportarExcel} from '../../core/exportar-excel';
import {exportarPdf} from '../../core/exportar-pdf';
import {periodoValido} from '../../core/filtros-risco';
@Component({selector:'app-relatorios',standalone:true,imports:[FormsModule,CurrencyPipe,DatePipe],templateUrl:'./relatorios.component.html'})
export class RelatoriosComponent implements OnInit {
 dados:Tratativa[]=[];erro='';carregando=true;
 inicio='';fim='';categoria='';status='';produto='';decisao='';busca='';produtos:string[]=[];
 // Os dados já são filtrados pelo servidor: o mesmo conjunto alimenta tela, PDF e Excel.
 constructor(private api:ApiService){}
 ngOnInit():void{this.api.produtos().subscribe({next:lista=>this.produtos=lista,error:()=>this.produtos=[]});this.carregar();}
 carregar():void{if(!periodoValido(this.inicio,this.fim)){this.erro='Data inicial deve ser menor ou igual à final.';return;}
  this.carregando=true;this.erro='';this.api.tratativas({visao:'TODAS',inicio:this.inicio,fim:this.fim,categoria:this.categoria,status:this.status,produto:this.produto,decisao:this.decisao,responsavel:this.busca}).subscribe({
    next:d=>{this.dados=d;this.carregando=false;},error:e=>{this.carregando=false;this.erro=e?.error?.detail||e?.error?.erro||'Falha ao consultar relatório.';}
  });}
 limpar():void{this.inicio='';this.fim='';this.categoria='';this.status='';this.produto='';this.decisao='';this.busca='';this.carregar();}
 get filtrados(){return this.dados;}
 get total(){return this.dados.length;}
 get concluidas(){return this.dados.filter(o=>o.status==='CONCLUIDA').length;}
 get abertas(){return this.total-this.concluidas;}
 get valorTotal(){return this.dados.reduce((s,o)=>s+Number(o.valor||0),0);}
 get categorias(){return ['KYC','PLD','FRAUDE','CHARGEBACK'].map(c=>({nome:c,total:this.dados.filter(x=>x.categoria===c).length,concluidas:this.dados.filter(x=>x.categoria===c&&x.status==='CONCLUIDA').length,valor:this.dados.filter(x=>x.categoria===c).reduce((s,x)=>s+Number(x.valor||0),0)}));}
 linhas():(string|number)[][]{return [['ID','Categoria','Origem','Cliente','Produto','Status','Analista','Decisao','Valor','Data'],...this.dados.map(o=>[o.id,o.categoria,o.origemId,o.clienteId||'',o.produto||'',o.status,o.responsavelNome||'',o.decisao||'',Number(o.valor||0),o.dataOcorrencia||''])];}
 excel():void{if(!this.carregando&&!this.erro)exportarExcel('sgr-relatorio-ocorrencias.xlsx',this.linhas());}
 pdf():void{if(!this.carregando&&!this.erro)exportarPdf('sgr-relatorio-ocorrencias.pdf','SGR - Relatorio de Ocorrencias',this.linhas()[0].map(String),this.linhas().slice(1));}
}
