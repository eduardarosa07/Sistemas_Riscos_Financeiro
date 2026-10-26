import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import {
  AlertaFraude, AlertaPld, Chargeback, Cliente, ClienteHistorico, Tratativa, TratativaEvento, IndicadoresOcorrencias,
  Kyc, LoginResponse, RiscoCliente, Usuario
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  readonly baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  status(): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/status`);
  }

  login(email: string, senha: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, { email, senha });
  }

  me(): Observable<LoginResponse> {
    return this.http.get<LoginResponse>(`${this.baseUrl}/auth/me`);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {});
  }

  clientes(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(`${this.baseUrl}/clientes`);
  }

  clienteHistorico(id: string): Observable<ClienteHistorico> {
    return this.http.get<ClienteHistorico>(`${this.baseUrl}/clientes/${encodeURIComponent(id)}/historico`);
  }

  kycs(): Observable<Kyc[]> {
    return this.http.get<Kyc[]>(`${this.baseUrl}/kycs`);
  }

  plds(): Observable<AlertaPld[]> {
    return this.http.get<AlertaPld[]>(`${this.baseUrl}/alertas-pld`);
  }

  fraudes(): Observable<AlertaFraude[]> {
    return this.http.get<AlertaFraude[]>(`${this.baseUrl}/alertas-fraude`);
  }

  fraudesFiltradas(filtros: {
    status?: string; severidade?: string; tipoFraude?: string; busca?: string
  }): Observable<AlertaFraude[]> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([chave, valor]) => {
      if (valor) params = params.set(chave, valor);
    });
    return this.http.get<AlertaFraude[]>(`${this.baseUrl}/alertas-fraude/filtros`, { params });
  }

  chargebacks(): Observable<Chargeback[]> {
    return this.http.get<Chargeback[]>(`${this.baseUrl}/chargebacks`);
  }

  riscos(): Observable<RiscoCliente[]> {
    return this.http.get<RiscoCliente[]>(`${this.baseUrl}/riscos-clientes`);
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

  resumo(): Observable<{clientes:number;transacoes:number;fraude:number;pld:number;kyc:number;chargeback:number}> {
    return this.http.get<{clientes:number;transacoes:number;fraude:number;pld:number;kyc:number;chargeback:number}>(`${this.baseUrl}/indicadores/resumo`);
  }
  minhaSenha(senhaAtual:string,novaSenha:string):Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/minha-senha`,{senhaAtual,novaSenha});
  }
  redefinirSenha(id:number,novaSenha:string):Observable<void>{
    return this.http.post<void>(`${this.baseUrl}/usuarios/${id}/redefinir-senha`,{novaSenha});
  }
  tratativas(filtros: { categoria?:string; status?:string; inicio?:string; fim?:string; produto?:string; decisao?:string; responsavel?:string } = {}):Observable<Tratativa[]> {
    let params=new HttpParams();Object.entries(filtros).forEach(([k,v])=>{if(v)params=params.set(k,v);});
    return this.http.get<Tratativa[]>(`${this.baseUrl}/tratativas`,{params});
  }
  assumirTratativa(id:number):Observable<Tratativa>{return this.http.post<Tratativa>(`${this.baseUrl}/tratativas/${id}/assumir`,{});}
  solicitarParecer(id:number,justificativa:string):Observable<Tratativa>{return this.http.post<Tratativa>(`${this.baseUrl}/tratativas/${id}/parecer`,{justificativa});}
  concluirTratativa(id:number,decisao:string,justificativa:string):Observable<Tratativa>{return this.http.post<Tratativa>(`${this.baseUrl}/tratativas/${id}/concluir`,{decisao,justificativa});}
  historicoTratativa(id:number):Observable<TratativaEvento[]>{return this.http.get<TratativaEvento[]>(`${this.baseUrl}/tratativas/${id}/historico`);}
  decidirKyc(id:string,decisao:'ACEITO'|'NAO_ACEITO',parecer:string):Observable<unknown>{return this.http.post(`${this.baseUrl}/kycs/${encodeURIComponent(id)}/decisao`,{decisao,parecer});}
  produtos():Observable<string[]>{return this.http.get<string[]>(`${this.baseUrl}/indicadores/produtos`);}
  indicadoresOcorrencias(filtros: {inicio?:string;fim?:string;categoria?:string;status?:string;produto?:string}={}):Observable<IndicadoresOcorrencias>{let params=new HttpParams();Object.entries(filtros).forEach(([k,v])=>{if(v)params=params.set(k,v);});return this.http.get<IndicadoresOcorrencias>(`${this.baseUrl}/indicadores/ocorrencias`,{params});}
  dadosRisco() {
    return forkJoin({
      clientes: this.clientes(),
      kycs: this.kycs(),
      plds: this.plds(),
      fraudes: this.fraudes(),
      chargebacks: this.chargebacks(),
      riscos: this.riscos()
    });
  }
}
