package sr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;

@Configuration
public class DadosIniciaisConfig {

    @Bean
    CommandLineRunner criarAdministradorInicial(
            UsuarioRepository repository,
            PasswordEncoder encoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String senha,
            @Value("${app.bootstrap-admin.name:Administrador do Sistema}") String nome) {

        return args -> {
            if (repository.count() == 0 && !email.isBlank() && !senha.isBlank()) {
                Usuario admin = new Usuario();
                admin.setNome(nome.trim());
                admin.setEmail(email.trim().toLowerCase());
                admin.setSenhaHash(encoder.encode(senha));
                admin.setPerfil("ADMINISTRADOR");
                admin.setStatus("ATIVO");
                repository.save(admin);
            }
        };
    }
}
