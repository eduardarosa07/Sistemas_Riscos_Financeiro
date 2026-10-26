# Sprint 5 — Segurança, perfis, usuários e modernização SGR

## Estado de validação

Esta é uma entrega de **código-fonte para teste local**, não uma homologação. O ambiente de geração não possuía Maven nem dependências Angular completas; os comandos de compilação não puderam ser concluídos. Nenhuma conexão ao PostgreSQL local do usuário foi realizada. Faça backup e valide o sistema antes de aplicar no banco utilizado na apresentação.

## O que mudou

- Matriz `USUARIO`, `ANALISTA`, `GESTOR`, `ADMINISTRADOR` aplicada no interceptor do backend e nas rotas/menu Angular.
- Usuário comum pode acessar somente `/api/indicadores/resumo`, que retorna contagens agregadas sem registros de clientes ou transações.
- Administração de usuários exclusiva de Administrador; perfil validado no cadastro/edição.
- Revalidação de usuário ativo e versão de sessão em cada requisição protegida; troca de senha, perfil e inativação revogam sessão anterior.
- CSRF exigido em POST, PUT, PATCH e DELETE com token por sessão em `GET /api/auth/csrf`. Interceptor Angular obtém o token antes de cada operação de escrita.
- Fluxos de senha: `POST /api/auth/minha-senha` e `POST /api/usuarios/{id}/redefinir-senha`.
- Registro de auditoria administrativa em `auditoria_usuario`.
- Datas de cadastro/atualização e versão de sessão adicionadas à entidade Usuario.
- Login e navegação com identidade SGR em azul e branco; melhorias de formulário, layout e responsividade.
- Removido fallback de senha PostgreSQL armazenado em application.properties: use `DB_PASSWORD`.

## IMPORTANTE: backup e scripts SQL

1. Faça backup do banco `sistema_riscos` via pgAdmin antes de qualquer alteração.
2. Verifique a tabela `public.usuario` e registre um Administrador ativo.
3. Execute **uma única vez e nessa ordem**, no mesmo banco: `backend/database/03-migrar-perfis-usuarios.sql`, `04-auditoria-e-datas.sql`, `05-restricoes-usuarios.sql`.
4. Se o script 03 falhar por perfil não reconhecido, resolva manualmente a classificação; não apague usuários.
5. Se o script 05 falhar por duplicidade de e-mail normalizado, corrija as duplicidades sem descartar registros.
6. `created_at` de registros legados receberá a data da migração, que NÃO é a data histórica de criação.
7. Como `spring.jpa.hibernate.ddl-auto=validate`, o backend atualizado **não inicia** antes da execução das migrações.
8. Não execute scripts de migração em bases compartilhadas sem validação e backup.

## Preparação e execução local (PowerShell)

Backend, após definir `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` (variáveis com valores reais nunca devem ir para Git):

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend em outra janela:

```powershell
cd frontend
npm ci
npm run build
npm start
```

Abrir `http://localhost:4200`. A API usa `http://localhost:8080`. Para produção HTTPS, configurar `SESSION_COOKIE_SECURE=true`. O frontend está configurado para execução local com origem `http://localhost:4200`.

## Testes obrigatórios

1. Login correto com cada perfil; senha errada e cinco tentativas repetidas.
2. Usuário comum: dashboard somente com totais; nenhum registro individual de KYC, PLD, Fraude ou Cliente na aba Network.
3. Usuário comum: `GET /api/clientes` retorna 403; Analista/Gestor recebem acesso.
4. Analista e Gestor: `GET /api/usuarios` retorna 403; Administrador recebe 200.
5. Administrador: cadastro, edição, reclassificação de perfil, ativação/inativação, redefinição de senha; compare persistência do hash BCrypt.
6. Administrador não consegue inativar a própria conta, nem deixar o sistema sem Administrador ativo.
7. Ao inativar, redefinir senha ou alterar perfil de conta logada, a próxima chamada protegida recebe 401; novo login usa as regras novas.
8. Alteração da própria senha exige senha atual; ao concluir, login anterior perde validade.
9. Duas variações de capitalização do mesmo e-mail não podem coexistir.
10. Verificar datas `created_at`/`updated_at` e registros em `auditoria_usuario`.
11. Uma escrita à API sem `X-CSRF-Token` deve retornar 403.
12. Exercitar login/menu/tabelas/formulários no desktop e em tela menor; avaliar navegação por teclado e leitores de tela.
13. Reexecutar testes de regressão dos módulos de Clientes, KYC, PLD, Fraude, Chargeback e Relatórios.

## Limitações conhecidas e avaliação adicional

- O limitador de tentativas de login é **em memória e por e-mail**. É adequado apenas como proteção inicial para aplicação local; uma proteção por IP, persistência e múltiplas instâncias requer outra abordagem.
- A auditoria registra eventos após sucesso nas operações, mas não é um log inviolável contra administradores do banco. Controle acesso ao PostgreSQL.
- As tabelas financeiras e as consultas preexistentes não foram remodeladas. Estatísticas agregadas não têm filtros detalhados por período, mas não retornam linhas individuais.
- O frontend SGR foi modernizado via CSS/layout, sem imagens externas nem fontes remotas. Não foi possível executar auditoria automatizada WCAG.
- Confirme existência e nomes das tabelas no banco real antes de ativar o dashboard agregado.
- Os scripts versionados não dependem de Flyway; o controle de execução é manual e deverá ser registrado pela equipe.
