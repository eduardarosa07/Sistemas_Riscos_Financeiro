package sr.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve ser válido")
    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Column(name = "senha_hash", nullable = false, length = 60)
    private String senhaHash;

    @NotBlank(message = "O status é obrigatório")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @NotBlank(message = "O perfil é obrigatório")
    @Column(name = "perfil", nullable = false, length = 50)
    private String perfil;

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;
    @Column(name="sessao_versao", nullable=false)
    private Long sessaoVersao = 0L;
    @PrePersist public void criarDatas() { createdAt=OffsetDateTime.now(); updatedAt=createdAt; }
    @PreUpdate public void atualizarData() { updatedAt=OffsetDateTime.now(); }
    public OffsetDateTime getCreatedAt(){return createdAt;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;}
    public Long getSessaoVersao(){return sessaoVersao == null ? 0L : sessaoVersao;}
    public void revogarSessoes(){sessaoVersao=getSessaoVersao()+1;}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
}
