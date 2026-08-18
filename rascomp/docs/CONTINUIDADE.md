# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-17T23:20:00-03:00

## 1. Objetivo e prazo

Plataforma para gestão de competições de robôs da RAS-UFRB, com backend Spring Boot, inscrições, equipes, robôs, categorias, resultados, chaveamentos e integração futura com Camunda.

Meta imediata: concluir o projeto base com **backend + dois frontends até 30/08/2026**. A prioridade é fechar o contrato funcional do backend e ir rapidamente para os frontends.

## 2. Stack consolidada

- Java 21
- Spring Boot 3
- Spring Data JPA / Hibernate
- Jakarta Validation
- Lombok
- MySQL como banco persistente principal
- Flyway para migrations
- Camunda 7 embarcado
- Swagger / OpenAPI
- Package root: `br.edu.ufrb.rascomp`

Persistência definitiva:

```text
Spring Boot
    -> Spring Data JPA / Hibernate
    -> JDBC
    -> MySQL
```

Já aplicado:

- `mysql-connector-j`;
- `flyway-mysql`;
- MySQL configurado no `application.properties`;
- `ddl-auto=validate`;
- Flyway habilitado;
- `V1__create_rascomp_schema.sql` com schema base;
- `V2__create_inspecoes_sumo.sql` para inspeções do Sumô;
- tabela `Institution` normalizada para `institutions`.

Configuração padrão:

```text
DB_URL=jdbc:mysql://localhost:3306/rascomp?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bahia
DB_USERNAME=root
DB_PASSWORD=
```

## 3. Estratégia de desenvolvimento e testes

Os testes completos serão executados depois do fechamento das regras avançadas restantes para evitar retrabalho durante a corrida para os frontends.

Arquivo de referência dos testes:

`docs/TESTES_POSTMAN.md`

Arquivo local não versionado de referência de código:

`docs/CODIGOS_REFERENCIA.md`

## 4. Status funcional

### CRUDs principais

Status: **implementados**

- `CompetitionCategory`
- `ConfigSumo`
- `ConfigFollow`
- `Institution`
- `Team`
- `Competitor`
- `Robot`
- `Competition`
- `Registration`
- `TentativaSeguidorLinha`
- `Bracket`
- `Match`
- `MatchResult`

### Tratamento global de exceções

Status: **implementação/conferência final pendente na bateria de testes**.

### ETAPA A — ConfigFollow validando TentativaSeguidorLinha

Status: **implementado — testes finais pendentes**.

Regras:

- limites de tomadas;
- tentativas por tomada;
- checkpoints;
- tempo máximo;
- tentativa acima do tempo permanece registrada, mas é inválida.

### ETAPA B — RankingFollowService

Status: **implementado — testes finais pendentes**.

Regras:

- ranking por competição/categoria;
- somente inscrições ativas e aprovadas;
- somente tentativas válidas e concluídas;
- tempo final = tempo bruto + penalidade;
- melhor tentativa de cada inscrição;
- desempate por tempo final, tempo bruto e `registrationId`.

Endpoint:

```text
GET /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}
```

### ETAPA C — geração automática da primeira rodada

Status: **implementado — testes finais pendentes**.

Endpoint:

```text
POST /api/v1/chaveamentos/gerar?competitionId={id}&categoryId={id}
```

Regras:

- somente inscrições ativas e `APROVADA`;
- mínimo de 2 participantes;
- próxima potência de 2 define o tamanho da chave;
- quantidade de BYEs = tamanho da chave - quantidade de participantes;
- participantes são embaralhados antes da montagem, sorteando também quem recebe BYE;
- BYE é ausência de adversário na chave, diferente de W.O. futuro.

### ETAPA D — árvore completa do chaveamento

Status: **implementado — testes finais pendentes**.

- todas as rodadas até a final são criadas de uma vez;
- rodadas futuras começam `AGUARDANDO_PARTICIPANTES`;
- estrutura pronta para receber vencedores automaticamente.

### ETAPA E — avanço automático de vencedor + BYE

Status: **implementado — testes finais pendentes**.

Novo serviço:

- `BracketProgressionService`.

Regras:

- partida de ordem ímpar alimenta `registrationA` da próxima partida;
- partida de ordem par alimenta `registrationB`;
- próxima ordem calculada por `(ordem + 1) / 2`;
- quando somente um slot está preenchido, status permanece `AGUARDANDO_PARTICIPANTES`;
- quando ambos os slots são preenchidos, status passa para `AGENDADA`;
- `MatchResultService` passou a avançar automaticamente o vencedor após registrar resultado;
- partidas `BYE` não recebem resultado manual;
- BYEs são finalizados e avançados automaticamente durante a geração do chaveamento;
- participantes da primeira rodada são sorteados com `Collections.shuffle`;
- ao finalizar a última partida, o `Bracket` passa para `FINALIZADO`;
- alteração de participante é bloqueada se a próxima partida já estiver iniciada/finalizada.

### ETAPA F — inspeção do Sumô

Status: **implementado — testes finais pendentes**.

Arquivos adicionados:

- `InspecaoSumo`;
- `InspecaoSumoDTO`;
- `InspecaoSumoRepository`;
- `InspecaoSumoService`;
- `InspecaoSumoController`;
- migration `V2__create_inspecoes_sumo.sql`.

Novo status de inscrição:

```text
DESCLASSIFICADA
```

Regras:

- inspeção vinculada à `Registration`;
- somente inscrição ativa, aprovada e de categoria `SUMO` pode ser inspecionada;
- configuração é obtida de `ConfigSumo`;
- número da tentativa de inspeção é calculado pelo backend;
- peso aprovado quando `pesoMedido <= pesoMax`;
- bloqueia novas inspeções depois de uma aprovação;
- respeita `maxTentativasInspecao`;
- se a última tentativa permitida falhar, a inscrição passa para `DESCLASSIFICADA`;
- se `exigeInspecao=false`, a inscrição aprovada é considerada apta mesmo sem inspeção;
- se `exigeInspecao=true`, precisa existir inspeção aprovada.

Endpoints:

```text
POST /api/v1/inspecoes-sumo
GET  /api/v1/inspecoes-sumo/{id}
GET  /api/v1/inspecoes-sumo/por-inscricao?registrationId={id}
GET  /api/v1/inspecoes-sumo/ultima?registrationId={id}
GET  /api/v1/inspecoes-sumo/aptidao?registrationId={id}
```

## 5. Próxima regra — ETAPA G

Próxima implementação prioritária:

**Rounds do Sumô**.

Modelo esperado:

- `RoundSumo` relacionado a `Match`;
- número do round;
- vencedor opcional;
- motivo/status do round;
- rounds regulares definidos por `ConfigSumo.numeroRounds`;
- vitórias necessárias definidas por `roundsParaVencer`;
- round adicional quando permitido e necessário;
- somente participantes aptos pela inspeção podem competir;
- ao atingir vitórias necessárias, gerar/consolidar `MatchResult` e usar a ETAPA E para avançar o vencedor.

## 6. Prioridade até 30/08/2026

1. **ETAPA G** — rounds do Sumô + consolidação do resultado.
2. Revisar endpoints necessários pelos dois frontends.
3. Validar MySQL + Flyway localmente.
4. Executar bateria essencial de `docs/TESTES_POSTMAN.md`.
5. Congelar contrato da API base.
6. Iniciar imediatamente os dois frontends.
7. JWT, testes automatizados adicionais e refinamentos que não bloqueiem o frontend ficam depois do contrato principal.

## 7. Histórico resumido

- 2026-07-28 — planejamento inicial do backend.
- 2026-08-03 a 2026-08-06 — CRUDs principais implementados.
- 2026-08-10 — aplicação validada com H2, JPA, Camunda e DataInitializer.
- 2026-08-17 — ETAPA A implementada: ConfigFollow aplicado às tentativas.
- 2026-08-17 — ETAPA B implementada: ranking do Seguidor de Linha.
- 2026-08-17 — persistência migrada para MySQL + Flyway.
- 2026-08-17 — ETAPA C implementada: geração automática da primeira rodada.
- 2026-08-17 — ETAPA D implementada: árvore completa do chaveamento.
- 2026-08-17 — ETAPA E implementada: sorteio de BYEs e avanço automático de vencedores.
- 2026-08-17 — ETAPA F implementada: inspeção do Sumô com limite de tentativas e desclassificação.
