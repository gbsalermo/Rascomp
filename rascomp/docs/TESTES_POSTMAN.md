# Testes Postman — Rascomp

Última atualização: 2026-08-23

## 1. Status

A bateria manual final do backend foi concluída com sucesso em 23/08/2026.

```text
Contrato básico                 ✅ PASS
FOLLOW_LINE                     ✅ PASS
SUMO                            ✅ PASS
Tratamento de erros             ✅ PASS
Persistência/migrations         ✅ PASS
```

Este arquivo passa a funcionar como **roteiro de regressão manual**. Não é necessário repetir toda a bateria antes de cada alteração documental; repetir somente quando houver mudança de comportamento da API.

Base URL:

```text
http://localhost:8080
```

Header para requests com body:

```text
Content-Type: application/json
```

---

## 2. Regra de domínio validada

### FOLLOW_LINE

```text
Registration APROVADA
    -> ConfigFollow
    -> 3 tomadas
    -> até 3 tentativas por tomada
    -> TentativaSeguidorLinha
    -> melhor tentativa válida e concluída
    -> RankingFollowService
    -> menor tempo final vence
```

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

FOLLOW_LINE não usa:

```text
Bracket
Match
MatchResult
RoundSumo
```

### SUMO

```text
Registration APROVADA
    -> InspecaoSumo
    -> aptidão
    -> Bracket
    -> Match
    -> RoundSumo
    -> MatchResult automático
    -> avanço do vencedor
```

Chaveamentos e partidas são exclusivos de SUMO.

---

## 3. Seed opcional para regressão

No Eclipse, em `Run Configurations -> Environment`:

```text
RASCOMP_SEED_POSTMAN=true
```

O console informa cenários isolados:

```text
FOLLOW - competição: X | categoria: Y | inscrições: A, B, C
SUMO   - competição: Z | categoria: W | inscrições: D, E, F
```

O initializer também garante `ConfigFollow` e `ConfigSumo` para as categorias usadas.

Nunca assumir IDs sem conferir o console ou a API.

---

# PARTE A — CONTRATO BÁSICO

## 4. Categorias

```http
GET /api/v1/categorias
GET /api/v1/categorias/{id}
GET /api/v1/categorias/por-modalidade?modalidade=FOLLOW_LINE
GET /api/v1/categorias/por-modalidade?modalidade=SUMO
```

Resultado final: `PASS`.

## 5. Inscrições

```http
GET /api/v1/inscricoes
GET /api/v1/inscricoes/{id}
GET /api/v1/inscricoes/por-competicao?competitionId={id}
GET /api/v1/inscricoes/por-status?status=APROVADA
```

Duplicidade validada:

```text
competitionId + categoryId + robotId
```

Resultado final: `PASS`.

---

# PARTE B — FOLLOW_LINE

## 6. ConfigFollow

```http
GET /api/v1/categorias/{categoryId}/config-follow
```

Configuração usada na bateria:

```json
{
  "numeroTomadas": 3,
  "tentativasPorTomada": 3,
  "maxTempoSegundos": 180,
  "numeroCheckpoints": 5
}
```

Resultado final: `PASS`.

## 7. Criar tentativa

```http
POST /api/v1/tentativas-seguidor-linha
```

Exemplo:

```json
{
  "registrationId": 4,
  "tomada": 1,
  "numeroTentativa": 1,
  "tempoSegundos": 45.000,
  "checkpointsAlcancados": 5,
  "penalidadeSegundos": 0,
  "concluida": true,
  "valida": true,
  "observacao": "Tentativa de regressão"
}
```

A bateria validou:

```text
3 tomadas x 3 tentativas                         ✅
tomada 4 -> 400                                  ✅
numeroTentativa 4 -> 400                         ✅
checkpoints acima do limite -> 400               ✅
duplicidade tomada+tentativa -> 400              ✅
tempo > maxTempo -> persiste com valida=false    ✅
tentativa inválida ignorada no ranking           ✅
tentativa não concluída ignorada                 ✅
penalidade somada ao tempo final                 ✅
inscrição SUMO rejeitada                         ✅
```

## 8. Listar tentativas por inscrição

```http
GET /api/v1/tentativas-seguidor-linha/por-inscricao?registrationId={id}
```

Resultado final: `PASS`.

## 9. Ranking Follow

```http
GET /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}
```

Cenário final validado:

```text
Postman Follow B -> 36.000
Postman Follow C -> 38.000
Postman Follow A -> 39.500
```

Ordenação esperada:

```text
1º B
2º C
3º A
```

Também foi validado que uma tentativa bruta mais rápida com penalidade não supera um tempo final melhor e que tentativas inválidas/não concluídas não participam da seleção.

Resultado final: `PASS`.

## 10. Proteções de modalidade

Bracket Follow:

```http
POST /api/v1/chaveamentos/gerar?competitionId={FOLLOW_COMPETITION}&categoryId={FOLLOW_CATEGORY}
```

Esperado:

```text
400
```

Ranking usando categoria SUMO:

```http
GET /api/v1/ranking/seguidor-linha?competitionId={SUMO_COMPETITION}&categoryId={SUMO_CATEGORY}
```

Esperado:

```text
400
```

Resultado final: `PASS`.

---

# PARTE C — SUMO

## 11. ConfigSumo

```http
GET /api/v1/categorias/{categoryId}/config-sumo
```

A bateria confirmou que o cenário Postman possui configuração válida antes da inspeção.

Resultado final: `PASS`.

## 12. Inspeção

```http
POST /api/v1/inspecoes-sumo
```

Exemplo:

```json
{
  "registrationId": 7,
  "pesoMedido": 2.500,
  "observacao": "Inspeção aprovada"
}
```

Consultas:

```http
GET /api/v1/inspecoes-sumo/aptidao?registrationId={id}
GET /api/v1/inspecoes-sumo/por-inscricao?registrationId={id}
GET /api/v1/inspecoes-sumo/ultima?registrationId={id}
```

A bateria validou:

```text
aprovação por peso válido                         ✅
reprovações sucessivas                            ✅
desclassificação no limite configurado            ✅
aptidão true/true/false                           ✅
```

Resultado final: `PASS`.

## 13. Chaveamento SUMO

```http
POST /api/v1/chaveamentos/gerar?competitionId={SUMO_COMPETITION}&categoryId={SUMO_CATEGORY}
```

Regras confirmadas:

```text
modalidade SUMO
+ inscrição ativa
+ status APROVADA
+ aptidão válida
```

Inscrição desclassificada/não apta ficou fora da chave.

Consulta:

```http
GET /api/v1/partidas/por-chaveamento?bracketId={bracketId}
```

Resultado final: `PASS`.

## 14. RoundSumo

```http
POST /api/v1/rounds-sumo
```

Exemplo de vitória:

```json
{
  "matchId": 10,
  "winnerRegistrationId": 7,
  "status": "FINALIZADO",
  "observacao": "Vitória no round"
}
```

Exemplo de empate:

```json
{
  "matchId": 10,
  "winnerRegistrationId": null,
  "status": "EMPATADO",
  "observacao": "Round empatado"
}
```

Consultas:

```http
GET /api/v1/rounds-sumo/{id}
GET /api/v1/rounds-sumo/por-partida?matchId={id}
```

Fluxo validado:

```text
roundsParaVencer atingido
 -> MatchResult automático
 -> Match FINALIZADA
 -> avanço do vencedor
 -> final encerra Bracket
```

Resultado final: `PASS`.

## 15. MatchResult somente leitura

Consultas válidas:

```http
GET /api/v1/resultados-partida
GET /api/v1/resultados-partida/{id}
GET /api/v1/resultados-partida/por-partida?matchId={id}
GET /api/v1/resultados-partida/por-chaveamento?bracketId={id}
GET /api/v1/resultados-partida/por-competicao?competitionId={id}
```

Métodos externos não permitidos:

```text
POST   -> 405
PUT    -> 405
DELETE -> 405
```

Resultado final: `PASS`.

---

# PARTE D — ERROS ESSENCIAIS

## 16. 404

```http
GET /api/v1/inscricoes/999999
```

Esperado: `404`.

Resultado: `PASS`.

## 17. Parâmetro obrigatório

```http
GET /api/v1/inscricoes/por-competicao
```

Esperado: `400`.

Resultado: `PASS`.

## 18. Enum inválido

```http
GET /api/v1/competicoes/por-status?status=INVALIDO
```

Esperado: `400`.

Resultado: `PASS`.

## 19. Raiz API-only

```http
GET /
```

Esperado: `404`, nunca `500`.

Resultado: `PASS`.

---

## 20. Resultado final da etapa

Critério de congelamento atendido:

```text
CRUDs essenciais                             ✅
FOLLOW_LINE Config/Tentativas/Ranking         ✅
proteção Follow contra bracket                ✅
SUMO inspeção/aptidão                         ✅
SUMO bracket/filtro de aptidão                ✅
SUMO rounds                                   ✅
MatchResult automático                        ✅
MatchResult somente leitura                   ✅
400/404/405                                   ✅
persistência/migrations                       ✅
```

**Conclusão: API liberada para documentação Swagger/OpenAPI.**

Próxima bateria completa só é necessária se Swagger revelar alteração de contrato ou se uma etapa futura exigir mudança funcional.
