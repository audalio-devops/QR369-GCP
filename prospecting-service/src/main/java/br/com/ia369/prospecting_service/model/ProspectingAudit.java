package br.com.ia369.prospecting_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "prospecting_audit")
@Data
@NoArgsConstructor
public class ProspectingAudit {

    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(length = 14)
    private String cnpj;

    /**
     * Valores possíveis: "Iniciado", "Erro", "Finalizado", "Funcionando", "Parado",
     * "Ok",
     * "Error"
     */
    @Column(length = 50, nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String log;

    @PrePersist
    protected void onCreate() {
        if (this.dataEvento == null) {
            this.dataEvento = LocalDateTime.now(ZONE_SP);
        }
        if (this.dataEnvio == null) {
            this.dataEnvio = this.dataEvento;
        }
    }
}
