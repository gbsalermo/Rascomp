# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-18T10:31:00-03:00

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

Status: **MySQL + Flyway + Hibernate + Camunda + persistência validados em execução real**.

Resultado observado:

- conexão efetiva com `jdbc:mysql://localhost:3306/rascomp` usando MySQL 9.6;
- Flyway validou e executou `V1`, `V2` e `V3`, todas com `success=1`;
- Hibernate com `ddl-auto=validate` passou após alinhar explicitamente `MatchResult.pontosA/pontosB` com `pontos_a/pontos_b`;
- datasource Hikari configurado com `TRANSACTION_READ_COMMITTED` para Camunda/MySQL;
- Camunda criou 49 tabelas `ACT_*`, criou o `Process Engine default` e iniciou o JobExecutor;
- Tomcat iniciou na porta `8080`;
- `DataInitializer` persistiu os dados de teste;
- aplicação foi reiniciada e reutilizou o mesmo banco com sucesso.

Correções aplicadas durante a validação:

- `MatchResult`: `@Column(name = "pontos_a")` e `@Column(name = "pontos_b")`;
- `application.properties`: `spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED`.

Observação: Flyway emite warning porque MySQL 9.6 é mais novo que a versão oficialmente testada pelo Flyway atual. O warning não impediu conexão, migrations nem startup.

### Ambiente local do MySQL

Foi criado um fluxo local para evitar redefinir as credenciais a cada PowerShell:

- `.env.local` está no `.gitignore` e nunca deve ser versionado;
- `.env.example` documenta as variáveis esperadas sem segredo real;
- `run-local.ps1` cria `.env.local` na primeira execução, carrega as credenciais e inicia o Spring Boot;
- comando de uso: `.\run-local.ps1`.

## 3. Estratégia de desenvolvimento e testes

Arquivo oficial da bateria manual:

`docs/TESTES_POSTMAN.md`

O roteiro foi atualizado em 18/08 para refletir o backend real, incluindo:

- status `DESCLASSIFICADA`;
- avanço automático de vencedor e BYE;
- endpoints de inspeção do Sumô;
- endpoints de rounds do Sumô;
- consolidação automática de `MatchResult` no Sumô;
- bloqueio de POST/PUT/DELETE manual de resultado Sumô;
- tratamento global de exceções;
- ordem essencial de testes para não atrasar os frontends.

Arquivo local não versionado de referência:

`docs/CODIGOS_REFERENCIA.md`

## 4. Status funcional

### CRUDs principais

Status: **implementados — bateria manual final pendente**.

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

Status: **implementado — teste final pendente**.

### ETAPA B — RankingFollowService

Status: **implementado — teste final pendente**.

### ETAPA C — geração automática da primeira rodada

Status: **implementado — teste final pendente**.

- próxima potência de dois define o tamanho da chave;
- BYEs completam a chave;
- participantes são sorteados antes da montagem.

### ETAPA D — árvore completa do chaveamento

Status: **implementado — teste final pendente**.

- todas as rodadas até a final são criadas;
- rodadas futuras começam `AGUARDANDO_PARTICIPANTES`.

### ETAPA E — avanço automático de vencedor + BYE

Status: **implementado — teste final pendente**.

- `BracketProgressionService` preenche automaticamente a próxima partida;
- ordem ímpar -> `registrationA`;
- ordem par -> `registrationB`;
- próxima ordem = `(ordem + 1) / 2`;
- BYE avança automaticamente sem resultado manual;
- final encerra o `Bracket`.

### ETAPA F — inspeção do Sumô

Status: **implementado — teste final pendente**.

Regras principais:

- inspeção por `Registration`;
- peso comparado com `ConfigSumo.pesoMax`;
- limite por `maxTentativasInspecao`;
- última reprovação permitida -> `Registration.DESCLASSIFICADA`;
- `exigeInspecao=true` exige inspeção aprovada para competir.

Endpoints:

```text
POST /api/v1/inspecoes-sumo
GET  /api/v1/inspecoes-sumo/{id}
GET  /api/v1/inspecoes-sumo/por-inscricao?registrationId={id}
GET  /api/v1/inspecoes-sumo/ultima?registrationId={id}
GET  /api/v1/inspecoes-sumo/aptidao?registrationId={id}
```

### ETAPA G — rounds do Sumô

Status: **implementado — teste final pendente**.

Status possíveis:

```text
FINALIZADO
EMPATADO
ANULADO
CANCELADO
```

Endpoints:

```text
POST /api/v1/rounds-sumo
GET  /api/v1/rounds-sumo/{id}
GET  /api/v1/rounds-sumo/por-partida?matchId={id}
```

### ETAPA H — resultado automático do Sumô

Status: **implementado — teste final pendente**.

Fluxo:

```text
RoundSumo registrado
    -> conta vitórias de A e B
    -> compara com ConfigSumo.roundsParaVencer
    -> cria MatchResult ao atingir o limite
    -> Match FINALIZADA
    -> BracketProgressionService avança o vencedor
    -> final encerra o Bracket
```

Decisões:

- `MatchResult` do Sumô não é criado manualmente pelo frontend;
- POST, PUT e DELETE manuais para resultado SUMO são rejeitados;
- `pontosA/pontosB` representam vitórias em rounds;
- outras modalidades mantêm o fluxo manual de resultado.

## 5. Contrato relevante para frontend

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
GET  /api/v1/resultados-partida/por-partida?matchId={id}
```

### Sumô

```text
POST /api/v1/inspecoes-sumo
GET  /api/v1/inspecoes-sumo/aptidao?registrationId={id}
POST /api/v1/rounds-sumo
GET  /api/v1/rounds-sumo/por-partida?matchId={id}
GET  /api/v1/resultados-partida/por-partida?matchId={id}
```

## 6. Plano imediato

### Noite de 18/08

Executar `docs/TESTES_POSTMAN.md`, priorizando:

1. smoke test dos GETs;
2. CRUDs essenciais;
3. ConfigFollow + tentativa + ranking;
4. tratamento global de exceções;
5. bracket com 3 participantes para forçar BYE e progressão;
6. inscrições + inspeções Sumô;
7. chave Sumô + rounds até `MatchResult` automático;
8. bloqueio de resultado manual Sumô;
9. registrar e corrigir somente bugs bloqueadores.

### 19/08

Se a bateria essencial passar:

1. congelar contrato base do backend;
2. iniciar os dois frontends;
3. priorizar primeiro os fluxos completos de competição;
4. deixar refinamentos visuais e funcionalidades não bloqueadoras para depois.

## 7. Prioridade até 30/08/2026

1. concluir bateria manual final;
2. corrigir apenas bugs que bloqueiem integração;
3. congelar API base;
4. iniciar imediatamente os dois frontends;
5. Swagger, JWT, testes automatizados adicionais e refinamentos não bloqueadores ficam em paralelo ou depois do primeiro fluxo fullstack funcional;
6. Camunda está validado como infraestrutura embarcada; BPMN funcional não deve bloquear o frontend.

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
- 2026-08-18 — corrigido mapeamento `MatchResult.pontosA/pontosB`;
- 2026-08-18 — datasource alterado para `TRANSACTION_READ_COMMITTED` para Camunda/MySQL;
- 2026-08-18 — MySQL, Flyway, Hibernate, Camunda e persistência validados;
- 2026-08-18 — criado fluxo local `.env.local` + `run-local.ps1` para credenciais MySQL;
- 2026-08-18 — `TESTES_POSTMAN.md` atualizado com ETAPAS E/F/G/H e ordem final de validação;
- 2026-08-18 — combinado: bateria manual à noite; início dos frontends em 19/08 se não houver bloqueador.
