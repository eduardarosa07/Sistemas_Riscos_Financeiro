import {Component,OnInit} from '@angular/core';
import {CurrencyPipe,DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ApiService} from '../../core/api.service';
import {Tratativa} from '../../core/models';
import {exportarExcel} from '../../core/exportar-excel';
import {exportarPdf} from '../../core/exportar-pdf';
@Component({selector:'app-relatorios',standalone:true,imports:[FormsModule,CurrencyPipe,DatePipe],templateUrl:'./relatorios.component.html'})
export class RelatoriosComponent implements OnInit {
 dados:Tratativa[]=[];erro='';carregando=true;inicio='';fim='';categoria='';status='';produto='';decisao='';busca='';produtos:string[]=[];
 constructor(private api:ApiService){}
 ngOnInit():void{this.api.produtos().subscribe({next:lista=>this.produtos=lista,error:()=>this.produtos=[]});this.carregar();}
 carregar():void{if(this.inicio&&this.fim&&this.inicio>this.fim){this.erro='Data inicial deve ser menor ou igual à final.';return;}
  this.carregando=true;this.erro='';this.api.tratativas({inicio:this.inicio,fim:this.fim,categoria:this.categoria,status:this.status,produto:this.produto,decisao:this.decisao}).subscribe({next:d=>{this.dados=d;this.carregando=false;},error:e=>{this.carregando=false;this.erro=e?.error?.erro||e?.error?.detail||'Falha ao consultar relatório.';}});}
 limpar():void{this.inicio='';this.fim='';this.categoria='';this.status='';this.produto='';this.decisao='';this.busca='';this.carregar();}
 get filtrados(){let q=this.busca.trim().toLowerCase();return this.dados.filter(o=>!q||String(o.id).includes(q)||o.origemId.toLowerCase().includes(q)||String(o.responsavelNome||'').toLowerCase().includes(q));}
 get total(){return this.filtrados.length;}
 get concluidas(){return this.filtrados.filter(o=>o.status==='CONCLUIDA').length;}
 get abertas(){return this.total-this.concluidas;}
 get valorTotal(){return this.filtrados.reduce((s,o)=>s+Number(o.valor||0),0);}
 get categorias(){return ['PLD','FRAUDE','CHARGEBACK'].map(c=>({nome:c,total:this.filtrados.filter(x=>x.categoria===c).length,concluidas:this.filtrados.filter(x=>x.categoria===c&&x.status==='CONCLUIDA').length,valor:this.filtrados.filter(x=>x.categoria===c).reduce((s,x)=>s+Number(x.valor||0),0)}));}
 linhas(): (string|number)[][]{return [['ID','Categoria','Origem','Cliente','Produto','Status','Analista','Decisao','Valor','Data'],...this.filtrados.map(o=>[o.id,o.categoria,o.origemId,o.clienteId||'',o.produto||'',o.status,o.responsavelNome||'',o.decisao||'',Number(o.valor||0),o.dataOcorrencia||''])];}
 excel():void{exportarExcel('sgr-relatorio-ocorrencias.xlsx',this.linhas());}
 pdf():void{exportarPdf('sgr-relatorio-ocorrencias.pdf','SGR - Relatorio de Ocorrencias',this.linhas()[0].map(String),this.linhas().slice(1));}
}
