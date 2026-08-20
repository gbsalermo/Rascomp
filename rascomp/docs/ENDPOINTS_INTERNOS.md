# Endpoints Internos — Rascomp

Base local:

```text
http://localhost:8080
```

Prefixo da API de negócio:

```text
/api/v1
```

> Regra central: `FOLLOW_LINE` é disputado por tentativas/ranking. `SUMO` usa inspeção, chaveamento, partidas e rounds.

---

## Categorias

Base: `/api/v1/categorias`

| Método | Endpoint | Função |
|---|---|---|
| POST | `/api/v1/categorias` | Cria categoria. |
| GET | `/api/v1/categorias` | Lista categorias. |
| GET | `/api/v1/categorias/{id}` | Busca por ID. |
| GET | `/api/v1/categorias/por-modalidade?modalidade=SUMO` | Lista por modalidade. |
| GET | `/api/v1/categorias/por-modalidade/ativas?modalidade=FOLLOW_LINE` | Lista ativas da modalidade. |
| PUT | `/api/v1/categorias/{id}` | Atualiza. |
| DELETE | `/api/v1/categorias/{id}` | Inativa. |

Modalidades:

```text
SUMO
FOLLOW_LINE
```

---

## Configuração de Sumô

Base: `/api/v1/categorias/{categoryId}/config-sumo`

```text
POST   cria
GET    consulta
PUT    atualiza
DELETE remove
```

Responsável por:

```text
pesoMax
exigeInspecao
maxTentativasInspecao
numeroRounds
roundsParaVencer
permiteRoundDesempate
```

---

## Configuração de Follow

Base: `/api/v1/categorias/{categoryId}/config-follow`

```text
POST   cria
GET    consulta
PUT    atualiza
DELETE remove
```

Responsável por:

```text
numeroTomadas
tentativasPorTomada
maxTempoSegundos
numeroCheckpoints
```

---

## Instituições

Base: `/api/v1/instituicoes`

```http
POST   /api/v1/instituicoes
GET    /api/v1/instituicoes
GET    /api/v1/instituicoes/{id}
GET    /api/v1/instituicoes/por-sigla?sigla={sigla}
PUT    /api/v1/instituicoes/{id}
DELETE /api/v1/instituicoes/{id}
PATCH  /api/v1/instituicoes/{id}/reativar
```

---

## Equipes

Base: `/api/v1/equipes`

```http
POST   /api/v1/equipes
GET    /api/v1/equipes
GET    /api/v1/equipes/{id}
GET    /api/v1/equipes/por-instituicao?institutionId={id}
PUT    /api/v1/equipes/{id}
DELETE /api/v1/equipes/{id}
PATCH  /api/v1/equipes/{id}/reativar
```

---

## Competidores

Base: `/api/v1/competidores`

```http
POST   /api/v1/competidores
GET    /api/v1/competidores
GET    /api/v1/competidores/{id}
GET    /api/v1/competidores/por-email?email={email}
GET    /api/v1/competidores/por-equipe?teamId={id}
PUT    /api/v1/competidores/{id}
DELETE /api/v1/competidores/{id}
PATCH  /api/v1/competidores/{id}/reativar
```

---

## Robôs

Base: `/api/v1/robos`

```http
POST   /api/v1/robos
GET    /api/v1/robos
GET    /api/v1/robos/{id}
GET    /api/v1/robos/por-equipe?teamId={id}
PUT    /api/v1/robos/{id}
DELETE /api/v1/robos/{id}
PATCH  /api/v1/robos/{id}/reativar
```

Regra: `nome + equipe` deve ser único.

---

## Competições

Base: `/api/v1/competicoes`

```http
POST   /api/v1/competicoes
GET    /api/v1/competicoes
GET    /api/v1/competicoes/{id}
GET    /api/v1/competicoes/por-status?status={status}
PUT    /api/v1/competicoes/{id}
DELETE /api/v1/competicoes/{id}
PATCH  /api/v1/competicoes/{id}/reativar
```

Status:

```text
PLANEJADA
INSCRICOES_ABERTAS
INSCRICOES_ENCERRADAS
EM_ANDAMENTO
FINALIZADA
CANCELADA
```

---

## Inscrições

Base: `/api/v1/inscricoes`

```http
POST   /api/v1/inscricoes
GET    /api/v1/inscricoes
GET    /api/v1/inscricoes/{id}
GET    /api/v1/inscricoes/por-competicao?competitionId={id}
GET    /api/v1/inscricoes/por-status?status={status}
PUT    /api/v1/inscricoes/{id}
DELETE /api/v1/inscricoes/{id}
PATCH  /api/v1/inscricoes/{id}/reativar
```

Status:

```text
PENDENTE
APROVADA
REJEITADA
CANCELADA
DESCLASSIFICADA
```

Unicidade:

```text
competitionId + categoryId + robotId
```

---

# FOLLOW_LINE

## Tentativas

Base: `/api/v1/tentativas-seguidor-linha`

```http
POST   /api/v1/tentativas-seguidor-linha
GET    /api/v1/tentativas-seguidor-linha/{id}
GET    /api/v1/tentativas-seguidor-linha/por-inscricao?registrationId={id}
PUT    /api/v1/tentativas-seguidor-linha/{id}
DELETE /api/v1/tentativas-seguidor-linha/{id}
```

O service valida os limites definidos em `ConfigFollow`.

## Ranking

```http
GET /api/v1/ranking/seguidor-linha?competitionId={id}&categoryId={id}
```

O ranking considera a melhor tentativa válida e concluída de cada inscrição ativa e aprovada.

```text
tempoFinal = tempoBruto + penalidade
```

**Não existem endpoints de Bracket/Match/MatchResult válidos para FOLLOW_LINE.**

A tentativa de gerar chaveamento para uma categoria Follow deve retornar `400`.

---

# SUMO

## Inspeções

Base: `/api/v1/inspecoes-sumo`

```http
POST /api/v1/inspecoes-sumo
GET  /api/v1/inspecoes-sumo/{id}
GET  /api/v1/inspecoes-sumo/por-inscricao?registrationId={id}
GET  /api/v1/inspecoes-sumo/ultima?registrationId={id}
GET  /api/v1/inspecoes-sumo/aptidao?registrationId={id}
```

A última reprovação permitida pode alterar a inscrição para `DESCLASSIFICADA`.

## Chaveamentos

Base: `/api/v1/chaveamentos`

```http
POST   /api/v1/chaveamentos
POST   /api/v1/chaveamentos/gerar?competitionId={id}&categoryId={id}
GET    /api/v1/chaveamentos
GET    /api/v1/chaveamentos/{id}
GET    /api/v1/chaveamentos/por-competicao?competitionId={id}
PUT    /api/v1/chaveamentos/{id}
DELETE /api/v1/chaveamentos/{id}
PATCH  /api/v1/chaveamentos/{id}/reativar
```

Regras:

- categoria deve ser `SUMO`;
- só entram inscrições ativas e `APROVADA`;
- a inscrição precisa estar apta para competir;
- apenas um bracket por `competição + categoria`;
- BYEs avançam automaticamente.

## Partidas

Base: `/api/v1/partidas`

```http
POST   /api/v1/partidas
GET    /api/v1/partidas
GET    /api/v1/partidas/{id}
GET    /api/v1/partidas/por-chaveamento?bracketId={id}
PUT    /api/v1/partidas/{id}
DELETE /api/v1/partidas/{id}
PATCH  /api/v1/partidas/{id}/reativar
```

Status:

```text
AGUARDANDO_PARTICIPANTES
AGENDADA
EM_ANDAMENTO
FINALIZADA
CANCELADA
BYE
```

## Rounds

Base: `/api/v1/rounds-sumo`

```http
POST /api/v1/rounds-sumo
GET  /api/v1/rounds-sumo/{id}
GET  /api/v1/rounds-sumo/por-partida?matchId={id}
```

Status:

```text
FINALIZADO
EMPATADO
ANULADO
CANCELADO
```

Ao atingir `roundsParaVencer`, o backend cria o resultado da partida automaticamente.

## Resultados de partida

Base: `/api/v1/resultados-partida`

A API é **somente leitura**:

```http
GET /api/v1/resultados-partida
GET /api/v1/resultados-partida/{id}
GET /api/v1/resultados-partida/por-partida?matchId={id}
GET /api/v1/resultados-partida/por-chaveamento?bracketId={id}
GET /api/v1/resultados-partida/por-competicao?competitionId={id}
```

No Sumô, `MatchResult` nasce automaticamente da consolidação dos rounds.

`POST`, `PUT` e `DELETE` não fazem parte do contrato atual e devem resultar em `405 Method Not Allowed`.

---

## Tratamento de erros

Contrato pretendido:

```text
400 regra de negócio / validação / parâmetro inválido
404 recurso não encontrado
405 método HTTP não suportado
409 conflito de integridade
500 erro realmente inesperado
```

---

## Fluxos consumidos pelos frontends

### Gestão

```text
cadastros
-> inscrições
-> aprovação/rejeição
-> Follow: lançamento de tentativas/ranking
-> Sumô: inspeção/chave/rounds
```

### Público

```text
competições
-> equipes/robôs
-> Follow: ranking
-> Sumô: chave/partidas/resultados
```

Swagger/OpenAPI será consolidado depois do congelamento desse contrato.
