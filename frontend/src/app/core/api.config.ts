import { InjectionToken } from '@angular/core';

/**
 * Endereço único da API do SGR.
 *
 * O token permite substituir a URL no bootstrap ou em testes sem espalhar
 * valores fixos pelos serviços e interceptadores HTTP.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => 'http://localhost:8080/api'
});
