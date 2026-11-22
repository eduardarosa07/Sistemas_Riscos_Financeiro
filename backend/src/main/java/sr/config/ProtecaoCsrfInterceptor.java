package sr.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ProtecaoCsrfInterceptor implements HandlerInterceptor {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String token(HttpSession sessao) {
        String token = (String) sessao.getAttribute("csrfToken");

        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
            sessao.setAttribute("csrfToken", token);
        }

        return token;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        String metodo = request.getMethod();
        if ("GET".equals(metodo)
                || "HEAD".equals(metodo)
                || "OPTIONS".equals(metodo)) {
            return true;
        }

        HttpSession sessao = request.getSession(false);
        String enviado = request.getHeader("X-CSRF-Token");
        String esperado = sessao == null
            ? null
            : (String) sessao.getAttribute("csrfToken");

        if (esperado == null
                || enviado == null
                || !tokensIguais(esperado, enviado)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    private boolean tokensIguais(String esperado, String enviado) {
        return MessageDigest.isEqual(
            esperado.getBytes(StandardCharsets.UTF_8),
            enviado.getBytes(StandardCharsets.UTF_8)
        );
    }
}
