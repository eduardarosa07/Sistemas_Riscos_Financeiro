import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { perfilGuard } from './core/perfil.guard';
import { LoginComponent } from './pages/login/login.component';
import { ShellComponent } from './layout/shell.component';
import { HomeComponent } from './pages/home/home.component';
import { ClientesComponent } from './pages/clientes/clientes.component';
import { ClienteDetalheComponent } from './pages/cliente-detalhe/cliente-detalhe.component';
import { OcorrenciasComponent } from './pages/ocorrencias/ocorrencias.component';
import { DominioComponent } from './pages/dominio/dominio.component';
import { RelatoriosComponent } from './pages/relatorios/relatorios.component';
import { UsuariosComponent } from './pages/usuarios/usuarios.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'home', canActivate: [perfilGuard], data: { perfis: ['USUARIO','ANALISTA','GESTOR'] }, component: HomeComponent },
      { path: 'clientes', canActivate: [perfilGuard], data: { perfis: ['ANALISTA','GESTOR'] }, component: ClientesComponent },
      { path: 'clientes/:id', canActivate: [perfilGuard], data: { perfis: ['ANALISTA','GESTOR'] }, component: ClienteDetalheComponent },
      { path: 'ocorrencias', canActivate: [perfilGuard], data: { perfis: ['ANALISTA','GESTOR'] }, component: OcorrenciasComponent },
      { path: 'ocorrencias/pld', canActivate: [perfilGuard], data: { perfis: ['ANALISTA','GESTOR'], dominio: 'pld' }, component: DominioComponent },
      { path: 'ocorrencias/chargeback', canActivate: [perfilGuard], data: { perfis: ['ANALISTA','GESTOR'], dominio: 'chargeback' }, component: DominioComponent },
      { path: 'ocorrencias/kyc', canActivate: [perfilGuard], data: { perfis: ['ANALISTA','GESTOR'], dominio: 'kyc' }, component: DominioComponent },
      { path: 'ocorrencias/fraude', canActivate: [perfilGuard], data: { perfis: ['ANALISTA','GESTOR'], dominio: 'fraude' }, component: DominioComponent },
      { path: 'relatorios', canActivate: [perfilGuard], data: { perfis: ['ANALISTA','GESTOR'] }, component: RelatoriosComponent },
      { path: 'usuarios', canActivate: [perfilGuard], data: { perfis: ['ADMINISTRADOR'] }, component: UsuariosComponent },
      { path: '', pathMatch: 'full', redirectTo: 'home' }
    ]
  },
  { path: '**', redirectTo: 'home' }
];
