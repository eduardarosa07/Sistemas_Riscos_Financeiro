import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  carregando = false;
  exibirSenha = false;
  erro = '';

  form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    senha: new FormControl('', { nonNullable: true, validators: [Validators.required] })
  });

  constructor(private auth: AuthService, private router: Router) {}

  entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.carregando = true;
    this.erro = '';
    const { email, senha } = this.form.getRawValue();
    this.auth.login(email, senha).subscribe({
      next: (u) => this.router.navigate([u.perfil==='ADMINISTRADOR'?'/usuarios':'/home']),
      error: (err) => {
        this.carregando = false;
        this.erro = err?.error?.message || 'Não foi possível autenticar. Verifique o backend e as credenciais.';
      }
    });
  }
}
