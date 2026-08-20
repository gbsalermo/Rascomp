# Testes Postman — Rascomp

Última atualização: 2026-08-19

## 1. Objetivo

Bateria manual final do backend antes do congelamento do contrato da API.

Base URL:

```text
http://localhost:8080
```

Header para requests com body:

```text
Content-Type: application/json
```

## 2. Regra de domínio que orienta os testes

As duas modalidades possuem fluxos diferentes e não devem ser misturadas.

### FOLLOW_LINE

```text
Registration APROVADA
    -> ConfigFollow
    -> até 3 tomadas
    -> até 3 tentativas por tomada
    -> TentativaSeguidorLinha
    -> RankingFollowService
    -> menor tempo final vence
```

`FOLLOW_LINE` **não usa** `Bracket`, `Match` ou `MatchResult`.

Tempo final:

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

A melhor tentativa válida e concluída de cada inscrição representa o robô no ranking.

### SUMO

```text
Registration APROVADA
    -> InspecaoSumo
    -> aptidão
    -> Bracket
    -> Match
    -> RoundSumo
    -> MatchResult automático
    -> avanço no chaveamento
```

Chaveamentos e partidas são exclusivos de `SUMO`.

---

## 3. Infraestrutura já validada

- MySQL persistente;
- Flyway;
- Hibernate `ddl-auto=validate`;
- Camunda 7 embarcado;
- Hikari em `TRANSACTION_READ_COMMITTED`;
- persistência após reinicialização.

Após a correção de domínio, existe também:

```text
V4__remove_follow_line_brackets.sql
```

A V4 remove `Bracket`, `Match`, `MatchResult` e eventuais `RoundSumo` legados ligados a categoria `FOLLOW_LINE`.

---

## 4. Seed rápido para Postman

No Eclipse, em `Run Configurations -> Environment`, adicione:

```text
RASCOMP_SEED_POSTMAN=true
```

Ao reiniciar, o console informa dois cenários:

```text
FOLLOW - competição: X | categoria: Y | inscrições: A, B, C
SUMO   - competição: Z | categoria: W | inscrições: D, E, F
```

O cenário FOLLOW é para tentativas/ranking.
O cenário SUMO é para inspeção/chaveamento/rounds.

---

# PARTE A — contrato básico

## 5. Categorias

```http
GET /api/v1/categorias
GET /api/v1/categorias/{id}
GET /api/v1/categorias/por-modalidade?modalidade=FOLLOW_LINE
GET /api/v1/categorias/por-modalidade?modalidade=SUMO
```

## 6. Registration

```http
GET /api/v1/inscricoes
GET /api/v1/inscricoes/{id}
GET /api/v1/inscricoes/por-competicao?competitionId={id}
GET /api/v1/inscricoes/por-status?status=APROVADA
```

Duplicidade obrigatória:

```text
competitionId + categoryId + robotId
```

Tentar repetir deve retornar erro de regra de negócio.

---

# PARTE B — FOLLOW_LINE

## 7. ConfigFollow

```http
GET /api/v1/categorias/{categoryId}/config-follow
```

Configuração de referência:

```json
{
  "numeroTomadas": 3,
  "tentativasPorTomada": 3,
  "maxTempoSegundos": 180,
  "numeroCheckpoints": 5
}
```

## 8. TentativaSeguidorLinha

```http
POST /api/v1/tentativas-seguidor-linha
```

```json
{
  "registrationId": 1,
  "tomada": 2,
  "numeroTentativa": 1,
  "tempoSegundos": 45.350,
  "checkpointsAlcancados": 5,
  "penalidadeSegundos": 0,
  "concluida": true,
  "valida": true,
  "observacao": "Tentativa válida"
}
```

Validar:

```text
tomada > numeroTomadas -> rejeitar
numeroTentativa > tentativasPorTomada -> rejeitar
checkpoints > numeroCheckpoints -> rejeitar
tempo > maxTempoSegundos -> persiste com valida=false
```

## 9. Ranking Follow

```http
GET /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}
```

No seed da Vespa:

```text
42.315 + 0 = 42.315
40.870 + 2 = 42.870
```

Logo `42.315` deve ser a melhor marca.

Validar:

- apenas inscrições ativas e `APROVADA`;
- apenas tentativas válidas e concluídas;
- uma melhor tentativa por inscrição;
- ordenação crescente por tempo final;
- desempate por tempo final, tempo bruto e `registrationId`.

## 10. Proteção de domínio Follow

A tentativa abaixo deve ser rejeitada:

```http
POST /api/v1/chaveamentos/gerar?competitionId={FOLLOW_COMPETITION}&categoryId={FOLLOW_CATEGORY}
```

Esperado: `400`, informando que chaveamento é exclusivo de `SUMO` e que Follow é definido por ranking.

---

# PARTE C — SUMO

## 11. Inspeção

Use as três inscrições SUMO criadas pelo `PostmanScenarioInitializer`.

Aprovar A e B com peso dentro do limite:

```http
POST /api/v1/inspecoes-sumo
```

```json
{
  "registrationId": 7,
  "pesoMedido": 0.450,
  "observacao": "Aprovado"
}
```

Consulta:

```http
GET /api/v1/inspecoes-sumo/aptidao?registrationId={id}
GET /api/v1/inspecoes-sumo/por-inscricao?registrationId={id}
GET /api/v1/inspecoes-sumo/ultima?registrationId={id}
```

Para validar desclassificação, use a terceira inscrição e repita peso acima de `pesoMax` até `maxTentativasInspecao`.

Na última reprovação permitida:

```text
Registration -> DESCLASSIFICADA
```

## 12. Chaveamento SUMO

O gerador agora exige:

```text
SUMO
+ inscrição ativa
+ APROVADA
+ apta na inspeção quando exigeInspecao=true
```

Com apenas A e B aptos:

```http
POST /api/v1/chaveamentos/gerar?competitionId={SUMO_COMPETITION}&categoryId={SUMO_CATEGORY}
```

Esperado: uma chave com uma partida.

Para validar BYE, mantenha três inscrições SUMO aprovadas **e aptas** antes de gerar a chave.

Com três participantes aptos:

```text
chave de 4 posições
2 partidas na rodada 1
1 final
1 BYE com avanço automático
```

Conferir:

```http
GET /api/v1/partidas/por-chaveamento?bracketId={bracketId}
```

## 13. Proteção de Match

`Match` pertence somente a bracket `SUMO`.

Ao criar/alterar partida, os participantes devem:

- estar ativos;
- estar `APROVADA`;
- pertencer à mesma competição/categoria do bracket;
- estar aptos para competir no Sumô.

## 14. Rounds Sumô

```http
POST /api/v1/rounds-sumo
```

```json
{
  "matchId": 10,
  "winnerRegistrationId": 7,
  "status": "FINALIZADO",
  "observacao": "Vitória no round"
}
```

Status possíveis:

```text
FINALIZADO
EMPATADO
ANULADO
CANCELADO
```

Consultas:

```http
GET /api/v1/rounds-sumo/{id}
GET /api/v1/rounds-sumo/por-partida?matchId={id}
```

Quando uma inscrição atingir `ConfigSumo.roundsParaVencer`:

```text
RoundSumo
 -> MatchResult automático
 -> Match FINALIZADA
 -> vencedor avança
 -> final encerra Bracket
```

## 15. MatchResult é somente leitura pela API

No domínio atual:

- Follow não possui `MatchResult`;
- Sumô consolida `MatchResult` automaticamente pelos rounds.

Logo a API externa expõe apenas consultas:

```http
GET /api/v1/resultados-partida
GET /api/v1/resultados-partida/{id}
GET /api/v1/resultados-partida/por-partida?matchId={id}
GET /api/v1/resultados-partida/por-chaveamento?bracketId={id}
GET /api/v1/resultados-partida/por-competicao?competitionId={id}
```

`POST`, `PUT` e `DELETE` em `/api/v1/resultados-partida` não fazem parte do contrato atual e devem resultar em método não permitido (`405`).

---

# PARTE D — erros essenciais

## 16. Tratamento de erro

```http
GET /api/v1/inscricoes/999999
```

Esperado: `404`.

```http
GET /api/v1/inscricoes/por-competicao
```

Esperado: `400`.

```http
GET /api/v1/competicoes/por-status?status=INVALIDO
```

Esperado: `400`.

```http
GET /
```

O backend é API-only; raiz não mapeada deve ser tratada como `404`, não como erro interno `500`.

---

## 17. Critério para congelar a API

Pode avançar para congelamento quando estiverem validados:

```text
CRUDs essenciais
+ Follow: tentativas e ranking
+ bloqueio de bracket Follow
+ Sumô: inspeção/aptidão
+ bracket apenas Sumô
+ BYE/progressão
+ rounds
+ MatchResult automático
+ tratamento global de erros
```

Depois do congelamento:

```text
Swagger/OpenAPI
 -> primeiro fluxo do Frontend de Gestão
 -> Camunda BPMN funcional
 -> restante do Frontend de Gestão
 -> Frontend Público
```
