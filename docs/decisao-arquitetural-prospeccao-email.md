# Decisão arquitetural: prospecção por e-mail

## Contexto

O `prospecting-service` atual executa um fluxo voltado à prospecção de contadores pelo WhatsApp. Ele combina obtenção e enriquecimento de dados empresariais, agendamento e processamento, seleção de mensagens e integração com a Z-API. A implantação atual também configura esse serviço como aplicação própria no `docker-compose.yml`.

A nova demanda prevê e-mails com conteúdo diferente, anexos e outro público-alvo. Portanto, ela introduz diferenças tanto no canal de entrega quanto nas regras da campanha e nos dados de destinatários.

## Recomendação

**Evoluir o `prospecting-service` como um serviço de campanhas multicanal, mantendo os fluxos de WhatsApp e e-mail separados por módulos e implementações de canal.** Não criar agora um microserviço independente chamado `prospecting-mailing-service`.

O serviço deve deixar de tratar detalhes da Z-API como se fossem a própria lógica de prospecção. Uma camada de campanha deve cuidar das regras compartilhadas que realmente existirem, como execução, estado e rastreabilidade. Implementações específicas de canal cuidariam de entrega e conteúdo: WhatsApp/Z-API de um lado e e-mail, incluindo anexos, de outro. Cada campanha deve poder definir seu público, mensagem, agenda e parâmetros próprios.

Essa recomendação mantém um único serviço operacional enquanto há apenas dois fluxos conhecidos, mas cria limites internos que permitem extrair o módulo de e-mail depois, se ele passar a exigir operação independente. Os dados e regras específicos da nova audiência não devem ser forçados a caber no modelo atual de CNPJ/telefone ou nas mensagens existentes.

## Motivos

### Por que não misturar o envio de e-mail diretamente ao fluxo WhatsApp

- A Z-API e um provedor de e-mail têm autenticação, limites, erros e respostas diferentes. Colocar essas particularidades no mesmo fluxo dificulta manutenção e tratamento de falhas.
- Anexos exigem validação, armazenamento ou leitura de arquivos e controle de tamanho e tipo, preocupações que não existem no envio atual de texto pelo WhatsApp.
- Público e conteúdo diferentes tendem a gerar critérios próprios de seleção, opt-out, agenda e acompanhamento. Compartilhar essas regras sem necessidade cria acoplamento e pode levar a campanhas incorretas.
- Uma abstração interna de canal preserva a possibilidade de compartilhar orquestração sem fazer as integrações dependerem uma da outra.

### Por que não criar um novo microserviço agora

- A demanda descreve um novo fluxo, mas ainda não demonstra necessidade de implantação, escala, disponibilidade ou ciclo de mudanças independentes.
- Um serviço separado acrescentaria configuração, monitoramento, deploy, comunicação entre serviços, gestão de credenciais e decisões sobre persistência e consistência dos dados.
- Se ambos os serviços precisarem consultar a mesma base de empresas, filas ou histórico, a separação pode criar dependência distribuída antes de haver benefício operacional claro.
- Se o serviço novo receber também a responsabilidade de selecionar destinatários e coordenar campanhas, podem surgir duas implementações de auditoria, agendamento e controle de duplicidade.

## Alternativas consideradas

| Alternativa | Avaliação |
| --- | --- |
| Acrescentar o envio de e-mail diretamente às classes atuais de WhatsApp | Não recomendado. Mistura provedores, formatos e regras de audiência no mesmo fluxo. |
| Criar já o `prospecting-mailing-service` | Não recomendado como primeiro passo. Isola o canal, mas aumenta o custo operacional e pode duplicar responsabilidades compartilhadas sem evidência de que a implantação independente seja necessária. |
| Manter um serviço de campanhas com módulos de canal isolados | **Recomendado.** Evita duplicação operacional agora e permite que as regras e integrações de e-mail evoluam sem invadir o fluxo WhatsApp. |

## Diretrizes para a evolução

1. Separar a coordenação de campanhas das integrações de entrega. O fluxo de WhatsApp deve continuar encapsulando a Z-API; o fluxo de e-mail deve encapsular o provedor escolhido e o tratamento dos anexos.
2. Modelar audiência, conteúdo, agenda e canal por campanha. Reutilizar entidades ou regras somente quando representarem conceitos realmente comuns.
3. Registrar tentativas e resultados com identificação da campanha, destinatário, canal, horário e estado. Evitar depender de campos específicos de CNPJ para representar todo destinatário; usar identificador de destinatário apropriado ao público de cada canal.
4. Prever idempotência e tratamento de falhas por destinatário para que repetição de uma execução não dispare envios duplicados.
5. Definir como serão tratados descadastro/opt-out, validade e origem dos contatos, acesso a anexos e retenção dos registros antes de operar campanhas reais. Essas regras podem variar conforme público e canal.
6. Manter credenciais do provedor e configurações de envio fora do código, usando os mecanismos de configuração e segredos do ambiente.

## Quando reconsiderar a extração para outro serviço

Extrair um `prospecting-mailing-service` passa a fazer sentido se houver sinais concretos, por exemplo:

- necessidade de escalar ou implantar o envio de e-mail independentemente do WhatsApp;
- disponibilidade, limites de taxa ou janelas de execução que exijam isolamento operacional;
- evolução por uma equipe ou ciclo de release independente;
- volume ou processamento de anexos que justifique recursos e monitoramento próprios;
- integração com outros produtos que consumam uma capacidade de envio de e-mail reutilizável.

Se essa extração ocorrer, deve-se definir explicitamente quem é dono das campanhas e dos dados dos destinatários, bem como a API ou fila entre os serviços. Evitar que os dois serviços escrevam diretamente nas mesmas tabelas como forma de integração.

## Conclusão

Para a necessidade apresentada, a escolha mais equilibrada é manter um único `prospecting-service` por enquanto e reorganizar a responsabilidade de envio em módulos de canal bem delimitados. Isso acomoda o e-mail sem acoplar anexos, provedor e público novo às regras atuais da Z-API, e mantém aberta a extração futura quando requisitos operacionais justificarem um serviço independente.
