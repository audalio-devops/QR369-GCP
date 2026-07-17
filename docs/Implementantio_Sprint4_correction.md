# Correção: Cache-Aside Pattern para consulta de CNPJ via BrasilAPI

## Problema

Após a Sprint4, o `CnpjService.buscarPorCnpj()` consulta **apenas** a tabela `empresas` local  
(`empresaRepository.findById(cnpj)`). Quando o CNPJ não existe no banco, retorna `404`  
— sem nunca chamar a BrasilAPI. A lógica de fallback para a API externa foi removida.

**Fluxo atual (quebrado):**
```
GET /cnpj/{cnpj} → CnpjService → empresas (banco) → 404 se não existe
```

**Fluxo esperado (Cache-Aside):**
```
GET /cnpj/{cnpj}
  → encontrou no banco? → retorna
  → não encontrou?
      → chama BrasilAPI
      → persiste em empresas
      → retorna
```

## Proposed Changes

### 1. DTO para a resposta bruta da BrasilAPI

#### [NEW] BrasilApiCnpjDTO.java
`prospecting-service/src/main/java/br/com/ia369/prospecting_service/dto/BrasilApiCnpjDTO.java`

Record que mapeia os campos retornados pela BrasilAPI (`cnpj`, `razao_social`, `nome_fantasia`,
`situacao_cadastral`, `data_inicio_atividade`, `ddd_telefone_1`, `email`, `logradouro`,
`numero`, `complemento`, `bairro`, `municipio`, `uf`, `cep`). Usa `@JsonProperty` para
os nomes com underscores.

---

### 2. Cliente HTTP para a BrasilAPI

#### [NEW] BrasilApiClient.java
`prospecting-service/src/main/java/br/com/ia369\prospecting_service/client/BrasilApiClient.java`

Componente Spring que usa `RestClient` (já disponível em Spring Boot 3.3) para chamar
`https://brasilapi.com.br/api/cnpj/v1/{cnpj}`. Expõe método `buscarCnpj(String cnpj)` que
retorna `Optional<BrasilApiCnpjDTO>` — retorna `Optional.empty()` em caso de 404 ou erro.

---

### 3. Mapper Empresa ↔ BrasilApiCnpjDTO

#### [NEW] EmpresaMapper.java
`prospecting-service/src/main/java/br/com/ia369/prospecting_service/mapper/EmpresaMapper.java`

> [!NOTE]
> Já existe [EmpresaMapperTest.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/mapper/EmpresaMapperTest.java) nos testes — o mapper estava sendo previsto mas não foi
> criado. Este plano formaliza sua criação.

Classe utilitária estática com método `toEntity(BrasilApiCnpjDTO dto)` → [Empresa](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/Empresa.java#14-58).
Trata a separação de `ddd_telefone_1` (ex: `"11 99999-9999"`) em campos `ddd` e `telefone`.

---

### 4. Atualização do CnpjService (Cache-Aside)

#### [MODIFY] CnpjService.java
[prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/CnpjService.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/CnpjService.java)

1. Injetar `BrasilApiClient` e `EmpresaMapper` no construtor.
2. Alterar [buscarPorCnpj(String cnpj)](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/service/CnpjService.java#30-33) para implementar o Cache-Aside:
   - Busca em `empresaRepository.findById(cnpj)`.
   - Se encontrar → retorna `Optional<CNPJResponse>`.
   - Se não encontrar → chama `brasilApiClient.buscarCnpj(cnpj)`.
     - Se BrasilAPI retornar dados → mapeia para [Empresa](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/Empresa.java#14-58), persiste via `empresaRepository.save()`, retorna [CNPJResponse](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/dto/CNPJResponse.java#5-22).
     - Se BrasilAPI retornar vazio (404/erro) → retorna `Optional.empty()`.

---

### 5. application.yml — URL da BrasilAPI

#### [MODIFY] application.yml
[prospecting-service/src/main/resources/application.yml](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/application.yml)

Adicionar propriedade:
```yaml
brasilapi:
  base-url: https://brasilapi.com.br
```

---

## Verification Plan

### Testes Automatizados Existentes

Existem 3 arquivos de teste:
- [CnpjControllerTest.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/controller/CnpjControllerTest.java)
- [EmpresaMapperTest.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/mapper/EmpresaMapperTest.java)
- [CnpjServiceBatchTest.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/service/CnpjServiceBatchTest.java)

O [CnpjServiceBatchTest](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/service/CnpjServiceBatchTest.java#22-90) será atualizado para mockar também o `BrasilApiClient`.

#### Executar todos os testes:
```bash
cd c:\Projetos\IA369\QR369-GCP\prospecting-service
mvn test
```

### Novos Testes Unitários

Adicionar cenários em [CnpjServiceBatchTest.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/test/java/br/com/ia369/prospecting_service/service/CnpjServiceBatchTest.java) (ou novo `CnpjServiceTest.java`):
1. **Cache-hit**: CNPJ já existe no banco → não deve chamar BrasilAPI.
2. **Cache-miss, API found**: CNPJ não existe no banco, BrasilAPI retorna dados → deve salvar no banco e retornar.
3. **Cache-miss, API not found**: CNPJ não existe no banco, BrasilAPI retorna 404 → deve retornar `Optional.empty()`.

### Verificação Manual (Docker)

1. Subir ambiente:
   ```bash
   cd c:\Projetos\IA369\QR369-GCP
   docker compose up -d
   ```
2. Aguardar ~30s e testar via curl ou Postman:
   ```
   GET http://localhost:8081/cnpj/12234452000117
   ```
3. Resultado esperado: `200 OK` com JSON da empresa (dados vindos da BrasilAPI).
4. Chamar novamente — desta vez o retorno deve vir do banco (mesmo resultado, porém sem chamar a API).
5. Verificar logs do container:
   ```bash
   docker compose logs prospecting-service -f
   ```
   Primeira chamada: log `"Buscando CNPJ na BrasilAPI: 12234452000117"`.  
   Segunda chamada: log `"CNPJ encontrado no cache local"`.
