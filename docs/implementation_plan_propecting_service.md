# Migração: Férias → CNPJ no `prospecting-service`

## Objetivo

Substituir a funcionalidade de consulta de férias (banco H2 em memória, entidade [Ferias](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/Ferias.java#13-81)) por uma funcionalidade de consulta e cadastro de CNPJs a serem prospectados. O serviço deve expor o endpoint `GET /cnpj/{cnpj}` — que é exatamente o que [CNPJTools.java](file:///c:/Projetos/IA369/QR369-GCP/virtual-assistant/src/main/java/br/com/ia369/virtual_assistant/cnpj/CNPJTools.java) do `virtual-assistant` já chama.

---

## Visão Geral do Estado Atual vs. Desejado

| Camada | Hoje ([Ferias](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/Ferias.java#13-81)) | Após migração ([CNPJ](file:///c:/Projetos/IA369/QR369-GCP/virtual-assistant/src/main/java/br/com/ia369/virtual_assistant/cnpj/CNPJTools.java#18-65)) |
|---|---|---|
| Entidade JPA | [Ferias](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/Ferias.java#13-81) → `tb_ferias` | `Cnpj` → `tb_cnpj` |
| Repositório | [FeriasRepository](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/repository/FeriasRepository.java#8-13) | `CnpjRepository` |
| DTO de resposta | [FeriasResponse](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/dto/FeriasResponse.java#7-20) | [CNPJResponse](file:///c:/Projetos/IA369/QR369-GCP/virtual-assistant/src/main/java/br/com/ia369/virtual_assistant/cnpj/CNPJResponse.java#5-22) (já existe!) |
| Controller | [FeriasController](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/controller/FeriasController.java#13-39) → `GET /ferias/{matricula}` | `CnpjController` → `GET /cnpj/{cnpj}` |
| Seed de dados | [DataSeeder](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/config/DataSeeder.java#14-40) (férias hardcoded) | `CnpjDataSeeder` (CNPJs de exemplo) |
| Banco | H2 in-memory | **H2 mantido** (simples, não exige infra extra) |

---

## Proposed Changes

### Limpeza — arquivos a remover

#### [DELETE] [Ferias.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/entity/Ferias.java)
#### [DELETE] [FeriasRepository.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/repository/FeriasRepository.java)
#### [DELETE] [FeriasController.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/controller/FeriasController.java)
#### [DELETE] [FeriasResponse.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/dto/FeriasResponse.java)
#### [DELETE] [DataSeeder.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/config/DataSeeder.java)

---

### Camada de domínio

#### [MODIFY] [CNPJResponse.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/dto/CNPJResponse.java)

Já existe e já tem os campos corretos. Nenhuma alteração necessária.

#### [NEW] `entity/Cnpj.java`

Entidade JPA mapeando a tabela `tb_cnpj`. Campos espelham [CNPJResponse](file:///c:/Projetos/IA369/QR369-GCP/virtual-assistant/src/main/java/br/com/ia369/virtual_assistant/cnpj/CNPJResponse.java#5-22):

```java
@Entity
@Table(name = "tb_cnpj")
public class Cnpj {
    @Id
    @Column(unique = true, nullable = false, length = 14)
    private String cnpj;         // PK natural — o próprio número de CNPJ

    private String razaoSocial;
    private String nomeFantasia;
    private String situacaoCadastral;
    private LocalDate dataAbertura;
    private String ddd;
    private String telefone;
    private String email;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String cep;
    // getters/setters ou usar Java Records não é possível aqui (JPA precisa de classe mutável)
}
```

> [!NOTE]
> O CNPJ é usado como chave primária natural (string de 14 dígitos), evitando uma coluna `id` desnecessária.

---

### Camada de repositório

#### [NEW] `repository/CnpjRepository.java`

```java
public interface CnpjRepository extends JpaRepository<Cnpj, String> {
    // findById(cnpj) já é fornecido pelo JpaRepository — nenhum método extra necessário.
}
```

---

### Camada de serviço (opcional, recomendado)

#### [NEW] `service/CnpjService.java`

Encapsula a lógica de negócio, deixando o controller fino:

```java
@Service
public class CnpjService {
    private final CnpjRepository repository;

    public Optional<CNPJResponse> buscarPorCnpj(String cnpj) {
        return repository.findById(cnpj).map(this::toResponse);
    }

    private CNPJResponse toResponse(Cnpj c) {
        return new CNPJResponse(c.getCnpj(), c.getRazaoSocial(), ...);
    }
}
```

---

### Camada de controller

#### [DELETE] [FeriasController.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/controller/FeriasController.java)

#### [NEW] `controller/CnpjController.java`

```java
@RestController
@RequestMapping("/cnpj")
public class CnpjController {

    @GetMapping("/{cnpj}")
    public ResponseEntity<CNPJResponse> buscarPorCnpj(@PathVariable String cnpj) {
        return cnpjService.buscarPorCnpj(cnpj)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

> [!IMPORTANT]
> O path `/cnpj/{cnpj}` é o exato contrato que [CNPJTools.java](file:///c:/Projetos/IA369/QR369-GCP/virtual-assistant/src/main/java/br/com/ia369/virtual_assistant/cnpj/CNPJTools.java) do `virtual-assistant` já chama:
> `.uri("/cnpj/{cnpj}", cnpj)`. Não alterar esse path.

---

### Seed de dados

#### [MODIFY] [DataSeeder.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/config/DataSeeder.java)

Renomear para `CnpjDataSeeder.java` ou modificar [DataSeeder.java](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/java/br/com/ia369/prospecting_service/config/DataSeeder.java) para semear CNPJs de exemplo:

```java
@Component
public class CnpjDataSeeder implements CommandLineRunner {
    public void run(String... args) {
        repository.saveAll(List.of(
            new Cnpj("60701190000104", "BRADESCO S.A.", "BRADESCO", "ATIVA",
                     LocalDate.of(1943, 3, 10), "11", "37350000", "contato@bradesco.com.br",
                     "Cidade de Deus", "s/n", "", "Vila Yara", "Osasco", "SP", "06029-900"),
            // outros CNPJs de exemplo...
        ));
        log.info("Seed concluido: {} CNPJs inseridos", 1);
    }
}
```

---

### Configuração

#### [MODIFY] [application.yml](file:///c:/Projetos/IA369/QR369-GCP/prospecting-service/src/main/resources/application.yml)

Manter H2, apenas nomear o banco mais semanticamente e desabilitar console fora do perfil `dev`:

```yaml
server:
  port: ${PS_SERVER_PORT:8081}

spring:
  application:
    name: prospecting-service       # nome mais descritivo
  datasource:
    url: jdbc:h2:mem:cnpjdb         # banco renomeado
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: false                # desabilitado por padrão (segurança)
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

> [!NOTE]
> O [pom.xml](file:///c:/Projetos/IA369/QR369-GCP/pom.xml) **não precisa de alterações** — as dependências `spring-boot-starter-data-jpa` e `h2` já cobrem tudo o que precisamos.

---

## Verification Plan

### Teste Automático via Maven

Como não há testes automatizados no projeto, criaremos um teste de integração básico:

#### [NEW] `src/test/java/.../CnpjControllerTest.java`

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class CnpjControllerTest {

    @Test
    void deveRetornar200QuandoCnpjExiste() { ... }

    @Test
    void deveRetornar404QuandoCnpjNaoExiste() { ... }
}
```

**Comando para rodar:**
```bash
cd c:\Projetos\IA369\QR369-GCP\prospecting-service
.\mvnw test
```

---

### Verificação Manual

1. **Subir o serviço:**
   ```bash
   cd c:\Projetos\IA369\QR369-GCP\prospecting-service
   .\mvnw spring-boot:run
   ```

2. **CNPJ existente (deve retornar 200):**
   ```
   GET http://localhost:8081/cnpj/60701190000104
   ```
   Resposta esperada: JSON com `razaoSocial`, `cidade`, etc.

3. **CNPJ inexistente (deve retornar 404):**
   ```
   GET http://localhost:8081/cnpj/00000000000000
   ```
   Resposta esperada: `404 Not Found` — sem body.

4. **Verificar integração com `virtual-assistant`** (se ambos estiverem rodando):
   - Enviar ao assistente a mensagem: *"Consulte o CNPJ 60701190000104"*
   - Ele deve invocar `CNPJTools.consultarCNPJ()` e retornar os dados da empresa.

---

## Ordem de Execução Sugerida

```
1. Deletar: Ferias.java, FeriasRepository.java, FeriasController.java, FeriasResponse.java, DataSeeder.java
2. Criar:  entity/Cnpj.java
3. Criar:  repository/CnpjRepository.java
4. Criar:  service/CnpjService.java
5. Criar:  controller/CnpjController.java
6. Modificar: DataSeeder → CnpjDataSeeder.java
7. Modificar: application.yml (nome do banco, console off)
8. Criar:  CnpjControllerTest.java
9. Rodar:  mvnw test
10. Testar: GET /cnpj/{cnpj} manualmente
```
