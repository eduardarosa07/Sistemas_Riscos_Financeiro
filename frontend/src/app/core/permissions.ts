export const PERFIS = ['USUARIO','ANALISTA','GESTOR','ADMINISTRADOR'] as const;
export function podeOperacional(perfil?:string):boolean{return perfil==='ANALISTA'||perfil==='GESTOR';}
export function podeAdministrar(perfil?:string):boolean{return perfil==='ADMINISTRADOR';}
export function podeTratar(perfil?:string):boolean{return perfil==='ANALISTA';}
