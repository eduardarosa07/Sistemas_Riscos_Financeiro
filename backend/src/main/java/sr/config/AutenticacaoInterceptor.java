
package sr.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import sr.dto.LoginResponse;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;

import java.util.Objects;

@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    private final UsuarioRepository repo;

    public AutenticacaoInterceptor(UsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        // Permitir requisições CORS OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Validar sessão do usuário
        HttpSession session = request.getSession(false);

        if (session == null ||
                !(session.getAttribute("usuarioLogado")
                        instanceof LoginResponse login)) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );
            return false;
        }

        // Consultar usuário autenticado
        Usuario user = repo.findById(login.getId()).orElse(null);

        if (user == null ||
                !"ATIVO".equals(user.getStatus()) ||
                !Objects.equals(
                        user.getSessaoVersao(),
                        session.getAttribute("sessaoVersao"))) {

            session.invalidate();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );
            return false;
        }

        // Atualizar dados da sessão
        session.setAttribute(
                "usuarioLogado",
                new LoginResponse(
                        user.getId(),
                        user.getNome(),
                        user.getEmail(),
                        user.getPerfil()
                )
        );

        // Dados da requisição
        String path = request.getRequestURI();
        String method = request.getMethod();
        String perfil = user.getPerfil();

        // Identificar operações de leitura
        boolean leitura =
                "GET".equalsIgnoreCase(method) ||
                "HEAD".equalsIgnoreCase(method);

        // Perfis do sistema
        boolean admin = "ADMINISTRADOR".equals(perfil);
        boolean analista = "ANALISTA".equals(perfil);
        boolean gestor = "GESTOR".equals(perfil);

        // Preservar compatibilidade com perfil existente
        boolean usuario = "USUARIO".equals(perfil);

        // Rotas de autenticação
        boolean auth = path.startsWith("/api/auth/");

        // Indicadores permitidos ao perfil USUARIO
        boolean indicadoresUsuario =
                usuario &&
                path.startsWith("/api/indicadores/") &&
                leitura;

        // Rotas administrativas
        boolean administracao =
                path.startsWith("/api/usuarios") ||
                path.startsWith("/api/auditoria-usuarios");

        // Acesso operacional
        boolean operacional = analista || gestor;

        // Tratativas
        boolean rotaTratativa =
                path.startsWith("/api/tratativas");

        // Decisões KYC
        boolean escritaKyc =
                path.matches("/api/kycs/[^/]+/decisao") &&
                "POST".equalsIgnoreCase(method);

        // Bloquear operações genéricas de escrita
        boolean escritaLegada =
                !leitura &&
                (
                        path.startsWith("/api/alertas-pld") ||
                        path.startsWith("/api/alertas-fraude") ||
                        path.startsWith("/api/chargebacks") ||
                        path.startsWith("/api/kycs") ||
                        path.startsWith("/api/clientes") ||
                        path.startsWith("/api/transacoes") ||
                        path.startsWith("/api/contas") ||
                        path.startsWith("/api/regras-risco") ||
                        path.startsWith("/api/riscos-clientes")
                );

        // Controle de autorização
        boolean acessoNegado;

        if (administracao) {

            // Administração exclusiva do Administrador
            acessoNegado = !admin;

        } else if (auth) {

            // Ações autenticadas da própria conta
            acessoNegado = false;

        } else if (indicadoresUsuario) {

            // Preservar perfil USUARIO existente
            acessoNegado = false;

        } else if (rotaTratativa) {

            // Analista pode consultar e tratar.
            // Gestor pode apenas consultar.
            acessoNegado = leitura
                    ? !operacional
                    : !analista;

        } else if (escritaKyc) {

            // Decisão KYC exclusiva do Analista
            acessoNegado = !analista;

        } else if (escritaLegada) {

            // Bloquear escrita genérica nos módulos consultivos
            acessoNegado = true;

        } else {

            // Acesso aos demais módulos operacionais
            acessoNegado = !operacional;
        }

        if (acessoNegado) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN
            );

            return false;
        }

        return true;
    }
}
