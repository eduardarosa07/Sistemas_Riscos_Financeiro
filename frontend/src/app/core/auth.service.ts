import { Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { ApiService } from './api.service';
import { LoginResponse } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly usuario = signal<LoginResponse | null>(null);

  constructor(private api: ApiService) {}

  login(email: string, senha: string): Observable<LoginResponse> {
    return this.api.login(email, senha).pipe(tap(u => this.usuario.set(u)));
  }

  verificarSessao(): Observable<boolean> {
    return this.api.me().pipe(
      tap(u => this.usuario.set(u)),
      map(() => true),
      catchError(() => {
        this.usuario.set(null);
        return of(false);
      })
    );
  }

  logout(): Observable<void> {
    return this.api.logout().pipe(tap(() => this.usuario.set(null)),catchError(() => {this.usuario.set(null);return of(void 0);}));
  }
}
