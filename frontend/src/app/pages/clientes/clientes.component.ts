import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { ClienteResumo } from '../../core/models';
import {filtrarClientes} from '../../core/filtros-risco';

@Component({selector:'app-clientes',standalone:true,imports:[FormsModule],templateUrl:'./clientes.component.html'})
export class ClientesComponent implements OnInit {
  clientes:ClienteResumo[]=[]; carregando=true;erro='';
  busca='';tipo='TODOS';situacao='TODAS';risco='TODOS';
  // Filtros editáveis só são aplicados ao clicar em Filtrar.
  aplicados={busca:'',tipo:'TODOS',situacao:'TODAS',risco:'TODOS'};
  constructor(private api:ApiService,private router:Router){}
  ngOnInit():void{this.carregar();}
  carregar():void{this.carregando=true;this.erro='';this.api.clientesVisao().subscribe({
    next:x=>{this.clientes=x;this.carregando=false;},
    error:()=>{this.carregando=false;this.erro='Não foi possível consultar clientes e riscos consolidados.';}
  });}
  filtrar():void{this.aplicados={busca:this.busca,tipo:this.tipo,situacao:this.situacao,risco:this.risco};}
  limpar():void{this.busca='';this.tipo='TODOS';this.situacao='TODAS';this.risco='TODOS';this.filtrar();}
  get filtrados():ClienteResumo[]{return filtrarClientes(this.clientes,this.aplicados);}
  abrir(c:ClienteResumo):void{this.router.navigate(['/clientes',c.idCliente]);}
}
