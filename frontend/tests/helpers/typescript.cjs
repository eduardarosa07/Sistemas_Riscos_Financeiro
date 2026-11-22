const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

// Testes da lógica dos componentes, com RxJS real e sem navegador/API.
// A verificação de templates e tipos continua sendo feita pelo compilador Angular.
function ambiente() {
  const cache = new Map();
  const blobs = [];
  const root = path.resolve(__dirname, '../..');
  function load(relative) {
    const file = path.isAbsolute(relative) ? relative : path.resolve(root, relative);
    if (cache.has(file)) return cache.get(file);
    const module = {exports:{}};
    cache.set(file, module.exports);
    const code = ts.transpileModule(fs.readFileSync(file, 'utf8'), {
      compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,experimentalDecorators:true}
    }).outputText;
    const localRequire = id => {
      if (id.startsWith('.')) return load(path.resolve(path.dirname(file), id+'.ts'));
      if (id === '@angular/core') return {
        Component:()=>c=>c,
        Injectable:()=>c=>c,
        Input:()=>()=>{},
        Output:()=>()=>{},
        EventEmitter:class{emit(){}}
      };
      if (id === '@angular/common') return {DatePipe:class{},CurrencyPipe:class{},CommonModule:class{}};
      if (id === '@angular/forms') return {FormsModule:class{}};
      if (id === '@angular/router' || id === '@angular/common/http') return {};
      return require(id);
    };
    vm.runInNewContext(code, {
      module,exports:module.exports,require:localRequire,
      Blob,TextEncoder,Uint8Array,console,setTimeout:()=>0,
      URL:{createObjectURL:blob=>(blobs.push(blob),'blob:teste'),revokeObjectURL:()=>{}},
      document:{createElement:()=>({click(){}})}
    }, {filename:file});
    return module.exports;
  }
  return {load,blobs};
}
module.exports = {ambiente};
