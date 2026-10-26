package sr.model;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="auditoria_usuario")
public class AuditoriaUsuario {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="usuario_executor_id") private Long executorId;
 @Column(name="usuario_afetado_id") private Long afetadoId;
 @Column(nullable=false,length=50) private String acao;
 @Column(name="data_hora",nullable=false) private OffsetDateTime dataHora;
 @Column(length=500) private String detalhes;
 @PrePersist public void iniciar(){if(dataHora==null)dataHora=OffsetDateTime.now();}
 public Long getId(){return id;} public Long getExecutorId(){return executorId;}
 public void setExecutorId(Long x){executorId=x;} public Long getAfetadoId(){return afetadoId;}
 public void setAfetadoId(Long x){afetadoId=x;} public String getAcao(){return acao;}
 public void setAcao(String x){acao=x;} public OffsetDateTime getDataHora(){return dataHora;}
 public String getDetalhes(){return detalhes;} public void setDetalhes(String x){detalhes=x;}
}
