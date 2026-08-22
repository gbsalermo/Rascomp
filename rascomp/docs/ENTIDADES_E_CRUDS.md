# Entidades e CRUDs — Rascomp

Mapa rápido do papel de cada entidade e de como ela participa dos dois fluxos competitivos.

## Visão geral

```text
Institution
  ↓
Team
  ├─ Competitor
  └─ Robot

Competition
  ↓
CompetitionCategory
  ↓
Registration
  ├──────── FOLLOW_LINE ────────┐
  │                              ↓
  │                    TentativaSeguidorLinha
  │                              ↓
  │                       RankingFollowService
  │
  └────────── SUMO ─────────────┐
                                 ↓
                           InspecaoSumo
                                 ↓
                              Bracket
                                 ↓
                               Match
                                 ↓
                            RoundSumo
                                 ↓
                         MatchResult automático
```

---

## 1. Institution

Representa a instituição de origem da equipe.

Exemplos:

```text
UFRB
IFBA
```

Relação:

```text
Institution 1 -> N Team
```

CRUD:

- criar;
- listar;
- buscar por ID/sigla;
- atualizar;
- inativar;
- reativar.

Regra: `sigla` é única.

---

## 2. Team

Representa a equipe participante.

Relações:

```text
Team N -> 1 Institution
Team 1 -> N Competitor
Team 1 -> N Robot
Team 1 -> N Registration
```

CRUD completo com inativação lógica e reativação.

---

## 3. Competitor

Representa uma pessoa integrante da equipe.

Relação:

```text
Competitor N -> 1 Team
```

Regra: `email` é único.

No modelo atual, o participante competitivo direto é o `Robot` por meio da `Registration`.

---

## 4. Robot

Representa o robô físico.

Relações:

```text
Robot N -> 1 Team
Robot 1 -> N Registration
```

Regra:

```text
nome + team
```

é único.

---

## 5. Competition

Representa o evento competitivo.

Campos centrais:

```text
inicioInscricoes
fimInscricoes
dataInicio
dataFim
status
ativo
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

Relações principais:

```text
Competition 1 -> N Registration
Competition 1 -> N Bracket (somente categorias SUMO)
```

---

## 6. CompetitionCategory

Representa uma categoria e define a modalidade.

```text
Mini Sumô       -> SUMO
Sumô 3 kg       -> SUMO
Seguidor Linha  -> FOLLOW_LINE
```

Relações:

```text
CompetitionCategory 1 -> 0..1 ConfigSumo
CompetitionCategory 1 -> 0..1 ConfigFollow
CompetitionCategory 1 -> N Registration
```

`Bracket` só é válido quando a categoria é `SUMO`.

---

## 7. ConfigFollow

Configuração específica do Seguidor de Linha.

Campos:

```text
numeroTomadas
tentativasPorTomada
maxTempoSegundos
numeroCheckpoints
```

Relação:

```text
ConfigFollow 1 <-> 1 CompetitionCategory
```

Usada por `TentativaSeguidorLinhaService`.

---

## 8. ConfigSumo

Configuração específica do Sumô.

Campos:

```text
pesoMax
exigeInspecao
maxTentativasInspecao
numeroRounds
roundsParaVencer
permiteRoundDesempate
```

Usada por:

```text
InspecaoSumoService
BracketGenerationService
RoundSumoService
```

---

## 9. Registration

Representa a inscrição de um robô em uma categoria de uma competição.

Relações:

```text
Registration N -> 1 Competition
Registration N -> 1 CompetitionCategory
Registration N -> 1 Team
Registration N -> 1 Robot
```

Status:

```text
PENDENTE
APROVADA
REJEITADA
CANCELADA
DESCLASSIFICADA
```

Regra de unicidade:

```text
competition + category + robot
```

É o ponto de entrada para ambos os fluxos competitivos.

No planejamento do Camunda, `Registration` é a primeira entidade candidata a processo BPMN:

```text
PENDENTE -> análise -> APROVADA/REJEITADA
```

---

# FOLLOW_LINE

## 10. TentativaSeguidorLinha

Representa uma tentativa individual de uma inscrição Follow.

Relação:

```text
TentativaSeguidorLinha N -> 1 Registration
```

Campos relevantes:

```text
tomada
numeroTentativa
tempoSegundos
checkpointsAlcancados
penalidadeSegundos
concluida
valida
```

CRUD:

- criar;
- buscar;
- listar por inscrição;
- atualizar;
- excluir.

Regra de unicidade:

```text
registration + tomada + numeroTentativa
```

Tempo acima do máximo é persistido com `valida=false`.

---

## 11. RankingFollowService

Não é entidade JPA.

Responsabilidade:

```text
pegar inscrições Follow ativas/aprovadas
-> filtrar tentativas válidas e concluídas
-> calcular tempo final
-> escolher melhor tentativa de cada inscrição
-> ordenar ranking
```

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

O menor tempo final ocupa a primeira posição.

### O que não pertence ao Follow

```text
Bracket
Match
MatchResult
RoundSumo
```

---

# SUMO

## 12. InspecaoSumo

Representa uma tentativa de inspeção de uma inscrição Sumô.

Relação:

```text
InspecaoSumo N -> 1 Registration
```

O service calcula:

```text
numeroTentativa
aprovada
```

Regras:

- inscrição precisa ser SUMO, ativa e `APROVADA`;
- peso é comparado a `ConfigSumo.pesoMax`;
- inspeção aprovada encerra novas tentativas;
- última reprovação permitida leva a `DESCLASSIFICADA`.

Também fornece a consulta de **aptidão para competir**.

---

## 13. Bracket

Representa a chave eliminatória inteira de uma categoria `SUMO` dentro de uma competição.

Relações:

```text
Bracket N -> 1 Competition
Bracket N -> 1 CompetitionCategory SUMO
Bracket 1 -> N Match
```

Regra:

```text
competition + category
```

é único.

O CRUD manual existe para administração, mas o fluxo principal usa `BracketGenerationService`.

O backend rejeita Bracket em `FOLLOW_LINE`.

---

## 14. BracketGenerationService

Não é entidade JPA.

Responsabilidade:

```text
inscrições SUMO ativas + APROVADA + aptas
-> sorteio
-> próxima potência de 2
-> BYEs
-> árvore completa de partidas
```

Com 3 participantes aptos:

```text
chave de 4
2 partidas na rodada 1
1 BYE
1 final
```

---

## 15. Match

Representa uma partida dentro do bracket Sumô.

Relações:

```text
Match N -> 1 Bracket
Match -> Registration A
Match -> Registration B
Match 1 -> N RoundSumo
Match 1 -> 0..1 MatchResult
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

Participantes precisam estar ativos, aprovados, pertencer à mesma competição/categoria e estar aptos na inspeção quando exigida.

---

## 16. RoundSumo

Representa um round individual de uma `Match` Sumô.

Relações:

```text
RoundSumo N -> 1 Match
RoundSumo N -> 0..1 Registration vencedora
```

Status:

```text
FINALIZADO
EMPATADO
ANULADO
CANCELADO
```

Somente `FINALIZADO` com vencedor conta como vitória.

Ao atingir `roundsParaVencer`, o service dispara a consolidação do resultado.

---

## 17. MatchResult

Representa o resultado consolidado de uma partida Sumô.

Relações:

```text
MatchResult 1 <-> 1 Match
MatchResult N -> 0..1 Registration vencedora
```

No domínio atual ele é **automático e somente leitura pela API externa**.

```text
RoundSumo
-> contagem de vitórias
-> MatchResult automático
-> Match FINALIZADA
-> avanço do vencedor
```

`pontosA` e `pontosB` representam vitórias em rounds.

Não há POST/PUT/DELETE público para `MatchResult` no contrato atual.

---

## 18. BracketProgressionService

Não é entidade JPA.

Responsabilidade:

```text
vencedor da Match
-> slot correto da próxima Match
-> AGENDADA quando os dois lados chegam
```

Também:

- avança BYEs automaticamente;
- finaliza o bracket quando a final termina.

---

## 19. Camunda

Não é entidade do domínio.

Estado atual:

```text
Process Engine  OK
JobExecutor     OK
ACT_* no MySQL  OK
BPMN Rascomp    pendente
```

A função futura é **orquestrar processos de negócio**, começando pela análise administrativa de inscrições.

---

## 20. Resumo mental

```text
Institution = origem institucional
Team        = equipe
Competitor  = pessoa da equipe
Robot       = robô
Competition = evento
Category    = regra/modalidade
Registration = robô inscrito

FOLLOW_LINE:
TentativaSeguidorLinha = execução individual
RankingFollowService   = classificação final

SUMO:
InspecaoSumo = aptidão
Bracket      = chave inteira
Match        = confronto
RoundSumo    = round do confronto
MatchResult  = resultado automático da partida
```
