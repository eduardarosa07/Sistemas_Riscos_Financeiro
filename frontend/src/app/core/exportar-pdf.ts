/** Exportação PDF A4 paisagem. Quebra campos em linhas, sem cortar o conteúdo. */
export function exportarPdf(nome:string,titulo:string,cabecalhos:string[],linhas:(string|number|null|undefined)[][]):void {
  // Helvetica/Courier usam WinAnsi: preservar acentos portugueses. Caracteres
  // fora dessa codificação são representados por seu escape Unicode legível.
  const winAnsi:Record<string,number>={'€':128,'‚':130,'ƒ':131,'„':132,'…':133,'†':134,'‡':135,'ˆ':136,'‰':137,'Š':138,'‹':139,'Œ':140,'Ž':142,'‘':145,'’':146,'“':147,'”':148,'•':149,'–':150,'—':151,'˜':152,'™':153,'š':154,'›':155,'œ':156,'ž':158,'Ÿ':159};
  const texto=(valor:unknown):string=>Array.from(String(valor??'').replace(/\r\n?/g,'\n')).map(c=>{
    if(c==='\n')return c;
    if(c==='\t')return '    ';
    const codigo=c.codePointAt(0)!;
    if((codigo>=32&&codigo<=126)||(codigo>=160&&codigo<=255))return c;
    if(winAnsi[c]!==undefined)return String.fromCharCode(winAnsi[c]);
    return '\\u{'+codigo.toString(16).toUpperCase()+'}';
  }).join('');
  const esc=(s:string)=>s.replace(/\\/g,'\\\\').replace(/\(/g,'\\(').replace(/\)/g,'\\)');
  const colunas=linhas.reduce((n,l)=>Math.max(n,l.length),Math.max(1,cabecalhos.length));
  const largura=Math.max(1,Math.floor((150-(colunas-1)*3)/colunas));
  const quebrar=(s:string):string[]=>s.split('\n').flatMap(parte=>{
    const partes:string[]=[];
    for(let i=0;i<parte.length;i+=largura)partes.push(parte.slice(i,i+largura));
    return partes.length?partes:[''];
  });
  const formatar=(linha:(string|number|null|undefined)[]):string[]=>{
    const celulas=Array.from({length:colunas},(_,i)=>quebrar(texto(linha[i])));
    const altura=celulas.reduce((n,c)=>Math.max(n,c.length),1);
    return Array.from({length:altura},(_,i)=>celulas.map(c=>(c[i]||'').padEnd(largura)).join(' | '));
  };
  const cabecalho=formatar(cabecalhos);
  const paginas:string[][]=[];
  const maxLinhas=42;
  const novoPagina=()=>{const pagina:string[]=[];paginas.push(pagina);return pagina;};
  let pagina=novoPagina();
  // Cabeçalhos excepcionalmente longos também são preservados integralmente.
  for(const linha of cabecalho){if(pagina.length===maxLinhas)pagina=novoPagina();pagina.push(linha);}
  for(const registro of linhas){
    const partes=formatar(registro);
    if(partes.length<=maxLinhas-cabecalho.length && pagina.length+partes.length>maxLinhas){
      pagina=novoPagina();pagina.push(...cabecalho);
    }
    for(const parte of partes){
      if(pagina.length===maxLinhas){
        pagina=novoPagina();
        if(cabecalho.length<maxLinhas)pagina.push(...cabecalho);
      }
      pagina.push(parte);
    }
  }
  const objetos:string[]=[''];
  objetos[1]='<< /Type /Catalog /Pages 2 0 R >>';
  objetos[3]='<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>';
  const ids:number[]=[];
  for(let i=0;i<paginas.length;i++){
    const pageId=4+i*2,streamId=pageId+1;ids.push(pageId);
    let cmd=`BT /F1 15 Tf 32 555 Td (${esc(texto(titulo))}) Tj ET\n`;
    cmd+=`BT /F1 9 Tf 32 538 Td (SGR - pagina ${i+1} de ${paginas.length}) Tj ET\n`;
    let y=518;
    for(const linha of paginas[i]){cmd+=`BT /F1 8 Tf 32 ${y} Td (${esc(linha)}) Tj ET\n`;y-=11;}
    objetos[pageId]=`<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 3 0 R >> >> /Contents ${streamId} 0 R >>`;
    objetos[streamId]=`<< /Length ${cmd.length} >>\nstream\n${cmd}endstream`;
  }
  objetos[2]=`<< /Type /Pages /Kids [${ids.map(i=>`${i} 0 R`).join(' ')}] /Count ${ids.length} >>`;
  const blocos=['%PDF-1.4\n'];const offsets=[0];let posicao=blocos[0].length;
  for(let i=1;i<objetos.length;i++){
    offsets[i]=posicao;
    const bloco=`${i} 0 obj\n${objetos[i]}\nendobj\n`;blocos.push(bloco);posicao+=bloco.length;
  }
  const inicioXref=posicao;
  let xref=`xref\n0 ${objetos.length}\n0000000000 65535 f \n`;
  for(let i=1;i<objetos.length;i++)xref+=`${String(offsets[i]).padStart(10,'0')} 00000 n \n`;
  xref+=`trailer\n<< /Size ${objetos.length} /Root 1 0 R >>\nstartxref\n${inicioXref}\n%%EOF`;
  blocos.push(xref);
  const dados=Uint8Array.from(blocos.join(''),c=>c.charCodeAt(0));
  const blob=new Blob([dados],{type:'application/pdf'});
  const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=nome;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
}