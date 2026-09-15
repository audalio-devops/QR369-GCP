package br.com.ia369.prospecting_service.service;

import br.com.ia369.prospecting_service.dto.ImportacaoResponse;
import br.com.ia369.prospecting_service.model.ProspectingDataSource;
import br.com.ia369.prospecting_service.repository.ProspectingDataSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportacaoService {

    @Autowired
    private ProspectingDataSourceRepository repository;

    @Autowired
    private ResourceLoader resourceLoader;

    public ImportacaoResponse processarArquivo(int tipo) {
        String filePath = (tipo == 1) ? "classpath:data_sources/ds_cnpj.csv" : "classpath:data_sources/ds_razao_social.csv";
        Resource resource = resourceLoader.getResource(filePath);
        List<ProspectingDataSource> dataSources = new ArrayList<>();
        long registrosImportados = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            reader.readLine(); // Pula o cabeçalho

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Pula linhas em branco
                }

                // Usar -1 para garantir que colunas vazias no final sejam mantidas
                String[] fields = line.split(",", -1);
                ProspectingDataSource dataSource = new ProspectingDataSource();

                if (tipo == 1) {
                    // Importa se tiver pelo menos um CNPJ
                    if (fields.length > 0 && fields[0] != null && !fields[0].trim().isEmpty()) {
                        dataSource.setCnpj(fields[0].trim());

                        if (fields.length > 1) {
                            dataSource.setRazaoSocial(fields[1].trim());
                        }

                        if (fields.length > 3) {
                            String ddd1 = fields[2].trim();
                            String telefone1 = fields[3].trim();
                            if (!ddd1.isEmpty() && !telefone1.isEmpty()) {
                                dataSource.setTelefone1("55" + ddd1 + telefone1);
                            }
                        }

                        if (fields.length > 5) {
                            String ddd2 = fields[4].trim();
                            String telefone2 = fields[5].trim();
                            if (!ddd2.isEmpty() && !telefone2.isEmpty()) {
                                dataSource.setTelefone2("55" + ddd2 + telefone2);
                            }
                        }

                        if (fields.length > 6) {
                            dataSource.setEmail(fields[6].trim());
                        }
                        
                        dataSources.add(dataSource);
                    }
                } else if (tipo == 2) {
                    // Importa se tiver pelo menos a Razão Social
                    if (fields.length > 0 && fields[0] != null && !fields[0].trim().isEmpty()) {
                        dataSource.setRazaoSocial(fields[0].trim());
                        if (fields.length > 1) {
                            dataSource.setTelefone1(fields[1].trim());
                        }
                        dataSources.add(dataSource);
                    }
                }
            }

            if (!dataSources.isEmpty()) {
                repository.saveAll(dataSources);
                registrosImportados = dataSources.size();
            }

            return new ImportacaoResponse("Sucesso", registrosImportados, "Importação concluída.");

        } catch (Exception e) {
            e.printStackTrace();
            return new ImportacaoResponse("Erro", 0, "Falha ao processar o arquivo: " + e.getMessage());
        }
    }
}
