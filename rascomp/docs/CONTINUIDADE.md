# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-17T23:04:00-03:00

## 1. Objetivo

Plataforma para gestão de competições de robôs da RAS-UFRB, com backend Spring Boot, inscrições, equipes, robôs, categorias, resultados, chaveamentos e integração futura com Camunda.

## 2. Stack e convenções

- Java 21
- Spring Boot 3
- Spring Data JPA
- Jakarta Validation
- Lombok
- H2 durante o desenvolvimento
- banco persistente da execução real em reavaliação (`MySQL` ou `PostgreSQL`)
- Package root: `br.edu.ufrb.rascomp`
- Estrutura por camadas: `controller`, `dto`, `model`, `repository`, `service`, `exception`
- relacionamentos carregados com `FetchType.LAZY` por padrão;
- exclusão lógica para entidades principais;
- exclusão física apenas para registros dependentes sem histórico próprio, quando adequado;
- testes e validações só são marcados como concluídos após execução local.

### Decisão de persistência em reavaliação

O uso de PostgreSQL deixou de ser tratado como requisito fechado.

O Rascomp é um sistema de escopo contido para operação durante competições, com volume de dados relativamente pequeno e sem necessidade atual de recursos específicos do PostgreSQL.

Direção recomendada para avaliação final:

- manter `Spring Data JPA/Hibernate` como camada de persistência;
- manter H2 para desenvolvimento/testes rápidos;
- considerar `MySQL` como banco persistente da execução real por simplicidade operacional e familiaridade;
- manter PostgreSQL como alternativa válida caso surja necessidade de infraestrutura já padronizada nele;
- não substituir JPA por JDBC puro apenas para trocar o banco: JDBC é a API de acesso ao banco, não um banco de dados, e aumentaria código manual sem benefício claro para o domínio atual;
- Flyway continua útil independentemente da escolha entre MySQL e PostgreSQL quando o schema deixar de ser recriado automaticamente.

A troca definitiva do driver/configuração será feita somente após as regras de domínio e os testes finais.

## 3. Estratégia de desenvolvimento

O mantenedor implementa e realiza testes locais.

O assistente orienta, revisa e, quando solicitado, implementa diretamente no repositório.

O arquivo local de referência permanece:

`docs/CODIGOS_REFERENCIA.md`

Esse arquivo deve permanecer no `.gitignore` e não deve ser versionado.

Foi criado também o arquivo versionado:

`docs/TESTES_POSTMAN.md`

Ele concentra:

- endpoints atuais;
- exemplos de bodies JSON;
- ordem recomendada de testes;
- testes positivos e negativos;
- validações do tratamento global de exceções;
- testes das regras de domínio já implementadas;
- seções reservadas para geração de chaveamento, avanço de vencedores e regras do Sumô.

Decisão atual: os testes completos serão executados ao final das implementações avançadas, evitando repetir a bateria manual a cada pequena alteração.

## 4. Status atual

### Projeto base

Status: **concluído**

- estrutura inicial criada;
- H2 configurado como banco de desenvolvimento;
- DataInitializer expandido com cenário integrado para testes;
- aplicação já subiu corretamente com os relacionamentos JPA e dados de teste;
- Swagger/OpenAPI disponível no projeto;
- escolha do banco persistente definitivo ficou para depois das regras de domínio;
- Docker, JWT e configuração final de migrations permanecem para etapas posteriores.

### CRUDs principais

Status: **implementados e smoke tests manuais iniciais executados**

Módulos:

- `CompetitionCategory`, `ConfigSumo`, `ConfigFollow`;
- `Institution`;
- `Team`;
- `Competitor`;
- `Robot`;
- `Competition`;
- `Registration`;
- `TentativaSeguidorLinha`;
- `Bracket`;
- `Match`;
- `MatchResult`.

Decisões principais mantidas:

- `Robot` não possui modalidade/categoria diretamente;
- `Registration` liga competição, categoria, equipe e robô;
- `Bracket` representa o chaveamento de uma categoria dentro de uma competição;
- `MatchResult` permanece separado de `Match`;
- partidas aceitam participante nulo para suportar `BYE`.

### Tratamento global de exceções

Status: **implementação prevista/conferência final pendente**

Objetivo:

- substituir respostas genéricas do Spring por erros padronizados para recurso inexistente, regra de negócio, validação, parâmetros inválidos e conflitos de integridade.

Os casos de teste correspondentes foram registrados em `docs/TESTES_POSTMAN.md` para execução na bateria final.

### ETAPA A — `TentativaSeguidorLinha` + `ConfigFollow`

Status: **implementado — testes finais pendentes**

Regras aplicadas no `TentativaSeguidorLinhaService`:

- busca obrigatória do `ConfigFollow` da categoria da inscrição;
- tomada deve estar entre 1 e `numeroTomadas`;
- número da tentativa deve estar entre 1 e `tentativasPorTomada`;
- checkpoints devem estar entre 0 e `numeroCheckpoints`;
- tentativa acima de `maxTempoSegundos` continua registrada, mas é marcada automaticamente como inválida;
- as mesmas regras são aplicadas na criação e atualização;
- validade final deixa de depender somente do valor enviado pelo cliente.

### ETAPA B — Ranking do Seguidor de Linha

Status: **implementado — testes finais pendentes**

Arquivos:

- `RankingFollowDTO`;
- `RankingFollowService`;
- `RankingFollowController`.

Regras da primeira versão:

- ranking por competição e categoria;
- categoria deve ser ativa e `FOLLOW_LINE`;
- considera somente inscrições ativas e aprovadas;
- considera somente tentativas válidas, concluídas e com tempo registrado;
- tempo final = tempo bruto + penalidade;
- seleciona a melhor tentativa de cada inscrição;
- ordena por menor tempo final;
- em empate, usa menor tempo bruto e depois `registrationId`;
- atribui posição sequencial;
- endpoint: `GET /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}`.

### `Bracket`, `Match` e `MatchResult`

Status: **CRUD implementado; automações pendentes**

Ainda faltam:

- geração automática do chaveamento;
- geração das rodadas futuras;
- avanço automático de vencedores;
- tratamento automático de `BYE`;
- regras específicas de Sumô.

### Sumô

Status: **regras avançadas pendentes**

Próxima modelagem esperada:

- inspeção do robô por inscrição;
- limite de tentativas de inspeção;
- peso máximo conforme `ConfigSumo`;
- rounds por partida;
- consolidação automática do vencedor;
- desclassificação, ausência e desempate.

## 5. Estratégia de testes

Os testes completos foram deliberadamente movidos para o final das regras avançadas.

Arquivo principal:

`docs/TESTES_POSTMAN.md`

A bateria final deverá validar:

- CRUDs;
- filtros e consultas por relacionamento;
- soft delete e reativação;
- validações de DTO;
- tratamento global de exceções;
- regras de `ConfigFollow`;
- ranking do Seguidor de Linha;
- geração de chaveamento;
- avanço de vencedores e BYE;
- inspeção e rounds do Sumô;
- persistência no banco escolhido para uso real.

## 6. Próximas etapas

1. Implementar geração automática de chaveamento — primeira rodada.
2. Evoluir geração para a árvore completa de rodadas.
3. Implementar avanço automático de vencedores.
4. Implementar tratamento automático de `BYE`.
5. Modelar e implementar inspeção do Sumô.
6. Modelar e implementar rounds do Sumô e consolidação automática do resultado.
7. Revisar tratamento global de exceções no estado final da API.
8. Executar a bateria completa de `docs/TESTES_POSTMAN.md`.
9. Criar testes automatizados para as regras críticas.
10. Definir banco persistente final (`MySQL` recomendado para avaliação ou PostgreSQL se houver motivo operacional).
11. Configurar migrations do banco escolhido com Flyway.
12. Integrar Camunda apenas aos fluxos que realmente precisarem de processo/orquestração.
13. Implementar segurança JWT.
14. Preparar estratégia final de execução/deploy da competição.

## 7. Histórico resumido

- 2026-07-28 — Documento inicial e planejamento do backend.
- 2026-07-29 — Projeto renomeado para Rascomp e package root ajustado.
- 2026-08-03 — CRUD `CompetitionCategory` implementado.
- 2026-08-04 — `ConfigSumo`, `ConfigFollow` e `TentativaSeguidorLinha` planejados.
- 2026-08-05 — CRUD `Institution` concluído.
- 2026-08-06 — CRUDs `Team`, `Competitor` e `Robot` implementados.
- 2026-08-06 — CRUDs `Competition`, `Registration`, `TentativaSeguidorLinha`, `Bracket`, `Match` e `MatchResult` implementados.
- 2026-08-10 — aplicação validada subindo com H2, JPA, Camunda e DataInitializer.
- 2026-08-17 — `ConfigFollow` passou a validar `TentativaSeguidorLinha`.
- 2026-08-17 — primeira versão do ranking do Seguidor de Linha implementada.
- 2026-08-17 — bateria final de endpoints/testes consolidada em `docs/TESTES_POSTMAN.md`.
- 2026-08-17 — banco persistente definitivo passou de PostgreSQL obrigatório para decisão em reavaliação, com MySQL como alternativa recomendada para o escopo atual.
