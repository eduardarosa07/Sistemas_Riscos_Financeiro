
import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';

import {
  FormsModule,
  ReactiveFormsModule,
  FormControl,
  FormGroup,
  Validators
} from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Usuario } from '../../core/models';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    DatePipe
  ],
  templateUrl: './usuarios.component.html'
})
export class UsuariosComponent implements OnInit {

  usuarios: Usuario[] = [];

  carregando = true;
  erro = '';
  busca = '';
  perfil = 'TODOS';
  situacao = 'TODAS';

  modal = false;
  editando?: Usuario;
  salvando = false;
  mensagem = '';

  perfis = [
    'USUARIO',
    'ANALISTA',
    'GESTOR',
    'ADMINISTRADOR'
  ];

  senhaModal = false;
  usuarioSenha?: Usuario;
  novaSenha = '';
  erroRedefinicao = '';
  salvandoRedefinicao = false;

  form = new FormGroup({
    nome: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
    }),

    email: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.email
      ]
    }),

    senha: new FormControl('', {
      nonNullable: true
    }),

    perfil: new FormControl('ANALISTA', {
      nonNullable: true,
      validators: [Validators.required]
    })
  });

  constructor(
    private api: ApiService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.carregar();
  }

  // IDENTIFICA O ADMINISTRADOR AUTENTICADO

  get usuarioLogado() {
    return this.auth.usuario();
  }

  get meuId(): number | null {
    return this.usuarioLogado?.id ?? null;
  }

  // VERIFICA SE O USUÁRIO É A PRÓPRIA CONTA

  ehMinhaConta(usuario?: Usuario): boolean {
    if (!usuario || this.meuId === null) {
      return false;
    }

    return usuario.id === this.meuId;
  }

  // IDENTIFICA A EDIÇÃO DO PRÓPRIO ADMINISTRADOR

  get editandoMinhaConta(): boolean {
    return this.ehMinhaConta(this.editando);
  }

  // LISTAR USUÁRIOS

  carregar(): void {
    this.carregando = true;
    this.erro = '';

    this.api.usuarios().subscribe({
      next: usuarios => {
        this.usuarios = usuarios;
        this.carregando = false;
      },

      error: () => {
        this.carregando = false;
        this.erro =
          'Não foi possível listar usuários. ' +
          'Verifique a comunicação com o servidor.';
      }
    });
  }

  // FILTRAR USUÁRIOS

  get filtrados(): Usuario[] {
    const q = this.busca.toLowerCase().trim();

    return this.usuarios.filter(usuario => {
      const correspondeBusca =
        !q ||
        usuario.nome.toLowerCase().includes(q) ||
        usuario.email.toLowerCase().includes(q);

      const correspondePerfil =
        this.perfil === 'TODOS' ||
        usuario.perfil === this.perfil;

      const correspondeSituacao =
        this.situacao === 'TODAS' ||
        usuario.status === this.situacao;

      return (
        correspondeBusca &&
        correspondePerfil &&
        correspondeSituacao
      );
    });
  }

  // CADASTRAR NOVO USUÁRIO

  novo(): void {
    this.editando = undefined;

    this.form.reset({
      nome: '',
      email: '',
      senha: '',
      perfil: 'ANALISTA'
    });

    this.form.controls.perfil.enable({
      emitEvent: false
    });

    this.modal = true;
    this.mensagem = '';
  }

  // EDITAR USUÁRIO

  editar(usuario: Usuario): void {
    this.editando = usuario;

    this.form.controls.perfil.enable({
      emitEvent: false
    });

    this.form.reset({
      nome: usuario.nome,
      email: usuario.email,
      senha: '',
      perfil: usuario.perfil
    });

    // O administrador pode editar seus dados,
    // mas não modificar o próprio perfil.

    if (this.ehMinhaConta(usuario)) {
      this.form.controls.perfil.disable({
        emitEvent: false
      });
    }

    this.modal = true;
    this.mensagem = '';
  }

  // SALVAR CADASTRO OU ALTERAÇÃO

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();

    if (!this.editando && valores.senha.length < 8) {
      this.mensagem =
        'A senha deve ter no mínimo 8 caracteres.';
      return;
    }

    // getRawValue() inclui controles desabilitados.
    // Na edição da própria conta, preservamos
    // explicitamente o perfil original.

    const perfilParaSalvar =
      this.editandoMinhaConta && this.editando
        ? this.editando.perfil
        : valores.perfil;

    this.salvando = true;
    this.mensagem = '';

    const requisicao = this.editando
      ? this.api.atualizarUsuario(
          this.editando.id,
          {
            nome: valores.nome,
            email: valores.email,
            perfil: perfilParaSalvar
          }
        )
      : this.api.cadastrarUsuario({
          nome: valores.nome,
          email: valores.email,
          senha: valores.senha,
          perfil: perfilParaSalvar
        });

    requisicao.subscribe({
      next: () => {
        this.salvando = false;
        this.modal = false;
        this.editando = undefined;
        this.carregar();
      },

      error: erro => {
        this.salvando = false;

        this.mensagem =
          erro?.error?.message ||
          erro?.error?.erro ||
          'Não foi possível salvar o usuário.';
      }
    });
  }

  // ABRIR MODAL DE REDEFINIÇÃO DE SENHA

  abrirSenha(usuario: Usuario): void {

    if (this.ehMinhaConta(usuario)) {
      this.erro =
        'Para alterar sua própria senha, utilize ' +
        'a opção "Alterar minha senha".';
      return;
    }

    this.usuarioSenha = usuario;
    this.novaSenha = '';
    this.erroRedefinicao = '';
    this.senhaModal = true;
  }

  // REDEFINIR SENHA DE OUTRO USUÁRIO

  redefinirSenha(): void {

    if (!this.usuarioSenha) {
      return;
    }

    if (this.ehMinhaConta(this.usuarioSenha)) {
      this.erroRedefinicao =
        'Utilize a opção Alterar minha senha.';
      return;
    }

    if (this.novaSenha.length < 8) {
      this.erroRedefinicao =
        'A senha deve ter no mínimo 8 caracteres.';
      return;
    }

    this.salvandoRedefinicao = true;
    this.erroRedefinicao = '';

    this.api.redefinirSenha(
      this.usuarioSenha.id,
      this.novaSenha
    ).subscribe({
      next: () => {
        this.salvandoRedefinicao = false;
        this.senhaModal = false;
        this.usuarioSenha = undefined;
        this.novaSenha = '';

        this.mensagem =
          'Senha redefinida com sucesso.';
      },

      error: erro => {
        this.salvandoRedefinicao = false;

        this.erroRedefinicao =
          erro?.error?.message ||
          erro?.error?.erro ||
          'Não foi possível redefinir a senha.';
      }
    });
  }

  // ATIVAR OU INATIVAR USUÁRIO

  alternar(usuario: Usuario): void {

    const novoStatus: 'ATIVO' | 'INATIVO' =
      usuario.status === 'ATIVO'
        ? 'INATIVO'
        : 'ATIVO';

    // IMPEDIR AUTO-INATIVAÇÃO

    if (
      novoStatus === 'INATIVO' &&
      this.ehMinhaConta(usuario)
    ) {
      this.erro =
        'O administrador não pode inativar ' +
        'a própria conta.';
      return;
    }

    const acao =
      novoStatus === 'INATIVO'
        ? 'inativar'
        : 'ativar';

    if (!confirm(
      `Confirma ${acao} ${usuario.nome}?`
    )) {
      return;
    }

    this.erro = '';

    this.api.alterarStatusUsuario(
      usuario.id,
      novoStatus
    ).subscribe({
      next: () => {
        this.carregar();
      },

      error: erro => {
        this.erro =
          erro?.error?.message ||
          erro?.error?.erro ||
          'Não foi possível alterar o status do usuário.';
      }
    });
  }
}
