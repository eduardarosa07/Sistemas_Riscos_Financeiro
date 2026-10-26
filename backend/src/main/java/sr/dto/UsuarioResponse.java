package sr.dto;
import java.time.OffsetDateTime;
public record UsuarioResponse(Long id,String nome,String email,String perfil,String status,OffsetDateTime createdAt,OffsetDateTime updatedAt) {}
