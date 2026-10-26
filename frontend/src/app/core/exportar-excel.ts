/** Gera uma planilha .xlsx OOXML sem dependências adicionais. ZIP com entradas STORE (sem compressão). */
export function exportarExcel(nome: string, linhas: (string | number | null | undefined)[][]): void {
  const xml = (s: string) => s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&apos;');
  const coluna = (i: number): string => { let t=''; for(let n=i+1;n>0;n=Math.floor((n-1)/26)) t=String.fromCharCode(65+(n-1)%26)+t;return t; };
  const sheet = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>'+
    linhas.map((l,r)=>'<row r="'+(r+1)+'">'+l.map((c,i)=>{
      const ref=coluna(i)+(r+1);
      return typeof c==='number' && Number.isFinite(c)
        ? `<c r="${ref}"><v>${c}</v></c>`
        : `<c r="${ref}" t="inlineStr"><is><t>${xml(String(c??''))}</t></is></c>`;
    }).join('')+'</row>').join('')+'</sheetData></worksheet>';
  const arquivos: [string,string][] = [
    ['[Content_Types].xml','<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>'],
    ['_rels/.rels','<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>'],
    ['xl/workbook.xml','<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Ocorrencias" sheetId="1" r:id="rId1"/></sheets></workbook>'],
    ['xl/_rels/workbook.xml.rels','<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>'],
    ['xl/worksheets/sheet1.xml',sheet]
  ];
  const bytes = new TextEncoder(); const entradas: number[]=[]; const central: number[]=[];
  const u16=(a:number[],n:number)=>a.push(n&255,(n>>8)&255);
  const u32=(a:number[],n:number)=>a.push(n&255,(n>>8)&255,(n>>16)&255,(n>>24)&255);
  const crc=(dados:Uint8Array):number=>{let n=0xffffffff;for(const c of dados){n^=c;for(let j=0;j<8;j++)n=(n>>>1)^((n&1)?0xedb88320:0);}return (n^0xffffffff)>>>0;};
  for(const [caminho,conteudo] of arquivos){
    const n=bytes.encode(caminho),d=bytes.encode(conteudo),offset=entradas.length,checksum=crc(d);
    u32(entradas,0x04034b50);u16(entradas,20);u16(entradas,0);u16(entradas,0);u16(entradas,0);u16(entradas,0);
    u32(entradas,checksum);u32(entradas,d.length);u32(entradas,d.length);u16(entradas,n.length);u16(entradas,0);entradas.push(...n,...d);
    u32(central,0x02014b50);u16(central,20);u16(central,20);u16(central,0);u16(central,0);u16(central,0);u16(central,0);
    u32(central,checksum);u32(central,d.length);u32(central,d.length);u16(central,n.length);u16(central,0);u16(central,0);u16(central,0);u16(central,0);u32(central,0);u32(central,offset);central.push(...n);
  }
  const resultado=[...entradas,...central];u32(resultado,0x06054b50);u16(resultado,0);u16(resultado,0);u16(resultado,arquivos.length);u16(resultado,arquivos.length);u32(resultado,central.length);u32(resultado,entradas.length);u16(resultado,0);
  const blob=new Blob([new Uint8Array(resultado)],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'});
  const link=document.createElement('a');const url=URL.createObjectURL(blob);link.href=url;link.download=nome;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
}
