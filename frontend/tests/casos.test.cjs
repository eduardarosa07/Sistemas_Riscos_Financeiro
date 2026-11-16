const {test}=require('node:test');
const assert=require('node:assert/strict');
const {of,Subject,throwError}=require('rxjs');
const {ambiente}=require('./helpers/typescript.cjs');
const auth={usuario:()=>({id:3,perfil:'ANALISTA'})};
const route={snapshot:{queryParamMap:{get:()=>null}}};

test('Chargeback nao recebe risco calculado a partir de fraude declarada',()=>{
  const {load}=ambiente();
  const {ocorrenciasDe}=load('src/app/core/risco.utils.ts');
  for(const fraudeDeclarada of [true,false]){
    const dados=ocorrenciasDe([],[],[],[{idChargeback:'CB1',fraudeDeclarada}]);
    assert.equal(dados[0].risco,'');
  }
});

test('KYC reconhece estados finais atuais e historicos',()=>{
  const {load}=ambiente();
  const {kycEncerrado}=load('src/app/core/risco.utils.ts');
  for(const s of ['ACEITO','NAO_ACEITO','APROVADO','REPROVADO',' aprovado '])assert.equal(kycEncerrado(s),true);
  for(const s of ['PENDENTE','EM_ANALISE',null])assert.equal(kycEncerrado(s),false);
});

function dominio(api){
  const {load}=ambiente();
  const {DominioComponent}=load('src/app/pages/dominio/dominio.component.ts');
  const c=new DominioComponent(route,{},api,auth);
  c.categoria='KYC';c.carregando=false;
  c.dados=[{id:'A',status:'PENDENTE'},{id:'B',status:'PENDENTE'}];
  return c;
}

test('Trocar KYC limpa parecer e impede enviar o parecer do caso anterior',()=>{
  const enviados=[];
  const c=dominio({decidirKyc:(...args)=>(enviados.push(args),of({}))});
  c.kycSelecionado='A';c.parecer='Evidencias exclusivas de A';c.decisaoKyc='NAO_ACEITO';
  c.kycSelecionado='B';
  assert.equal(c.parecer,'');assert.equal(c.decisaoKyc,'ACEITO');
  c.decidirKyc();assert.equal(enviados.length,0);
});

test('Selecao KYC fica vinculada ao envio enquanto a gravacao esta em andamento',()=>{
  const retorno=new Subject();const enviados=[];
  const c=dominio({decidirKyc:(...args)=>(enviados.push(args),retorno)});
  c.kycSelecionado='A';c.parecer='Parecer de A';c.decidirKyc();
  c.kycSelecionado='B';
  assert.equal(c.kycSelecionado,'A');assert.equal(enviados[0][0],'A');assert.equal(c.salvando,true);
});

test('Fila KYC nao mistura aprovados legados com pendentes',()=>{
  const c=dominio({filaKyc:()=>of([{idKyc:'OLD',statusKyc:'APROVADO'},{idKyc:'NEW',statusKyc:'PENDENTE'}])});
  c.carregar();assert.equal(c.dados.length,1);assert.equal(c.dados[0].id,'NEW');
  c.visaoKyc='HISTORICO';c.carregar();assert.equal(c.dados.length,1);assert.equal(c.dados[0].id,'OLD');
});

test('Decisao valida envia o caso e o parecer corretos e limpa o formulario apos sucesso',()=>{
  const enviados=[];
  const c=dominio({decidirKyc:(...args)=>(enviados.push(args),of({})),filaKyc:()=>of([])});
  c.kycSelecionado='B';c.parecer='Parecer exclusivo de B';c.decisaoKyc='NAO_ACEITO';
  c.decidirKyc();
  assert.equal(enviados[0][0],'B');assert.equal(enviados[0][1],'NAO_ACEITO');assert.equal(enviados[0][2],'Parecer exclusivo de B');
  assert.equal(c.kycSelecionado,'');assert.equal(c.parecer,'');assert.equal(c.salvando,false);assert.ok(c.mensagem);
});

test('Historico KYC atrasado nao substitui o da nova selecao',()=>{
  const respostas={A:new Subject(),B:new Subject()};
  const c=dominio({historicoKyc:id=>respostas[id]});c.visaoKyc='HISTORICO';
  c.kycSelecionado='A';c.kycSelecionado='B';
  respostas.B.next([{id:2}]);respostas.A.next([{id:1}]);
  assert.equal(c.historicoKyc[0].id,2);c.ngOnDestroy();
});

function ocorrencias(api){
  const {load}=ambiente();
  const {OcorrenciasComponent}=load('src/app/pages/ocorrencias/ocorrencias.component.ts');
  return new OcorrenciasComponent(api,auth,route,{});
}
function caso(id){return {id,origemId:'ORIGEM'+id,categoria:'PLD',status:'PENDENTE',responsavelId:null};}

test('Fechar e abrir outro caso descarta contexto e historico atrasados',()=>{
  const contextos={1:new Subject(),2:new Subject()},historicos={1:new Subject(),2:new Subject()};
  const c=ocorrencias({contextoTratativa:id=>contextos[id],historicoTratativa:id=>historicos[id]});
  c.abrir(caso(1));c.fechar();c.abrir(caso(2));
  contextos[2].next({id:2});historicos[2].next([{id:20}]);
  contextos[1].next({id:1});historicos[1].next([{id:10}]);
  assert.equal(c.selecionada.id,2);assert.equal(c.contexto.id,2);assert.equal(c.historico[0].id,20);
  c.ngOnDestroy();
});

test('Falha de historico e contexto fica distinta de ausencia de eventos',()=>{
  const falha=()=>throwError(()=>new Error('offline'));
  const c=ocorrencias({contextoTratativa:falha,historicoTratativa:falha});
  c.abrir(caso(1));assert.ok(c.erroHistorico);assert.ok(c.erroContexto);assert.equal(c.carregandoHistorico,false);
});

test('Recusa de tratativa permanece visivel apos atualizar a fila',()=>{
  const c=ocorrencias({
    assumirTratativa:()=>throwError(()=>({error:{erro:'Reservada a outro analista'}})),
    tratativas:()=>of([])
  });
  c.selecionada=caso(1);c.executar('assumir');
  assert.match(c.erro,/Reservada a outro analista/);assert.equal(c.executando,false);
});

test('Resposta de gravacao de caso fechado nao abre novamente o painel antigo',()=>{
  const retorno=new Subject();
  const c=ocorrencias({assumirTratativa:()=>retorno,tratativas:()=>of([]),contextoTratativa:()=>of({id:2}),historicoTratativa:()=>of([])});
  c.selecionada=caso(1);c.executar('assumir');c.fechar();c.abrir(caso(2));
  retorno.next({...caso(1),status:'EM_ANALISE'});
  assert.equal(c.selecionada.id,2);assert.equal(c.aviso,'');
});

test('Uma consulta de fila antiga nao sobrepoe filtros mais recentes',()=>{
  const antiga=new Subject(),nova=new Subject();let chamada=0;
  const c=ocorrencias({tratativas:()=>++chamada===1?antiga:nova});
  c.carregar();c.categoria='KYC';c.carregar();nova.next([caso(2)]);antiga.next([caso(1)]);
  assert.equal(c.todas[0].id,2);c.ngOnDestroy();
});

test('Dashboard descarta indicadores anteriores quando a nova consulta falha',()=>{
  const {load}=ambiente();
  const {HomeComponent}=load('src/app/pages/home/home.component.ts');
  const resposta=new Subject();const c=new HomeComponent({indicadoresOcorrencias:()=>resposta});
  c.dados={total:99};c.carregar();
  resposta.error(new Error('offline'));
  assert.equal(c.dados,null);assert.ok(c.erro);assert.equal(c.carregando,false);c.ngOnDestroy();
});

test('Relatorio ignora resposta antiga quando outro filtro ja foi aplicado',()=>{
  const {load}=ambiente();
  const {RelatoriosComponent}=load('src/app/pages/relatorios/relatorios.component.ts');
  const antiga=new Subject(),nova=new Subject();let chamada=0;
  const c=new RelatoriosComponent({tratativas:()=>++chamada===1?antiga:nova});
  c.carregar();c.status='CONCLUIDA';c.carregar();nova.next([caso(2)]);antiga.next([caso(1)]);
  assert.equal(c.dados[0].id,2);c.ngOnDestroy();
});
