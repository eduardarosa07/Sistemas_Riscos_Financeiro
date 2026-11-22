import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AlertaFraude,
  AlertaPld,
  Chargeback,
  ClienteHistorico,
  ClienteResumo,
  ContextoTratativa,
  IndicadoresOcorrencias,
  Kyc,
  LoginResponse,
  Tratativa,
  TratativaEvento,
  Usuario
} from './models';
import { API_BASE_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) private readonly baseUrl: string
  ) {}

  login(email: string, senha: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, { email, senha });
  }

  me(): Observable<LoginResponse> {
    return this.http.get<LoginResponse>(`${this.baseUrl}/auth/me`);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {});
  }

  clientesVisao(): Observable<ClienteResumo[]> {
    return this.http.get<ClienteResumo[]>(`${this.baseUrl}/clientes/visao`);
  }

  clienteHistorico(id: string): Observable<ClienteHistorico> {
    return this.http.get<ClienteHistorico>(`${this.baseUrl}/clientes/${encodeURIComponent(id)}/historico`);
  }

  plds(): Observable<AlertaPld[]> {
    return this.http.get<AlertaPld[]>(`${this.baseUrl}/alertas-pld`);
  }

  fraudes(): Observable<AlertaFraude[]> {
    return this.http.get<AlertaFraude[]>(`${this.baseUrl}/alertas-fraude`);
  }

  chargebacks(): Observable<Chargeback[]> {
    return this.http.get<Chargeback[]>(`${this.baseUrl}/chargebacks`);
  }

  usuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.baseUrl}/usuarios`);
  }

  cadastrarUsuario(payload: { nome: string; email: string; senha: string; perfil: string }): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.baseUrl}/usuarios`, payload);
  }

  atualizarUsuario(id: number, payload: { nome: string; email: string; perfil: string }): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.baseUrl}/usuarios/${id}`, payload);
  }

  alterarStatusUsuario(id: number, status: 'ATIVO' | 'INATIVO'): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.baseUrl}/usuarios/${id}/status`, { status });
  }

  minhaSenha(senhaAtual: string, novaSenha: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/minha-senha`, {
      senhaAtual,
      novaSenha
    });
  }

  redefinirSenha(id: number, novaSenha: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/usuarios/${id}/redefinir-senha`, {
      novaSenha
    });
  }

  tratativas(
    filtros: {
      visao?: string;
      categoria?: string;
      status?: string;
      inicio?: string;
      fim?: string;
      produto?: string;
      decisao?: string;
      responsavel?: string;
    } = {}
  ): Observable<Tratativa[]> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([chave, valor]) => {
      if (valor) params = params.set(chave, valor);
    });
    return this.http.get<Tratativa[]>(`${this.baseUrl}/tratativas`, { params });
  }

  tratativaPorOrigem(categoria: string, origemId: string): Observable<Tratativa> {
    return this.http.get<Tratativa>(`${this.baseUrl}/tratativas/origem`, {
      params: { categoria, origemId }
    });
  }

  contextoTratativa(id: number): Observable<ContextoTratativa> {
    return this.http.get<ContextoTratativa>(`${this.baseUrl}/tratativas/${id}/contexto`);
  }

  filaKyc(visao: 'PENDENTES' | 'HISTORICO' = 'PENDENTES'): Observable<Kyc[]> {
    return this.http.get<Kyc[]>(`${this.baseUrl}/kycs/fila`, {
      params: { visao }
    });
  }

  historicoKyc(id: string): Observable<TratativaEvento[]> {
    return this.http.get<TratativaEvento[]>(`${this.baseUrl}/kycs/${encodeURIComponent(id)}/historico`);
  }

  assumirTratativa(id: number): Observable<Tratativa> {
    return this.http.post<Tratativa>(`${this.baseUrl}/tratativas/${id}/assumir`, {});
  }

  solicitarParecer(id: number, justificativa: string): Observable<Tratativa> {
    return this.http.post<Tratativa>(`${this.baseUrl}/tratativas/${id}/parecer`, {
      justificativa
    });
  }

  concluirTratativa(
    id: number,
    decisao: string,
    justificativa: string
  ): Observable<Tratativa> {
    return this.http.post<Tratativa>(`${this.baseUrl}/tratativas/${id}/concluir`, {
      decisao,
      justificativa
    });
  }

  historicoTratativa(id: number): Observable<TratativaEvento[]> {
    return this.http.get<TratativaEvento[]>(`${this.baseUrl}/tratativas/${id}/historico`);
  }

  decidirKyc(
    id: string,
    decisao: 'ACEITO' | 'NAO_ACEITO',
    parecer: string
  ): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/kycs/${encodeURIComponent(id)}/decisao`, {
      decisao,
      parecer
    });
  }

  produtos(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/indicadores/produtos`);
  }

  indicadoresOcorrencias(
    filtros: {
      inicio?: string;
      fim?: string;
      categoria?: string;
      status?: string;
      produto?: string;
    } = {}
  ): Observable<IndicadoresOcorrencias> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([chave, valor]) => {
      if (valor) params = params.set(chave, valor);
    });
    return this.http.get<IndicadoresOcorrencias>(`${this.baseUrl}/indicadores/ocorrencias`, {
      params
    });
  }
}
