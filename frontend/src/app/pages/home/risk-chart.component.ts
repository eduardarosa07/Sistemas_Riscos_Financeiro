import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
@Component({selector:'app-risk-chart',standalone:true,imports:[CommonModule],template:`
<div class="risk-chart" role="img" [attr.aria-label]="titulo+': '+descricao">
 @if(tipo==='donut'){
  <div class="risk-donut" [style.background]="gradiente()"><div class="risk-donut-inner"><strong>{{total}}</strong><small>ocorrências</small></div></div>
  <div class="risk-legend">@for(item of itens;track item.rotulo;let i=$index){<span><i [style.background]="cor(i)"></i>{{item.rotulo}}: {{item.valor}}</span>}</div>
 }@else if(tipo==='linha'){
  <svg class="risk-line" viewBox="0 0 620 205" preserveAspectRatio="none" aria-hidden="true">
   <line x1="25" y1="180" x2="600" y2="180" stroke="#d6e2ef"/>
   <polyline [attr.points]="pontos()" fill="none" stroke="#1464d2" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
   @for(item of itens;track $index;let i=$index){<circle [attr.cx]="x(i)" [attr.cy]="y(item.valor)" r="4" fill="#1464d2"/>}
  </svg>
  <div class="risk-axis">@for(item of itens;track $index){<span>{{item.rotulo}}</span>}</div>
 }@else {
  @for(item of itens;track item.rotulo;let i=$index){<div class="risk-bar-row"><span class="risk-bar-label">{{item.rotulo}}</span><div class="risk-bar-track"><div class="risk-bar-fill" [style.width.%]="porcentagem(item.valor)" [style.background]="cor(i)"></div></div><b>{{item.valor}}</b></div>}
 }
 @if(!itens.length){<p class="empty">Sem dados para os filtros selecionados.</p>}
</div>`})
export class RiskChartComponent {
 @Input() titulo='Gráfico'; @Input() tipo:'barras'|'donut'|'linha'='barras';
 @Input() itens:{rotulo:string;valor:number}[]=[];
 readonly cores=['#165eef','#18a37b','#f0a52f','#8b5cf6','#e15c67','#0f91bd','#687d94'];
 get total(){return this.itens.reduce((a,i)=>a+i.valor,0)}
 cor(i:number){return this.cores[i%this.cores.length]}
 porcentagem(v:number){return this.itens.length?100*v/Math.max(...this.itens.map(i=>i.valor),1):0}
 gradiente(){if(!this.total)return '#e8eef6';let offset=0;const slices=this.itens.map((item,i)=>{let start=offset;offset+=100*item.valor/this.total;return `${this.cor(i)} ${start}% ${offset}%`});return `conic-gradient(${slices.join(',')})`}
 get descricao(){return this.itens.map(x=>x.rotulo+' '+x.valor).join('; ')}
 x(i:number){return 25+(this.itens.length<2?285:i*575/(this.itens.length-1))}
 y(v:number){return 180-150*v/Math.max(...this.itens.map(x=>x.valor),1)}
 pontos(){return this.itens.map((item,i)=>`${this.x(i)},${this.y(item.valor)}`).join(' ')}
}
