# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-19T21:55:00-03:00

## 1. Estado atual

O Rascomp está na fase de **validação final do backend antes do congelamento da API**.

Infraestrutura já validada:

- Java 21 + Spring Boot 3.5.3;
- MySQL persistente;
- Flyway;
- Hibernate com `ddl-auto=validate`;
- Hikari em `TRANSACTION_READ_COMMITTED`;
- Camunda 7.22 embarcado, Process Engine e tabelas `ACT_*` validados;
- persistência após reinicialização.

Migrations atuais:

```text
V1__create_rascomp_schema.sql
V2__create_inspecoes_sumo.sql
V3__create_rounds_sumo.sql
V4__remove_follow_line_brackets.sql
```

A V4 remove chaveamentos legados de FOLLOW_LINE criados antes da correção de domínio.

---

## 2. Regra de domínio consolidada por modalidade

### FOLLOW_LINE

Fluxo oficial:

```text
Registration APROVADA
    -> ConfigFollow
    -> 3 tomadas
    -> até 3 tentativas por tomada
    -> TentativaSeguidorLinha
    -> RankingFollowService
    -> menor tempo final vence
```

Tempo final:

```text
tempoSegundos + penalidadeSegundos
```

FOLLOW_LINE **não utiliza**:

```text
Bracket
Match
MatchResult
```

O backend agora rejeita tentativa de criar ou gerar chaveamento para FOLLOW_LINE.

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
    -> BracketProgressionService
```

Regras consolidadas:

- Bracket é exclusivo de SUMO;
- geração de bracket considera apenas inscrições ativas, `APROVADA` e aptas;
- Match pertence apenas a bracket SUMO;
- participantes de Match também precisam estar aptos;
- MatchResult de SUMO nasce automaticamente dos rounds;
- MatchResult é somente leitura na API externa atual.

---

## 3. Correção de domínio aplicada em 19/08

Durante a bateria manual foi identificado que o backend permitia chaveamento para FOLLOW_LINE, apesar de a modalidade ser disputada por ranking.

Correções aplicadas:

- `BracketGenerationService` restrito a `Modalidade.SUMO`;
- `BracketService` restrito a SUMO;
- `MatchService` restrito a SUMO e com validação de aptidão;
- `BracketGenerationService` passou a filtrar inscrições pela aptidão do Sumô;
- `MatchResultController` passou a ser somente leitura;
- `MatchResultService` documenta/bloqueia operações manuais nas modalidades atuais;
- `DataInitializer` não cria mais bracket/match/result para Follow;
- `PostmanScenarioInitializer` separa cenário de ranking Follow e cenário de chaveamento Sumô;
- criada migration V4 para limpar registros legados Follow em `brackets`, `matches`, `match_results` e `rounds_sumo`;
- `TESTES_POSTMAN.md` refeito por modalidade.

O teste de ranking da Vespa já confirmou:

```text
42.315 + 0 = 42.315
40.870 + 2 = 42.870
```

Logo a marca válida `42.315` é a melhor da inscrição.

---

## 4. Testes manuais — ponto atual

Já observado na bateria:

- validação de duplicidade de `Registration` funcionando;
- ranking FOLLOW_LINE funcionando para o cenário seed;
- mecanismo de BYE/progressão chegou a funcionar em um bracket Follow, mas esse cenário foi classificado como **inválido do ponto de vista do domínio** e foi removido pela correção.

Próximo passo correto:

1. reiniciar a aplicação para aplicar Flyway V4;
2. ativar `RASCOMP_SEED_POSTMAN=true`;
3. confirmar que gerar bracket para FOLLOW_LINE retorna `400`;
4. usar o cenário SUMO;
5. aprovar inspeções;
6. gerar bracket SUMO;
7. validar BYE/progressão com três participantes aptos;
8. registrar rounds;
9. confirmar `MatchResult` automático;
10. terminar tratamento de erros e congelar API.

Arquivo oficial:

```text
docs/TESTES_POSTMAN.md
```

---

## 5. Testes automatizados

Branch dedicada:

```text
testes-automatizados
```

Ela contém JUnit 5 + Mockito e deve acompanhar a regra consolidada:

- ranking e tentativa para FOLLOW_LINE;
- rejeição de bracket FOLLOW_LINE;
- bracket/BYE apenas para SUMO;
- aptidão/inspeção no Sumô;
- consolidação automática de resultado.

A branch deve ser validada com:

```powershell
cd rascomp
.\mvnw.cmd test
```

antes do merge.

---

## 6. Planejamento oficial

Ordem atual:

```text
1. terminar Postman
2. corrigir bugs bloqueadores
3. validar testes automatizados
4. congelar contrato da API
5. implementar/configurar Swagger/OpenAPI sobre o contrato congelado
6. iniciar Frontend de Gestão
7. concluir um fluxo administrativo real
8. implementar Camunda BPMN funcional
9. integrar Camunda ao Frontend de Gestão
10. completar Frontend de Gestão
11. desenvolver Frontend Público
12. JWT/autenticação e refinamentos finais
```

### Camunda

Estado atual:

```text
Engine embarcado     OK
Banco ACT_*          OK
JobExecutor          OK
REST do Camunda      OK
BPMN do Rascomp      pendente
```

O primeiro BPMN planejado continua sendo o fluxo administrativo de inscrição:

```text
PENDENTE
  -> análise administrativa
  -> APROVADA ou REJEITADA
```

Camunda funcional entra **depois do primeiro fluxo do Frontend de Gestão**, para evitar modelar BPMN em cima de um contrato ainda instável.

### Swagger

Springdoc já está presente como dependência, mas a implementação/documentação formal deve ocorrer **após a bateria final e o congelamento da API**.

---

## 7. Critério de saída do backend

O backend pode ser considerado pronto para congelamento quando passarem:

```text
CRUDs essenciais
FOLLOW_LINE: ConfigFollow + Tentativa + Ranking
proteção contra Bracket/Match em FOLLOW_LINE
SUMO: inspeção + aptidão
SUMO: bracket + BYE + progressão
SUMO: rounds + MatchResult automático
tratamento global 400/404/409/405
persistência/migrations
```

A partir daí, mudanças de contrato devem ser excepcionais e motivadas por bloqueios reais do frontend.
