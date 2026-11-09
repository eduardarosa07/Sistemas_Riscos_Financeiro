"""Testes HTTP não destrutivos. Exigem backend + PostgreSQL executando.

Opcional: SGR_ANALISTA_EMAIL/SGR_ANALISTA_SENHA, SGR_USUARIO_EMAIL/SGR_USUARIO_SENHA.
Executar: python -m unittest -v scripts.teste_integracao_http
"""
from __future__ import annotations
import http.cookiejar
import json
import os
import unittest
import urllib.error
import urllib.request

BASE = os.getenv("SGR_BASE_URL", "http://localhost:8080/api").rstrip("/")


def cliente():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def requisicao(opener, method, path, payload=None, token=None):
    corpo = json.dumps(payload).encode("utf-8") if payload is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["X-CSRF-Token"] = token
    req = urllib.request.Request(BASE + path, method=method, data=corpo, headers=headers)
    try:
        with opener.open(req, timeout=15) as response:
            data = response.read().decode("utf-8")
            try:
                return response.status, json.loads(data)
            except json.JSONDecodeError:
                return response.status, data
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")


def sessao(email, senha):
    op = cliente()
    status, csrf = requisicao(op, "GET", "/auth/csrf")
    if status != 200:
        raise AssertionError(f"CSRF respondeu {status}")
    status, payload = requisicao(op, "POST", "/auth/login", {"email": email, "senha": senha}, csrf["token"])
    if status != 200:
        raise AssertionError(f"Login respondeu {status}: {payload}")
    return op


class ApiSemCredencial(unittest.TestCase):
    def test_status_publico(self):
        status, _ = requisicao(cliente(), "GET", "/status")
        self.assertEqual(status, 200)

    def test_usuario_anonimo_nao_ve_clientes(self):
        status, _ = requisicao(cliente(), "GET", "/clientes")
        self.assertEqual(status, 401)

    def test_csrf_disponivel(self):
        status, resposta = requisicao(cliente(), "GET", "/auth/csrf")
        self.assertEqual(status, 200)
        self.assertTrue(resposta.get("token"))

    def test_post_sem_csrf_deve_ser_negado(self):
        status, _ = requisicao(cliente(), "POST", "/auth/login", {"email": "inválido", "senha": "inválida"})
        self.assertEqual(status, 403)


@unittest.skipUnless(os.getenv("SGR_ANALISTA_EMAIL") and os.getenv("SGR_ANALISTA_SENHA"),
                     "Definir SGR_ANALISTA_EMAIL e SGR_ANALISTA_SENHA para executar testes autenticados")
class ApiAnalista(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.op = sessao(os.environ["SGR_ANALISTA_EMAIL"], os.environ["SGR_ANALISTA_SENHA"])

    def test_visao_de_clientes(self):
        status, dados = requisicao(self.op, "GET", "/clientes/visao")
        self.assertEqual(status, 200)
        self.assertIsInstance(dados, list)

    def test_kpis_dados_agregados(self):
        status, dados = requisicao(self.op, "GET", "/indicadores/ocorrencias")
        self.assertEqual(status, 200)
        self.assertIn("total", dados)
        self.assertEqual(int(dados["total"]), int(dados["abertas"]) + int(dados["concluidas"]))

    def test_periodo_invalido(self):
        status, _ = requisicao(self.op, "GET", "/indicadores/ocorrencias?inicio=2026-12-30&fim=2026-12-01")
        self.assertEqual(status, 400)

    def test_fila_e_historico_disjuntos(self):
        status, fila = requisicao(self.op, "GET", "/tratativas?visao=FILA")
        self.assertEqual(status, 200)
        status, hist = requisicao(self.op, "GET", "/tratativas?visao=HISTORICO")
        self.assertEqual(status, 200)
        self.assertTrue(all(r["status"] != "CONCLUIDA" for r in fila))
        self.assertTrue(all(r["status"] == "CONCLUIDA" for r in hist))

    def test_kyc_fila_e_historico(self):
        for visao in ("PENDENTES", "HISTORICO"):
            status, dados = requisicao(self.op, "GET", "/kycs/fila?visao=" + visao)
            self.assertEqual(status, 200)
            self.assertIsInstance(dados, list)


@unittest.skipUnless(os.getenv("SGR_USUARIO_EMAIL") and os.getenv("SGR_USUARIO_SENHA"),
                     "Definir SGR_USUARIO_EMAIL e SGR_USUARIO_SENHA para executar testes de autorização")
class ApiPerfilUsuario(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.op = sessao(os.environ["SGR_USUARIO_EMAIL"], os.environ["SGR_USUARIO_SENHA"])

    def test_usuario_so_indicadores(self):
        status, _ = requisicao(self.op, "GET", "/indicadores/ocorrencias")
        self.assertEqual(status, 200)
        status, _ = requisicao(self.op, "GET", "/clientes")
        self.assertEqual(status, 403)


if __name__ == "__main__":
    unittest.main(verbosity=2)
