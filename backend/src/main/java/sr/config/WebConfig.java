package sr.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration public class WebConfig implements WebMvcConfigurer {
 private final AutenticacaoInterceptor auth;
 private final ProtecaoCsrfInterceptor csrf;
 @Value("${app.frontend.origin:http://localhost:4200}") private String frontendOrigin;
 public WebConfig(AutenticacaoInterceptor a,ProtecaoCsrfInterceptor c){auth=a;csrf=c;}
 @Override public void addInterceptors(InterceptorRegistry registry){
  registry.addInterceptor(csrf).addPathPatterns("/api/**").excludePathPatterns("/api/status");
  registry.addInterceptor(auth).addPathPatterns("/api/**")
   .excludePathPatterns("/api/auth/login","/api/auth/me","/api/auth/csrf","/api/status");
 }
 @Override public void addCorsMappings(CorsRegistry registry){registry.addMapping("/api/**")
  .allowedOrigins(frontendOrigin).allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
  .allowedHeaders("*").allowCredentials(true);}
}
