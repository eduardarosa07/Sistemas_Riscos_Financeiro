/** Exportação PDF simples, paginada, em formato A4 paisagem. Sem bibliotecas externas. */
export function exportarPdf(nome:string,titulo:string,cabecalhos:string[],linhas:(string|number|null|undefined)[][]):void{
 const ascii=(v:unknown)=>String(v??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^\x20-\x7e]/g,'?');
 const esc=(s:string)=>ascii(s).replace(/\\/g,'\\\\').replace(/\(/g,'\\(').replace(/\)/g,'\\)');
 const paginas: string[][]=[];
 const conteudo=[cabecalhos,...linhas].map(l=>l.map(x=>ascii(x).slice(0,28)).join(' | ').slice(0,155));
 for(let i=0;i<conteudo.length;i+=42)paginas.push(conteudo.slice(i,i+42));
 if(paginas.length===0)paginas.push([]);
 const objeto:string[]=[''];
 objeto[1]='<< /Type /Catalog /Pages 2 0 R >>';
 objeto[2]='';
 objeto[3]='<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>';
 const paginasIds:number[]=[];
 for(let i=0;i<paginas.length;i++){
   const pageId=4+i*2,streamId=pageId+1;paginasIds.push(pageId);
   let cmd=`BT /F1 15 Tf 32 555 Td (${esc(titulo)}) Tj ET\n`;
   cmd+=`BT /F1 9 Tf 32 538 Td (SGR - pagina ${i+1} de ${paginas.length}) Tj ET\n`;
   let y=518;
   for(const l of paginas[i]){cmd+=`BT /F1 8 Tf 32 ${y} Td (${esc(l)}) Tj ET\n`;y-=11;}
   const streamSize=new TextEncoder().encode(cmd).length;
   objeto[pageId]=`<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 3 0 R >> >> /Contents ${streamId} 0 R >>`;
   objeto[streamId]=`<< /Length ${streamSize} >>\nstream\n${cmd}endstream`;
 }
 objeto[2]=`<< /Type /Pages /Kids [${paginasIds.map(i=>`${i} 0 R`).join(' ')}] /Count ${paginasIds.length} >>`;
 const blocos:string[]=['%PDF-1.4\n'];const offsets=[0];let position=blocos[0].length;
 for(let i=1;i<objeto.length;i++){offsets[i]=position;const s=`${i} 0 obj\n${objeto[i]}\nendobj\n`;blocos.push(s);position+=new TextEncoder().encode(s).length;}
 const inicioXref=position;
 let xref=`xref\n0 ${objeto.length}\n0000000000 65535 f \n`;
 for(let i=1;i<objeto.length;i++)xref+=`${String(offsets[i]).padStart(10,'0')} 00000 n \n`;
 xref+=`trailer\n<< /Size ${objeto.length} /Root 1 0 R >>\nstartxref\n${inicioXref}\n%%EOF`;
 blocos.push(xref);
 const blob=new Blob([new TextEncoder().encode(blocos.join(''))],{type:'application/pdf'});
 const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=nome;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
