import { HttpBackend, HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { API_BASE_URL } from './api.config';

const METODOS_COM_ESCRITA = ['POST', 'PUT', 'PATCH', 'DELETE'];

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const request = req.clone({ withCredentials: true });

  if (!METODOS_COM_ESCRITA.includes(req.method) || req.url.endsWith('/auth/csrf')) {
    return next(request);
  }

  const httpSemInterceptadores = new HttpClient(inject(HttpBackend));
  const apiBaseUrl = inject(API_BASE_URL);

  return httpSemInterceptadores
    .get<{ token: string }>(`${apiBaseUrl}/auth/csrf`, { withCredentials: true })
    .pipe(
      switchMap(({ token }) =>
        next(request.clone({ setHeaders: { 'X-CSRF-Token': token } }))
      )
    );
};
