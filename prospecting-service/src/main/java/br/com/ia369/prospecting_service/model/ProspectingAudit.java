package br.com.ia369.prospecting_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "prospecting_audit")
@Data
@NoArgsConstructor
public class ProspectingAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    @Column(length = 14)
    private String cnpj;

    /**
     * Valores possíveis: "Ok" ou "Error"
     */
    @Column(length = 10, nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String log;

    @PrePersist
    protected void onCreate() {
        if (this.dataEnvio == null) {
            this.dataEnvio = LocalDateTime.now();
        }
    }
}
