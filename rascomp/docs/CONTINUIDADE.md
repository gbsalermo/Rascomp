# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-23T22:50:00-03:00

## 1. Marco atual

O backend do Rascomp concluiu a fase de validação e o **contrato da API está congelado para a etapa Swagger/OpenAPI**.

Situação consolidada:

```text
Infraestrutura                    ✅
MySQL persistente                 ✅
Flyway V1–V4                      ✅
Camunda 7 embarcado               ✅
CRUDs essenciais                  ✅
FOLLOW_LINE manual                ✅
SUMO manual                       ✅
Erros HTTP essenciais             ✅
JUnit/Mockito                     ✅
GitHub Actions                    ✅
Branch testes-automatizados       ✅ mergeada na main
Contrato da API                   🔒 congelado
Swagger/OpenAPI                   ▶ próxima etapa
```

Merge da branch de testes realizado em 23/08/2026 pelo PR #2.

---

## 2. Stack consolidada

- Java 21;
- Spring Boot 3.5.3;
- Spring Web;
- Spring Data JPA / Hibernate;
- Jakarta Validation;
- MySQL persistente;
- HikariCP com `TRANSACTION_READ_COMMITTED`;
- Flyway;
- Camunda 7.22 embarcado;
- Springdoc OpenAPI 2.8.9;
- JUnit 5 + Mockito;
- GitHub Actions.

Migrations atuais:

```text
V1__create_rascomp_schema.sql
V2__create_inspecoes_sumo.sql
V3__create_rounds_sumo.sql
V4__remove_follow_line_brackets.sql
```

A V4 remove dados legados de chaveamento associados a `FOLLOW_LINE` criados antes da correção definitiva do domínio.

---

## 3. Domínio congelado por modalidade

### FOLLOW_LINE

Fluxo oficial:

```text
Registration APROVADA
    -> ConfigFollow
    -> 3 tomadas
    -> até 3 tentativas por tomada
    -> TentativaSeguidorLinha
    -> melhor tentativa válida e concluída
    -> RankingFollowService
    -> classificação por menor tempo final
```

Cálculo:

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

Regras validadas manualmente:

- 3 tomadas × 3 tentativas;
- limite de tomada;
- limite de tentativa;
- limite de checkpoints;
- duplicidade de `tomada + numeroTentativa` por inscrição;
- tempo acima do máximo persiste como tentativa inválida;
- tentativa inválida não entra no ranking;
- tentativa não concluída não entra no ranking;
- penalidade entra no tempo final;
- melhor tentativa válida representa o robô;
- ranking ordena corretamente os robôs;
- inscrição SUMO não aceita tentativa Follow;
- ranking Follow rejeita categoria SUMO;
- geração de bracket para Follow retorna erro de regra de negócio.

`FOLLOW_LINE` **não utiliza**:

```text
Bracket
Match
MatchResult
RoundSumo
```

### SUMO

Fluxo oficial:

```text
Registration APROVADA
    -> InspecaoSumo
    -> aptidão
    -> Bracket
    -> Match
    -> RoundSumo
    -> MatchResult automático
    -> avanço do vencedor
    -> encerramento do Bracket
```

Regras validadas manualmente:

- `ConfigSumo` presente para a categoria utilizada;
- aprovação por peso dentro do limite;
- reprovações de inspeção;
- desclassificação ao atingir limite de tentativas reprovadas;
- consulta de aptidão;
- apenas inscrições ativas, aprovadas e aptas entram no bracket;
- bracket exclusivo de SUMO;
- participante desclassificado/não apto fica fora da chave;
- rounds finalizados e empatados;
- vitória contabilizada conforme `roundsParaVencer`;
- `MatchResult` criado automaticamente;
- `Match` encerrado automaticamente;
- `Bracket` encerrado automaticamente na final;
- API de `MatchResult` é somente leitura;
- `POST`, `PUT` e `DELETE` externos em `resultados-partida` resultam em `405`.

---

## 4. Qualidade e testes

### Bateria manual

A bateria manual final foi concluída em 23/08/2026.

Cobertura funcional confirmada:

```text
CRUD/contrato básico             ✅
FOLLOW_LINE completo             ✅
SUMO completo                    ✅
400                              ✅
404                              ✅
405                              ✅
persistência                     ✅
regras de modalidade             ✅
```

Arquivo de referência:

```text
docs/TESTES_POSTMAN.md
```

### Testes automatizados

A branch `testes-automatizados` foi validada e mergeada na `main`.

A suíte contém testes de serviço para, entre outros:

- `CompetitionCategoryService`;
- `TentativaSeguidorLinhaService`;
- `RankingFollowService`;
- `InspecaoSumoService`;
- `BracketService`;
- `BracketGenerationService`;
- `MatchService`;
- `MatchResultService`.

O GitHub Actions `Backend Tests` executa:

```bash
mvn -B test
```

com Java 21.

A execução validada antes do merge terminou com `success`.

---

## 5. Congelamento da API

A partir deste marco, controllers, rotas, DTOs, enums e regras principais passam a ser tratados como contrato estável para Swagger e frontends.

Mudanças de contrato só devem ocorrer por:

1. bug comprovado;
2. incompatibilidade encontrada durante documentação Swagger;
3. bloqueio real de integração do frontend;
4. requisito funcional indispensável ainda não representado.

Evitar durante o congelamento:

- renomear endpoints sem necessidade;
- remover campos de DTO;
- alterar significado de enums;
- mover regra de negócio validada para controllers;
- reabrir a separação de domínio entre Follow e Sumô;
- alterar migrations antigas já aplicadas.

Migrations futuras devem ser novas e incrementais.

Documento específico:

```text
docs/CONGELAMENTO_API.md
```

---

## 6. Camunda — estado e decisão atual

Infraestrutura validada:

```text
Camunda 7.22         ✅
Process Engine       ✅
JobExecutor          ✅
tabelas ACT_*        ✅
REST starter         ✅
BPMN Rascomp         ⏳ não implementado
```

Decisão atual: **não implementar BPMN antes de concluir Swagger**.

As regras de competição continuam em Java. Camunda, se adotado no fluxo final, deverá **orquestrar processo humano/administrativo**, e não substituir `RankingFollowService`, `BracketGenerationService`, `RoundSumoService` ou demais regras já validadas.

Primeiro candidato continua sendo aprovação de inscrição:

```text
Inscrição criada
    -> PENDENTE
    -> análise administrativa
    -> APROVADA ou REJEITADA
```

Após Swagger haverá um checkpoint para escolher entre:

- integrar um BPMN mínimo de aprovação antes do frontend completo;
- integrar Camunda junto do primeiro fluxo do Frontend de Gestão;
- adiar BPMN funcional para depois do MVP visual, mantendo apenas a infraestrutura já pronta.

Nenhuma dessas opções é escolhida antes da revisão pós-Swagger.

---

## 7. Nova forma de execução do projeto

A partir deste marco, o desenvolvimento deve ser conduzido em modo de **orquestração**:

- a documentação define contratos, ordem e critérios de aceite;
- implementação pode ser delegada a IA/ferramentas/agentes;
- cada etapa deve terminar com revisão objetiva e teste verificável;
- não avançar por quantidade de código, e sim por critério de saída atendido;
- `CONTINUIDADE.md` permanece a fonte principal do estado do projeto.

Isso reduz trabalho manual de implementação e mantém controle arquitetural mesmo com execução delegada.

---

## 8. Próxima etapa — Swagger/OpenAPI

A dependência já está presente no `pom.xml`:

```text
org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9
```

A etapa Swagger deve documentar o contrato congelado, sem alterar o comportamento da API.

Critério de conclusão planejado:

```text
OpenAPI carregando
Swagger UI acessível
informações gerais da API
controllers organizados por tags
operações descritas
path/query params documentados
requests documentados
responses 200/201/204/400/404/405/409 documentadas
DTOs/schemas legíveis
exemplos úteis de JSON
erro padrão documentado
FOLLOW_LINE e SUMO claramente separados
MatchResult identificado como somente leitura
conferência visual endpoint por endpoint
```

URLs esperadas do Springdoc, sujeitas à validação na implementação:

```text
/v3/api-docs
/swagger-ui/index.html
```

---

## 9. Roadmap a partir daqui

```text
BACKEND VALIDADO E CONGELADO ✅
            ↓
SWAGGER / OPENAPI           ▶
            ↓
CHECKPOINT ARQUITETURAL
   ├─ Camunda agora?
   ├─ Frontend de Gestão primeiro?
   └─ BPMN pós-MVP?
            ↓
CAMINHO ESCOLHIDO
            ↓
Frontend de Gestão
            ↓
Frontend Público
            ↓
Autenticação/JWT e refinamentos
```

O roadmap deixa de fixar prematuramente a posição do Camunda. A decisão será tomada com a API documentada e a dimensão real da integração visível.

---

## 10. Próximo comando de trabalho

**Iniciar Swagger/OpenAPI sobre a API congelada.**

Antes de qualquer alteração de regra de negócio, consultar este arquivo e `docs/CONGELAMENTO_API.md`.
