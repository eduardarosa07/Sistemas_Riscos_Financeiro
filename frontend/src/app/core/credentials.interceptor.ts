import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { HttpClient, HttpBackend } from '@angular/common/http';
import { switchMap } from 'rxjs';
export const credentialsInterceptor: HttpInterceptorFn=(req,next)=>{
 const request=req.clone({withCredentials:true});
 if(!['POST','PUT','PATCH','DELETE'].includes(req.method) || req.url.endsWith('/auth/csrf')) return next(request);
 const plain=new HttpClient(inject(HttpBackend));
 return plain.get<{token:string}>('http://localhost:8080/api/auth/csrf',{withCredentials:true})
 .pipe(switchMap(({token})=>next(request.clone({setHeaders:{'X-CSRF-Token':token}}))));
};
