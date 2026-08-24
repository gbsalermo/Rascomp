# Exemplos JSON — API Rascomp

Documento de apoio para Postman e frontends.

Base:

```text
http://localhost:8080/api/v1
```

> Substitua os IDs pelos retornados pela sua execução.

---

## 1. Categoria FOLLOW_LINE

```http
POST /api/v1/categorias
```

```json
{
  "nome": "Seguidor de Linha",
  "descricao": "Categoria definida por ranking de tempo",
  "modalidade": "FOLLOW_LINE",
  "ativo": true
}
```

## 2. ConfigFollow

```http
POST /api/v1/categorias/{categoryId}/config-follow
```

```json
{
  "numeroTomadas": 3,
  "tentativasPorTomada": 3,
  "maxTempoSegundos": 180,
  "numeroCheckpoints": 5
}
```

## 3. Categoria SUMO

```http
POST /api/v1/categorias
```

```json
{
  "nome": "Mini Sumô",
  "descricao": "Categoria eliminatória de Sumô",
  "modalidade": "SUMO",
  "ativo": true
}
```

## 4. ConfigSumo

```http
POST /api/v1/categorias/{categoryId}/config-sumo
```

```json
{
  "pesoMax": 0.500,
  "exigeInspecao": true,
  "maxTentativasInspecao": 3,
  "numeroRounds": 3,
  "roundsParaVencer": 2,
  "permiteRoundDesempate": true
}
```

---

## 5. Institution

```json
{
  "nome": "Universidade Federal do Recôncavo da Bahia",
  "sigla": "UFRB",
  "cidade": "Cruz das Almas",
  "estado": "BA",
  "ativo": true
}
```

```http
POST /api/v1/instituicoes
```

---

## 6. Team

```json
{
  "nome": "RAS UFRB",
  "institutionId": 1,
  "ativo": true
}
```

```http
POST /api/v1/equipes
```

---

## 7. Competitor

```json
{
  "nome": "Competidor Teste",
  "email": "competidor@rascomp.dev",
  "telefone": "75999999999",
  "teamId": 1,
  "ativo": true
}
```

```http
POST /api/v1/competidores
```

---

## 8. Robot

```json
{
  "nome": "Vespa",
  "descricao": "Robô de competição",
  "teamId": 1,
  "ativo": true
}
```

```http
POST /api/v1/robos
```

---

## 9. Competition

```json
{
  "nome": "RRC 2026",
  "descricao": "Competição de robótica",
  "inicioInscricoes": "2026-08-01",
  "fimInscricoes": "2026-08-31",
  "dataInicio": "2026-09-05",
  "dataFim": "2026-09-06",
  "status": "INSCRICOES_ABERTAS",
  "ativo": true
}
```

```http
POST /api/v1/competicoes
```

---

## 10. Registration

### Follow

```json
{
  "competitionId": 1,
  "categoryId": 3,
  "teamId": 1,
  "robotId": 1,
  "status": "APROVADA",
  "observacao": "Inscrição Follow",
  "ativo": true
}
```

### Sumô

```json
{
  "competitionId": 2,
  "categoryId": 1,
  "teamId": 1,
  "robotId": 4,
  "status": "APROVADA",
  "observacao": "Inscrição Sumô",
  "ativo": true
}
```

```http
POST /api/v1/inscricoes
```

Regra de unicidade:

```text
competitionId + categoryId + robotId
```

---

# FOLLOW_LINE

## 11. Tentativa válida

```http
POST /api/v1/tentativas-seguidor-linha
```

```json
{
  "registrationId": 1,
  "tomada": 1,
  "numeroTentativa": 1,
  "tempoSegundos": 42.315,
  "checkpointsAlcancados": 5,
  "penalidadeSegundos": 0,
  "concluida": true,
  "valida": true,
  "observacao": "Primeira tentativa"
}
```

## 12. Tentativa com penalidade

```json
{
  "registrationId": 1,
  "tomada": 1,
  "numeroTentativa": 2,
  "tempoSegundos": 40.870,
  "checkpointsAlcancados": 5,
  "penalidadeSegundos": 2,
  "concluida": true,
  "valida": true,
  "observacao": "Tempo bruto menor, mas com penalidade"
}
```

Nesse exemplo:

```text
42.315 + 0 = 42.315
40.870 + 2 = 42.870
```

A primeira tentativa continua sendo a melhor.

## 13. Ranking

```http
GET /api/v1/ranking/seguidor-linha?competitionId=1&categoryId=3
```

Resposta conceitual:

```json
[
  {
    "posicao": 1,
    "registrationId": 1,
    "robotId": 1,
    "robotNome": "Vespa",
    "teamNome": "RAS UFRB",
    "tempoBrutoSegundos": 42.315,
    "penalidadeSegundos": 0,
    "tempoFinalSegundos": 42.315,
    "tomada": 1,
    "numeroTentativa": 1
  }
]
```

### O que NÃO fazer no Follow

```http
POST /api/v1/chaveamentos/gerar?competitionId=1&categoryId=3
```

Esse request deve ser rejeitado porque `FOLLOW_LINE` não usa chaveamento.

---

# SUMO

## 14. Inspeção aprovada

```http
POST /api/v1/inspecoes-sumo
```

```json
{
  "registrationId": 7,
  "pesoMedido": 0.450,
  "observacao": "Dentro do limite"
}
```

O backend calcula `numeroTentativa` e `aprovada`.

## 15. Inspeção reprovada

```json
{
  "registrationId": 8,
  "pesoMedido": 0.600,
  "observacao": "Acima do limite"
}
```

Ao atingir o limite de reprovações configurado, a inscrição passa para `DESCLASSIFICADA`.

## 16. Aptidão

```http
GET /api/v1/inspecoes-sumo/aptidao?registrationId=7
```

Resposta:

```json
true
```

---

## 17. Gerar chaveamento Sumô

Não possui body:

```http
POST /api/v1/chaveamentos/gerar?competitionId=2&categoryId=1
```

Só entram inscrições:

```text
ativas
+ APROVADA
+ aptas para competir
```

## 18. Bracket manual — somente SUMO

```http
POST /api/v1/chaveamentos
```

```json
{
  "competitionId": 2,
  "categoryId": 1,
  "nome": "Chave Mini Sumô - RRC 2026",
  "status": "RASCUNHO",
  "ativo": true
}
```

## 19. Match manual — somente SUMO

A geração automática normalmente cria as partidas, mas o CRUD aceita manutenção administrativa dentro das regras do domínio.

```http
POST /api/v1/partidas
```

```json
{
  "bracketId": 5,
  "rodada": 1,
  "ordem": 1,
  "registrationAId": 7,
  "registrationBId": 8,
  "dataHora": "2026-09-05T09:00:00",
  "status": "AGENDADA",
  "ativo": true
}
```

Os participantes precisam estar aptos no Sumô.

---

## 20. Round finalizado

```http
POST /api/v1/rounds-sumo
```

```json
{
  "matchId": 20,
  "winnerRegistrationId": 7,
  "status": "FINALIZADO",
  "observacao": "Vitória no round 1"
}
```

## 21. Round sem vencedor

```json
{
  "matchId": 20,
  "winnerRegistrationId": null,
  "status": "EMPATADO",
  "observacao": "Round empatado"
}
```

Também existem:

```text
ANULADO
CANCELADO
```

## 22. MatchResult automático

Não existe POST de resultado no contrato externo atual.

Após atingir `roundsParaVencer`, consulte:

```http
GET /api/v1/resultados-partida/por-partida?matchId=20
```

Resposta conceitual:

```json
{
  "id": 9,
  "matchId": 20,
  "winnerRegistrationId": 7,
  "winnerRobotNome": "Postman Sumo A",
  "pontosA": 2,
  "pontosB": 0,
  "observacao": "Resultado consolidado automaticamente pelos rounds do Sumô."
}
```

`pontosA` e `pontosB` representam vitórias em rounds.

---

## 23. Consultas úteis

```http
GET /api/v1/categorias/por-modalidade?modalidade=FOLLOW_LINE
GET /api/v1/categorias/por-modalidade?modalidade=SUMO
GET /api/v1/inscricoes/por-competicao?competitionId={id}

GET /api/v1/tentativas-seguidor-linha/por-inscricao?registrationId={id}
GET /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}

GET /api/v1/inspecoes-sumo/por-inscricao?registrationId={id}
GET /api/v1/inspecoes-sumo/aptidao?registrationId={id}
GET /api/v1/chaveamentos/por-competicao?competitionId={id}
GET /api/v1/partidas/por-chaveamento?bracketId={id}
GET /api/v1/rounds-sumo/por-partida?matchId={id}
GET /api/v1/resultados-partida/por-partida?matchId={id}
```

---

## 24. Ordem prática por modalidade

### Follow

```text
Competition + Category FOLLOW_LINE
-> Registration APROVADA
-> TentativaSeguidorLinha
-> RankingFollow
-> campeão = menor tempo final
```

### Sumô

```text
Competition + Category SUMO
-> Registration APROVADA
-> InspecaoSumo
-> aptidão
-> Bracket
-> Match
-> RoundSumo
-> MatchResult automático
-> progressão
```
