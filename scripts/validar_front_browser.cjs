const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),assert=require('node:assert/strict');
const {chromium}=require(process.env.SGR_PLAYWRIGHT_MODULE || 'playwright');
const root=path.resolve(process.argv[2] || 'frontend/dist/sistemas-riscos-financeiros/browser');
if(!fs.existsSync(path.join(root,'index.html')))throw new Error('Informe o diretorio browser do build Angular como primeiro argumento.');
const resultados=[];const jsErrors=[];let perfil='ANALISTA',decisoes=[];
const user=()=>({id:1,nome:'Analista de Ensaio',email:'ensaio@example.test',perfil});
const cliente={idCliente:'CLI_A',nomeRazaoSocial:'Cliente de Ensaio A',documentoFicticio:'DOC-TESTE-A',tipoPessoa:'PF',statusCliente:'ATIVO',cidade:'Cidade Teste',uf:'GO',segmento:'ENSAIO',rendaFaturamentoMensal:1000};
const casos=[1,2].map(id=>({id,categoria:'PLD',origemId:'ORIGEM_'+id,status:'PENDENTE',responsavelId:null,clienteId:'CLI_A',criadoEm:'2026-11-15T12:00:00Z',dataOcorrencia:'2026-11-15T12:00:00Z',valor:100,produto:'PIX'}));
const kycs=['A','B','OLD'].map(id=>({idKyc:id,cliente,nivelRiscoKyc:'BAIXO',scoreKyc:17,dataAnalise:'2026-11-15',statusKyc:id==='OLD'?'APROVADO':'PENDENTE'}));
const server=http.createServer((req,res)=>{let p=path.join(root,decodeURIComponent(req.url.split('?')[0]));if(!fs.existsSync(p)||fs.statSync(p).isDirectory())p=path.join(root,'index.html');res.setHeader('Content-Type',p.endsWith('.js')?'application/javascript':p.endsWith('.css')?'text/css':'text/html');fs.createReadStream(p).pipe(res);});
(async()=>{
 await new Promise(r=>server.listen(14200,'127.0.0.1',r));
 const edge=process.env.SGR_BROWSER_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
 const browser=await chromium.launch({headless:true,...(fs.existsSync(edge)?{executablePath:edge}:{})});
 const context=await browser.newContext({acceptDownloads:true});
 await context.route('**/api/**',async route=>{
  const req=route.request(),u=new URL(req.url()),p=u.pathname;let data={},status=200;
  const headers={'Access-Control-Allow-Origin':'http://127.0.0.1:14200','Access-Control-Allow-Credentials':'true','Access-Control-Allow-Headers':'Content-Type,X-CSRF-Token','Access-Control-Allow-Methods':'GET,POST,PUT,PATCH,DELETE,OPTIONS'};
  if(req.method()==='OPTIONS'){await route.fulfill({status:204,headers});return;}
  if(p.endsWith('/auth/csrf'))data={token:'ensaio'};
  else if(p.endsWith('/auth/me')||p.endsWith('/auth/login'))data=user();
  else if(p.endsWith('/clientes/visao'))data=[{...cliente,riscoGeral:'ALTO',scoreRiscoGeral:42}];
  else if(p.endsWith('/clientes/CLI_A/historico'))data={cliente,contas:[],transacoes:[],kycs:[],alertasFraude:[],alertasPld:[],chargebacks:[],riscoCliente:{nivelRisco:'ALTO',scoreRiscoGeral:42,scoreKyc:17}};
  else if(p.endsWith('/kycs/fila'))data=kycs.filter(k=>u.searchParams.get('visao')==='HISTORICO'?['ACEITO','APROVADO'].includes(k.statusKyc):k.statusKyc==='PENDENTE');
  else if(/\/kycs\/[^/]+\/decisao$/.test(p)){const id=p.split('/').at(-2);const body=req.postDataJSON();decisoes.push({id,...body});kycs.find(k=>k.idKyc===id).statusKyc=body.decisao;data={};}
  else if(p.endsWith('/kycs/OLD/historico'))data=[];
  else if(p.endsWith('/chargebacks'))data=[{idChargeback:'CB1',cliente,fraudeDeclarada:true,valorContestado:100,dataContestacao:'2026-11-15',statusChargeback:'PENDENTE'}];
  else if(p.endsWith('/tratativas'))data=u.searchParams.get('visao')==='TODAS'?Array.from({length:5000},(_,i)=>({...casos[0],id:i+1,origemId:'REGISTRO_'+i})):casos;
  else if(/\/tratativas\/[12]\/contexto$/.test(p)){const id=Number(p.split('/').at(-2));await new Promise(r=>setTimeout(r,id===1?600:10));data={id,clienteId:'CLI_'+id,clienteNome:'CONTEXTO_'+id,riscoGeral:'BAIXO'};}
  else if(/\/tratativas\/[12]\/historico$/.test(p)){const id=Number(p.split('/').at(-2));await new Promise(r=>setTimeout(r,id===1?600:10));data=[{id,acao:'ASSUMIR',executor_nome:'Ensaio',status_novo:'EM_ANALISE',justificativa:'EVENTO_'+id,registrado_em:'2026-11-15T12:00:00Z'}];}
  else if(p.endsWith('/indicadores/produtos')||p.endsWith('/usuarios'))data=[];
  else if(p.endsWith('/indicadores/ocorrencias'))data={total:2,abertas:2,concluidas:0,diaAtual:2,mesAtual:2,porCategoria:[],porStatus:[],porCategoriaStatus:[],porProduto:[],evolucao:[]};
  else {status=404;data={erro:'Rota de ensaio nao prevista'};}
  try{await route.fulfill({status,headers,contentType:'application/json',body:JSON.stringify(data)});}catch(e){if(!/closed|disposed|handled|canceled/i.test(e.message))throw e;}
 });
 const page=await context.newPage();page.on('pageerror',e=>jsErrors.push(e.message));page.setDefaultTimeout(15000);
 const base='http://127.0.0.1:14200';
 async function testar(nome,fn){try{await fn();resultados.push({nome,resultado:'APROVADO'});}catch(e){resultados.push({nome,resultado:'FALHOU',erro:e.message.slice(0,450)});}}
 await testar('Login e navegacao de Analista',async()=>{await page.goto(base+'/login');await page.locator('#login-email').fill('ensaio@example.test');await page.locator('#login-senha').fill('SENHA-SOMENTE-SIMULADA');await page.getByRole('button',{name:'Entrar',exact:true}).click();await page.waitForURL('**/home');});
 await testar('Risco recebido na lista e detalhe',async()=>{await page.goto(base+'/clientes');const row=page.locator('tbody tr').filter({hasText:'CLI_A'});await row.waitFor();assert.match(await row.innerText(),/ALTO/);await row.click();await page.waitForURL('**/clientes/CLI_A');assert.match(await page.locator('.client-summary').innerText(),/ALTO/);assert.match(await page.locator('.client-summary').innerText(),/42/);});
 await testar('Chargeback nao inventa risco com fraude declarada',async()=>{await page.goto(base+'/ocorrencias/chargeback');await page.getByText('CB1',{exact:false}).waitFor();assert.equal(await page.locator('tbody .risk-pill').innerText(),'Não informado');});
 await testar('Troca KYC limpa parecer e envia apenas a analise escolhida',async()=>{await page.goto(base+'/ocorrencias/kyc');const painel=page.locator('.panel').filter({has:page.getByRole('heading',{name:'Decisão cadastral KYC'})});await painel.locator('select').first().selectOption('A');await painel.locator('textarea').fill('Parecer exclusivo de A');await painel.locator('select').first().selectOption('B');assert.equal(await painel.locator('textarea').inputValue(),'');assert.equal(await painel.getByRole('button').isDisabled(),true);await painel.locator('textarea').fill('Parecer exclusivo de B');await painel.getByRole('button').click();await page.getByRole('status').filter({hasText:'Decisão KYC registrada'}).waitFor();assert.equal(decisoes.length,1);assert.equal(decisoes[0].id,'B');assert.equal(decisoes[0].parecer,'Parecer exclusivo de B');});
 await testar('KYC legado aparece no historico',async()=>{await page.goto(base+'/ocorrencias/kyc');await page.getByText('Visão KYC',{exact:false}).locator('select').selectOption('HISTORICO');await page.locator('tbody').getByText('#OLD',{exact:true}).waitFor();assert.match(await page.locator('tbody').innerText(),/Aprovado/);});
 await testar('Painel nao mistura contexto e historico apos troca rapida',async()=>{await page.goto(base+'/ocorrencias');await page.getByRole('button',{name:'Detalhes',exact:true}).first().click();await page.getByRole('button',{name:'Fechar',exact:true}).last().click();await page.getByRole('button',{name:'Detalhes',exact:true}).nth(1).click();await page.getByText('CONTEXTO_2',{exact:false}).waitFor();await page.waitForTimeout(900);const modal=page.getByRole('dialog');const text=await modal.innerText();assert.ok(text.includes('EVENTO_2'));assert.ok(!text.includes('EVENTO_1'));assert.ok(!text.includes('CONTEXTO_1'));await modal.getByRole('button',{name:'Fechar',exact:true}).last().click();});
 await testar('Excel e PDF com 5000 registros no navegador',async()=>{await page.goto(base+'/relatorios');await page.getByRole('button',{name:'Exportar Excel'}).waitFor();await page.waitForFunction(()=>!document.querySelector('.loading-card'),null,{timeout:45000});const [excel]=await Promise.all([page.waitForEvent('download'),page.getByRole('button',{name:'Exportar Excel'}).click()]);assert.ok(fs.statSync(await excel.path()).size>100000);const [pdf]=await Promise.all([page.waitForEvent('download'),page.getByRole('button',{name:'Exportar PDF'}).click()]);const bytes=fs.readFileSync(await pdf.path());assert.equal(bytes.subarray(0,8).toString(),'%PDF-1.4');assert.ok(bytes.includes(Buffer.from('REGISTRO_499')));});
 await testar('Guard impede Administrador em clientes',async()=>{perfil='ADMINISTRADOR';await page.goto(base+'/clientes');await page.waitForURL('**/usuarios');});
 assert.equal(jsErrors.length,0,'Erros JavaScript: '+jsErrors.join(';'));
 console.log(JSON.stringify({tipo:'NAVEGADOR_COM_API_SIMULADA',resultados,errosJavaScript:jsErrors},null,2));
 await browser.close();server.close();if(resultados.some(r=>r.resultado==='FALHOU'))process.exitCode=1;
})().catch(e=>{console.error(e.stack);server.close();process.exitCode=1;});