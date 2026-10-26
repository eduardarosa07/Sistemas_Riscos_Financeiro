package sr.config;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Component public class ProtecaoCsrfInterceptor implements HandlerInterceptor {
 private static final SecureRandom RANDOM=new SecureRandom();
 public static String token(HttpSession s){String t=(String)s.getAttribute("csrfToken");
  if(t==null){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);t=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);s.setAttribute("csrfToken",t);}return t;}
 @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse resp,Object handler){
  String method=req.getMethod();if("GET".equals(method)||"HEAD".equals(method)||"OPTIONS".equals(method))return true;
  HttpSession s=req.getSession(false);String sent=req.getHeader("X-CSRF-Token");
  String expected=s==null?null:(String)s.getAttribute("csrfToken");
  if(expected==null||sent==null||!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),sent.getBytes(StandardCharsets.UTF_8))){resp.setStatus(403);return false;}
  return true;
 }
}
