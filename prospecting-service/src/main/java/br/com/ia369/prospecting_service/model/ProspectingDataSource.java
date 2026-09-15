package br.com.ia369.prospecting_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prospecting_data_source")
@Data
@NoArgsConstructor
public class ProspectingDataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 14)
    private String cnpj;

    @Column(name = "razao_social", length = 255)
    private String razaoSocial;

    @Column(length = 20)
    private String telefone1;

    @Column(length = 20)
    private String telefone2;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String status;
}
