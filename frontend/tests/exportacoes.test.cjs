const {test}=require('node:test');
const assert=require('node:assert/strict');
const {crc32}=require('node:zlib');
const {ambiente}=require('./helpers/typescript.cjs');

test('Excel exporta 5000 registros com ZIP valido e ultima linha completa',async()=>{
  const {load,blobs}=ambiente();
  const {exportarExcel}=load('src/app/core/exportar-excel.ts');
  const linhas=[['ID','Categoria','Origem','Cliente','Produto','Status','Analista','Decisao','Valor','Data'],
    ...Array.from({length:5000},(_,i)=>[i,'PLD','ORIGEM_'+i,'CLIENTE_TESTE','PRODUTO','CONCLUIDA','ANALISTA_TESTE','PROCEDENTE',100,'2026-11-15'])];
  exportarExcel('teste.xlsx',linhas);
  const zip=Buffer.from(await blobs[0].arrayBuffer());
  let offset=0,arquivos=0,sheet='';
  while(zip.readUInt32LE(offset)===0x04034b50){
    const length=zip.readUInt32LE(offset+18),nameLength=zip.readUInt16LE(offset+26),extraLength=zip.readUInt16LE(offset+28);
    const name=zip.subarray(offset+30,offset+30+nameLength).toString();
    const inicio=offset+30+nameLength+extraLength,dados=zip.subarray(inicio,inicio+length);
    assert.equal(crc32(dados),zip.readUInt32LE(offset+14));
    if(name==='xl/worksheets/sheet1.xml')sheet=dados.toString('utf8');
    arquivos++;offset=inicio+length;
  }
  assert.equal(arquivos,5);assert.equal((sheet.match(/<row /g)||[]).length,5001);
  assert.ok(sheet.includes('ORIGEM_4999'));assert.ok(sheet.includes('2026-11-15'));
});

test('PDF preserva campos longos, valores, datas e acentos sem cortar colunas',async()=>{
  const {load,blobs}=ambiente();
  const {exportarPdf}=load('src/app/core/exportar-pdf.ts');
  const origem='IDENTIFICADOR'.repeat(16);
  exportarPdf('teste.pdf','Relatorio',['ID','Categoria','Origem','Cliente','Produto','Status','Analista','Decisao','Valor','Data'],
    [['ID1','PLD',origem,'Jo\u00e3o','Produto','CONCLUIDA','Analista','PROCEDENTE',987654,'2026-11-15']]);
  const dados=Buffer.from(await blobs[0].arrayBuffer()),pdf=dados.toString('latin1');
  const textos=[...pdf.matchAll(/\(((?:\\.|[^\\)])*)\) Tj/g)].map(m=>m[1].replace(/\\([\\()])/g,'$1'));
  const tabela=textos.slice(3);
  const origemExportada=tabela.map(l=>l.split(' | ')[2]?.trimEnd()||'').join('');
  assert.equal(origemExportada,origem);
  assert.ok(pdf.includes('987654'));assert.ok(pdf.includes('2026-11-15'));assert.ok(pdf.includes('Jo\u00e3o'));
  const inicio=Number(pdf.match(/startxref\n(\d+)/)[1]);
  assert.equal(dados.subarray(inicio,inicio+4).toString(),'xref');
  for(const m of pdf.matchAll(/\/Length (\d+) >>\nstream\n/g)){
    const inicioStream=m.index+m[0].length;
    assert.equal(pdf.slice(inicioStream+Number(m[1]),inicioStream+Number(m[1])+9),'endstream');
  }
});

test('PDF pagina e preserva o ultimo registro',async()=>{
  const {load,blobs}=ambiente();
  load('src/app/core/exportar-pdf.ts').exportarPdf('teste.pdf','Relatorio',['ID','Valor'],
    Array.from({length:100},(_,i)=>['REGISTRO_'+i,i]));
  const pdf=Buffer.from(await blobs[0].arrayBuffer()).toString('latin1');
  assert.ok(pdf.includes('REGISTRO_99'));assert.ok(Number(pdf.match(/\/Count (\d+)/)[1])>1);
});
