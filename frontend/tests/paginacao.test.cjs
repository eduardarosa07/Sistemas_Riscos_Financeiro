const { test } = require('node:test');
const assert = require('node:assert/strict');
const {
  normalizarPagina,
  normalizarTamanhoPagina,
  paginar,
  totalPaginas
} = require('../.unit-test/paginacao.js');

const itens = Array.from({ length: 23 }, (_, indice) => indice + 1);

test('paginação usa somente os tamanhos 10, 50 e 100', () => {
  assert.equal(normalizarTamanhoPagina(10), 10);
  assert.equal(normalizarTamanhoPagina(50), 50);
  assert.equal(normalizarTamanhoPagina(100), 100);
  assert.equal(normalizarTamanhoPagina(25), 10);
});

test('paginação calcula a quantidade de páginas', () => {
  assert.equal(totalPaginas(23, 10), 3);
  assert.equal(totalPaginas(50, 50), 1);
  assert.equal(totalPaginas(0, 10), 1);
});

test('paginação limita páginas fora do intervalo', () => {
  assert.equal(normalizarPagina(0, 23, 10), 1);
  assert.equal(normalizarPagina(20, 23, 10), 3);
});

test('paginação retorna somente os registros da página atual', () => {
  assert.deepEqual(paginar(itens, 1, 10), itens.slice(0, 10));
  assert.deepEqual(paginar(itens, 2, 10), itens.slice(10, 20));
  assert.deepEqual(paginar(itens, 3, 10), itens.slice(20, 23));
});
