import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { podeOperacional,podeAdministrar } from '../core/permissions';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive,FormsModule],
  templateUrl: './shell.component.html'
})
export class ShellComponent {
  menuRecolhido = false;
  menuCelular = false;
  modalSenha = false; senhaAtual=''; novaSenha=''; confirmacao=''; erroSenha=''; salvandoSenha=false;
  get podeDashboard(){return this.podeOperacional || this.auth.usuario()?.perfil==='USUARIO';}
  get podeOperacional(){return podeOperacional(this.auth.usuario()?.perfil);}
  get podeAdministrar(){return podeAdministrar(this.auth.usuario()?.perfil);}
  alterarSenha():void {
    if(this.novaSenha.length<8 || this.novaSenha!==this.confirmacao){this.erroSenha='Confira a nova senha (mínimo 8 caracteres).';return;}
    this.salvandoSenha=true;this.erroSenha='';this.api.minhaSenha(this.senhaAtual,this.novaSenha).subscribe({
      next:()=>{this.salvandoSenha=false;this.modalSenha=false;this.auth.usuario.set(null);this.router.navigate(['/login']);},
      error:e=>{this.salvandoSenha=false;this.erroSenha=e?.error?.erro||'Não foi possível alterar a senha.';}
    });
  }

  ocorrenciasAbertas = true;

  constructor(public auth: AuthService,private api:ApiService, private router: Router) {}

  sair(): void {
    this.auth.logout().subscribe({ next: () => this.router.navigate(['/login']) });
  }
}
