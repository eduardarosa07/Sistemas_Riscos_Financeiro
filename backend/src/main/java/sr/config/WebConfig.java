package sr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutenticacaoInterceptor autenticacao;
    private final ProtecaoCsrfInterceptor csrf;

    @Value("${app.frontend.origin:http://localhost:4200}")
    private String frontendOrigin;

    public WebConfig(
            AutenticacaoInterceptor autenticacao,
            ProtecaoCsrfInterceptor csrf) {
        this.autenticacao = autenticacao;
        this.csrf = csrf;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(csrf)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/status");

        registry.addInterceptor(autenticacao)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/me",
                "/api/auth/csrf",
                "/api/status"
            );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(frontendOrigin)
            .allowedMethods(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
            )
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
