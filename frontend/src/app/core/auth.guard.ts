import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';
export const authGuard: CanActivateFn=()=>{
 const auth=inject(AuthService),router=inject(Router);
 return auth.verificarSessao().pipe(map(ok=>ok?true:router.createUrlTree(['/login'])));
};
