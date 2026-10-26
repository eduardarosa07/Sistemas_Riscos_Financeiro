import {inject} from '@angular/core';
import {CanActivateFn,Router} from '@angular/router';
import {map} from 'rxjs';
import {AuthService} from './auth.service';
export const perfilGuard: CanActivateFn=(route)=>{
 const auth=inject(AuthService),router=inject(Router);
 const perfis=route.data['perfis'] as string[];
 return auth.verificarSessao().pipe(map(ok=>!ok?router.createUrlTree(['/login']):perfis.includes(auth.usuario()?.perfil||'')?true:router.createUrlTree([auth.usuario()?.perfil==='ADMINISTRADOR'?'/usuarios':'/home'])));
};
