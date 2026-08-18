# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-18T10:03:00-03:00

## 1. Objetivo e prazo

Plataforma para gestão de competições de robôs da RAS-UFRB, com backend Spring Boot, inscrições, equipes, robôs, categorias, resultados, chaveamentos e integração futura com Camunda.

Meta imediata: concluir o projeto base com **backend + dois frontends até 30/08/2026**. A prioridade é congelar rapidamente o contrato funcional do backend e iniciar os frontends.

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

Migrations atuais:

- `V1__create_rascomp_schema.sql` — schema base;
- `V2__create_inspecoes_sumo.sql` — inspeção do Sumô;
- `V3__create_rounds_sumo.sql` — rounds do Sumô.

### Validação de infraestrutura em 18/08/2026

Status: **MySQL + Flyway + Hibernate + Camunda validados em execução real**.

Resultado observado:

- conexão efetiva com `jdbc:mysql://localhost:3306/rascomp` usando MySQL 9.6;
- Flyway validou e executou `V1`, `V2` e `V3`, deixando o schema na versão `v3`;
- Hibernate com `ddl-auto=validate` passou após alinhar explicitamente `MatchResult.pontosA/pontosB` com as colunas `pontos_a/pontos_b`;
- datasource Hikari configurado com `TRANSACTION_READ_COMMITTED`, necessário para o Camunda no MySQL;
- Camunda criou seu schema interno e iniciou o `Process Engine default`;
- Tomcat iniciou na porta `8080`;
- `DataInitializer` persistiu os dados de teste no MySQL;
- `JobExecutor` do Camunda iniciou normalmente.

Correções aplicadas durante a validação:

- `MatchResult`: `@Column(name = "pontos_a")` e `@Column(name = "pontos_b")`;
- `application.properties`: `spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED`.

Observação: Flyway emitiu warning informando que MySQL 9.6 é mais novo que a versão oficialmente testada pelo Flyway atual. O warning não impediu conexão, validação nem execução das migrations.

## 3. Estratégia de desenvolvimento e testes

Os testes completos serão executados depois do fechamento das regras avançadas para evitar retrabalho antes dos frontends.

Arquivo de testes:

`docs/TESTES_POSTMAN.md`

Arquivo local não versionado de referência:

`docs/CODIGOS_REFERENCIA.md`

## 4. Status funcional

### CRUDs principais

Status: **implementados**.

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

### ETAPA A — ConfigFollow validando TentativaSeguidorLinha

Status: **implementado — testes finais pendentes**.

### ETAPA B — RankingFollowService

Status: **implementado — testes finais pendentes**.

### ETAPA C — geração automática da primeira rodada

Status: **implementado — testes finais pendentes**.

- próxima potência de dois define o tamanho da chave;
- BYEs completam a chave;
- participantes são sorteados antes da montagem.

### ETAPA D — árvore completa do chaveamento

Status: **implementado — testes finais pendentes**.

- todas as rodadas até a final são criadas;
- rodadas futuras começam `AGUARDANDO_PARTICIPANTES`.

### ETAPA E — avanço automático de vencedor + BYE

Status: **implementado — testes finais pendentes**.

- `BracketProgressionService` preenche automaticamente a próxima partida;
- ordem ímpar -> `registrationA`;
- ordem par -> `registrationB`;
- próxima ordem = `(ordem + 1) / 2`;
- BYE avança automaticamente sem resultado manual;
- final encerra o `Bracket`.

### ETAPA F — inspeção do Sumô

Status: **implementado — testes finais pendentes**.

Arquivos:

- `InspecaoSumo`;
- `InspecaoSumoDTO`;
- `InspecaoSumoRepository`;
- `InspecaoSumoService`;
- `InspecaoSumoController`.

Regras:

- inspeção por `Registration`;
- peso comparado com `ConfigSumo.pesoMax`;
- limite por `maxTentativasInspecao`;
- última reprovação permitida -> `Registration.DESCLASSIFICADA`;
- `exigeInspecao=true` exige inspeção aprovada para competir.

### ETAPA G — rounds do Sumô

Status: **implementado — testes finais pendentes**.

Arquivos adicionados:

- `StatusRoundSumo`;
- `RoundSumo`;
- `RoundSumoDTO`;
- `RoundSumoRepository`;
- `RoundSumoService`;
- `RoundSumoController`;
- migration `V3__create_rounds_sumo.sql`.

Status possíveis do round:

```text
FINALIZADO
EMPATADO
ANULADO
CANCELADO
```

Regras:

- round pertence a uma `Match`;
- somente partidas da modalidade `SUMO` aceitam rounds;
- partida deve possuir os dois participantes;
- ambos os participantes devem estar aptos pela regra de inspeção;
- número do round é calculado automaticamente pelo backend;
- `FINALIZADO` exige vencedor;
- vencedor deve ser um dos participantes da partida;
- `EMPATADO`, `ANULADO` e `CANCELADO` não aceitam vencedor;
- rounds regulares respeitam `ConfigSumo.numeroRounds`;
- somente um round adicional é permitido quando `permiteRoundDesempate=true` e ainda não existe vencedor;
- ao registrar o primeiro round, partida `AGENDADA` passa para `EM_ANDAMENTO`.

Endpoints:

```text
POST /api/v1/rounds-sumo
GET  /api/v1/rounds-sumo/{id}
GET  /api/v1/rounds-sumo/por-partida?matchId={id}
```

### ETAPA H — resultado automático do Sumô

Status: **implementado — testes finais pendentes**.

Fluxo:

```text
RoundSumo registrado
    -> conta vitórias de A e B
    -> compara com ConfigSumo.roundsParaVencer
    -> ao atingir o limite cria MatchResult automaticamente
    -> Match passa para FINALIZADA
    -> BracketProgressionService avança o vencedor
    -> se for a final, Bracket passa para FINALIZADO
```

Decisões:

- `MatchResult` do Sumô não é criado manualmente pelo frontend;
- POST manual de resultado para partida `SUMO` é rejeitado;
- atualização e exclusão manual de resultado automático do Sumô também são bloqueadas;
- `pontosA` e `pontosB` do `MatchResult` representam as vitórias em rounds de cada participante;
- observação automática identifica que o resultado foi consolidado pelos rounds;
- resultados de outras modalidades continuam usando o fluxo manual existente de `MatchResultService`.

## 5. Contrato atual relevante para frontend

### Seguidor de Linha

```text
POST /api/v1/tentativas-seguidor-linha
GET  /api/v1/tentativas-seguidor-linha/por-inscricao?registrationId={id}
GET  /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}
```

### Chaveamento

```text
POST /api/v1/chaveamentos/gerar?competitionId={id}&categoryId={id}
GET  /api/v1/partidas/por-chaveamento?bracketId={id}
```

### Sumô

```text
POST /api/v1/inspecoes-sumo
GET  /api/v1/inspecoes-sumo/por-inscricao?registrationId={id}
GET  /api/v1/inspecoes-sumo/aptidao?registrationId={id}
POST /api/v1/rounds-sumo
GET  /api/v1/rounds-sumo/por-partida?matchId={id}
GET  /api/v1/resultados-partida/por-partida?matchId={id}
```

## 6. Plano imediato — 18/08/2026

A infraestrutura persistente já subiu com sucesso. A próxima sequência é **confirmar persistência, revisar contrato e executar a bateria essencial de testes** antes de iniciar os frontends.

Ordem atual:

1. **Confirmar persistência e schema**
   - verificar `flyway_schema_history` com versões `1`, `2` e `3`;
   - verificar tabelas da aplicação e tabelas `ACT_*` do Camunda;
   - reiniciar a aplicação e confirmar que os dados permanecem e o `DataInitializer` não duplica a carga.

2. **Revisão rápida do backend e contrato da API**
   - conferir controllers, DTOs e services usados pelos frontends;
   - revisar inconsistências de rotas, retornos, status HTTP e nomes de campos;
   - remover ou corrigir somente o que bloquear integração;
   - evitar novas regras de domínio salvo bug crítico.

3. **Bateria de testes Postman**
   - atualizar `docs/TESTES_POSTMAN.md` para refletir ETAPAS E, F, G e H;
   - validar CRUDs essenciais;
   - validar tratamento global de exceções;
   - validar ConfigFollow e ranking;
   - validar geração da chave com potências de 2 e BYEs;
   - validar avanço automático de vencedores;
   - validar inspeção do Sumô;
   - validar rounds e resultado automático do Sumô;
   - registrar no documento qualquer endpoint/body que precisar de correção.

4. **Correções finais de bloqueio**
   - corrigir apenas erros encontrados nos testes que impeçam o fluxo real;
   - repetir somente os testes afetados pelas correções.

5. **Congelar o backend base**
   - marcar contrato principal da API como estável;
   - deixar melhorias não bloqueadoras para depois;
   - iniciar imediatamente os dois frontends.

## 7. Prioridade até 30/08/2026

1. 18/08 — persistência + revisão + bateria essencial de testes.
2. Congelar contrato da API base.
3. Iniciar os dois frontends imediatamente após a validação.
4. Priorizar primeiro os fluxos completos de competição no frontend antes de refinamentos visuais.
5. Swagger, JWT, testes automatizados adicionais e refinamentos não bloqueadores ficam em paralelo ou depois do primeiro fluxo fullstack funcional.
6. Camunda está validado como infraestrutura embarcada; modelagem BPMN funcional pode ser adicionada depois sem bloquear o início dos frontends.

## 8. Histórico resumido

- 2026-08-17 — ETAPA A: ConfigFollow aplicado às tentativas;
- 2026-08-17 — ETAPA B: ranking do Seguidor de Linha;
- 2026-08-17 — persistência migrada para MySQL + Flyway;
- 2026-08-17 — ETAPA C: geração automática da primeira rodada;
- 2026-08-17 — ETAPA D: árvore completa do chaveamento;
- 2026-08-17 — ETAPA E: sorteio de BYEs e avanço automático;
- 2026-08-17 — ETAPA F: inspeção do Sumô;
- 2026-08-17 — ETAPA G: rounds do Sumô;
- 2026-08-17 — ETAPA H: consolidação automática de MatchResult e avanço do vencedor;
- 2026-08-18 — corrigido mapeamento `MatchResult.pontosA/pontosB` para `pontos_a/pontos_b`;
- 2026-08-18 — datasource alterado para `TRANSACTION_READ_COMMITTED` para compatibilidade com Camunda/MySQL;
- 2026-08-18 — MySQL 9.6, Flyway V1/V2/V3, Hibernate validate, Camunda Process Engine, Tomcat 8080 e carga inicial validados com sucesso.
