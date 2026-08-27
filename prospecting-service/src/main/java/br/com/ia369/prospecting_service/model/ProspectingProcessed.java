package br.com.ia369.prospecting_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "prospecting_processed")
@Data
@NoArgsConstructor
public class ProspectingProcessed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 14, nullable = false)
    private String cnpj;

    @Column(length = 255)
    private String email;

    @Column(name = "razao_social", length = 255)
    private String razaoSocial;

    @Column(name = "telefone_valido", length = 20)
    private String telefoneValido;

    @Column(name = "data_contato")
    private LocalDateTime dataContato;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @Column(length = 100)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
