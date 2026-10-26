package sr.dto;
import jakarta.validation.constraints.*;
public record AlterarMinhaSenhaRequest(@NotBlank String senhaAtual, @NotBlank @Size(min=8,max=72) String novaSenha) {}
