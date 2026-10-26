import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { Cliente } from '../../core/models';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './clientes.component.html'
})
export class ClientesComponent implements OnInit {
  clientes: Cliente[] = [];
  carregando = true;
  erro = '';
  busca = '';
  tipo = 'TODOS';
  situacao = 'TODAS';

  constructor(private api: ApiService, private router: Router) {}
  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando = true; this.erro = '';
    this.api.clientes().subscribe({
      next: x => { this.clientes = x; this.carregando = false; },
      error: () => { this.carregando = false; this.erro = 'Backend indisponível. Nenhum dado de demonstração será exibido.'; }
    });
  }

  get filtrados(): Cliente[] {
    const q = this.busca.trim().toLowerCase();
    return this.clientes.filter(c =>
      (!q || c.idCliente.toLowerCase().includes(q) || c.nomeRazaoSocial.toLowerCase().includes(q) || c.documentoFicticio.toLowerCase().includes(q)) &&
      (this.tipo === 'TODOS' || c.tipoPessoa === this.tipo) &&
      (this.situacao === 'TODAS' || c.statusCliente.toUpperCase() === this.situacao)
    );
  }

  limpar(): void { this.busca = ''; this.tipo = 'TODOS'; this.situacao = 'TODAS'; }
  abrir(c: Cliente): void { this.router.navigate(['/clientes', c.idCliente]); }
}
