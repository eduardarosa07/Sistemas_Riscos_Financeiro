package sr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sr.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
    long countByPerfilAndStatus(String perfil,String status);

}