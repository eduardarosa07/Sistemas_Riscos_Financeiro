const {test}=require('node:test');
const assert=require('node:assert/strict');
const {periodoValido,selecoesCsv,filtrarClientes}=require('../.unit-test/filtros-risco.js');
const {podeOperacional,podeAdministrar,podeTratar}=require('../.unit-test/permissions.js');
const clientes=[
 {idCliente:'CLI001',nomeRazaoSocial:'Ana Silva',documentoFicticio:'1234',tipoPessoa:'PF',statusCliente:'ATIVO',riscoGeral:'ALTO'},
 {idCliente:'CLI002',nomeRazaoSocial:'Empresa Beta',documentoFicticio:'5678',tipoPessoa:'PJ',statusCliente:'INATIVO',riscoGeral:'BAIXO'}
];
const base={busca:'',tipo:'TODOS',situacao:'TODAS',risco:'TODOS'};
test('RF29: data inicial anterior à final',()=>assert.equal(periodoValido('2026-11-01','2026-11-30'),true));
test('RF29: datas invertidas',()=>assert.equal(periodoValido('2026-11-30','2026-11-01'),false));
test('RF29: período aberto permitido',()=>assert.equal(periodoValido('','2026-11-01'),true));
test('RF29: formato inválido rejeitado',()=>assert.equal(periodoValido('01/11/2026',''),false));
test('RF30-RF33: serializar seleções combinadas',()=>assert.equal(selecoesCsv(['PLD','PLD','FRAUDE']),'PLD,FRAUDE'));
test('RF30-RF33: seleção vazia equivale a todas',()=>assert.equal(selecoesCsv([]),''));
test('RF06: lista sem filtros',()=>assert.equal(filtrarClientes(clientes,base).length,2));
test('RF06: busca por nome',()=>assert.equal(filtrarClientes(clientes,{...base,busca:'ana'}).length,1));
test('RF06: busca por documento',()=>assert.equal(filtrarClientes(clientes,{...base,busca:'5678'})[0].idCliente,'CLI002'));
test('RF39: filtrar por risco geral',()=>assert.equal(filtrarClientes(clientes,{...base,risco:'ALTO'})[0].idCliente,'CLI001'));
test('RF06: filtros de situação e tipo combinados',()=>assert.equal(filtrarClientes(clientes,{...base,tipo:'PJ',situacao:'ATIVO'}).length,0));
test('RF05: perfil analista possui visão operacional',()=>assert.equal(podeOperacional('ANALISTA'),true));
test('RF05: usuário padrão não pode operar',()=>assert.equal(podeOperacional('USUARIO'),false));
test('RF34: somente administrador pode administrar',()=>assert.equal(podeAdministrar('ADMINISTRADOR'),true));
test('RF34: gestor não pode administrar',()=>assert.equal(podeAdministrar('GESTOR'),false));
test('RF23: somente analista pode tratar',()=>{assert.equal(podeTratar('ANALISTA'),true);assert.equal(podeTratar('GESTOR'),false);});
