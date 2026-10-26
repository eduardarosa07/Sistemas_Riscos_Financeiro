package sr.dto;
import jakarta.validation.constraints.*;
public record RedefinirSenhaRequest(@NotBlank @Size(min=8,max=72) String novaSenha) {}
