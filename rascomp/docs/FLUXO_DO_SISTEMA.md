# Fluxo do Sistema — Rascomp

Este documento descreve o fluxo funcional do Rascomp do cadastro à classificação final.

## 1. Estrutura base

```text
Institution
   ↓
Team
   ├─ Competitor
   └─ Robot

Competition
   ↓
CompetitionCategory
   ├─ SUMO        -> ConfigSumo
   └─ FOLLOW_LINE -> ConfigFollow
```

A inscrição relaciona:

```text
Competition + CompetitionCategory + Team + Robot
```

## 2. Registration

```text
competição ativa
+ período de inscrição válido
+ categoria ativa
+ equipe ativa
+ robô ativo da equipe
        ↓
   Registration
        ↓
     PENDENTE
```

Status:

```text
PENDENTE
APROVADA
REJEITADA
CANCELADA
DESCLASSIFICADA
```

A mesma combinação `competition + category + robot` não pode ser repetida.

---

# FOLLOW_LINE

## 3. Configuração

`ConfigFollow` define:

```text
numeroTomadas
tentativasPorTomada
maxTempoSegundos
numeroCheckpoints
```

Configuração de referência atual:

```text
3 tomadas
3 tentativas por tomada
180 s de tempo máximo
5 checkpoints
```

## 4. Execução da prova

```text
Registration APROVADA
        ↓
TentativaSeguidorLinha
        ↓
validação de tomada / tentativa / checkpoints / tempo
        ↓
armazenamento do resultado bruto
```

Cada tentativa registra:

```text
tomada
numeroTentativa
tempoSegundos
checkpointsAlcancados
penalidadeSegundos
concluida
valida
```

Tempo acima do limite pode ser persistido, mas fica `valida=false`.

## 5. Ranking

```text
inscrições ativas + APROVADA
        ↓
tentativas válidas + concluídas
        ↓
melhor tentativa de cada inscrição
        ↓
RankingFollowService
        ↓
menor tempo final vence
```

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

Critérios de ordenação:

```text
1. menor tempo final
2. menor tempo bruto
3. menor registrationId
```

### Regra estrutural

`FOLLOW_LINE` **não usa confronto direto**.

Logo não utiliza:

```text
Bracket
Match
MatchResult
RoundSumo
```

O backend rejeita tentativa de criar ou gerar chaveamento para categoria `FOLLOW_LINE`.

Fluxo resumido:

```text
Registration
   ↓
Tentativas
   ↓
Ranking
   ↓
1º lugar = menor tempo final
```

---

# SUMO

## 6. Configuração

`ConfigSumo` define:

```text
pesoMax
exigeInspecao
maxTentativasInspecao
numeroRounds
roundsParaVencer
permiteRoundDesempate
```

## 7. Inspeção

```text
Registration APROVADA
        ↓
InspecaoSumo
        ↓
pesoMedido <= pesoMax ?
    ↙               ↘
  sim               não
   ↓                 ↓
apta          nova tentativa
                     ↓
             atingiu limite?
                ↙       ↘
              não       sim
                        ↓
                DESCLASSIFICADA
```

Quando `exigeInspecao=false`, inscrição ativa e aprovada já é apta.

## 8. Geração do chaveamento

```text
inscrições SUMO
+ ativas
+ APROVADA
+ aptas
       ↓
BracketGenerationService
       ↓
Bracket
       ↓
árvore completa de Match
```

Regras:

- mínimo de 2 participantes aptos;
- próxima potência de 2 define a chave;
- participantes são embaralhados;
- BYEs completam posições vazias;
- todas as rodadas são criadas;
- rodadas futuras começam `AGUARDANDO_PARTICIPANTES`.

Exemplo com três participantes aptos:

```text
3 participantes
      ↓
chave de 4
      ↓
2 partidas na rodada 1
1 BYE
1 final
```

## 9. Match

Cada `Match` pertence a um `Bracket` SUMO.

Os participantes precisam:

```text
estar ativos
estar APROVADA
pertencer à competição/categoria do bracket
estar aptos no Sumô
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

## 10. BYE e progressão

```text
Match BYE
   ↓
único participante avança automaticamente
   ↓
próxima Match
```

Para resultados normais:

```text
Match finalizada
   ↓
winner
   ↓
BracketProgressionService
   ↓
próxima partida
```

Posicionamento atual:

```text
ordem ímpar -> registrationA
ordem par   -> registrationB
próxima ordem = (ordem + 1) / 2
```

Na final, o vencedor encerra o `Bracket` como `FINALIZADO`.

## 11. Rounds do Sumô

```text
Match AGENDADA
   ↓
RoundSumo #1
   ↓
RoundSumo #2
   ↓
...
   ↓
contagem de vitórias
```

Status de round:

```text
FINALIZADO
EMPATADO
ANULADO
CANCELADO
```

Somente `FINALIZADO` com vencedor conta como vitória.

## 12. MatchResult automático

Quando alguém atinge `roundsParaVencer`:

```text
RoundSumo
   ↓
MatchResultService.criarAutomaticoSumo
   ↓
MatchResult
   ↓
Match FINALIZADA
   ↓
BracketProgressionService
```

`pontosA` e `pontosB` representam vitórias em rounds.

A API externa de `MatchResult` é somente leitura. Não existe criação manual de resultado no contrato atual.

---

# CAMUNDA

## 13. Estado atual

O Camunda 7 está operacional como infraestrutura:

```text
Process Engine     OK
JobExecutor        OK
tabelas ACT_*      OK
mesmo MySQL        OK
```

As regras de competição ainda são executadas pelos Services Java.

## 14. Integração planejada

Depois do primeiro fluxo funcional do Frontend de Gestão:

```text
Registration PENDENTE
        ↓
processo BPMN
        ↓
tarefa administrativa
    ↙             ↘
APROVADA       REJEITADA
```

Camunda deve orquestrar processos, não substituir regras de domínio como validação de inscrição, ranking ou aptidão.

---

# FRONTENDS

## 15. Gestão

Fluxos principais:

```text
cadastros
-> inscrições
-> aprovação/rejeição
-> FOLLOW_LINE: tentativas e ranking
-> SUMO: inspeção, chave, partidas e rounds
```

## 16. Público

```text
competições
-> categorias
-> equipes/robôs
-> FOLLOW_LINE: ranking
-> SUMO: chaveamento, partidas e resultados
```

---

## Fluxo completo resumido

```text
Cadastros
   ↓
Competition + Category
   ↓
Registration
   ↓
APROVADA
   ├──────────────── FOLLOW_LINE
   │                     ↓
   │                 Tentativas
   │                     ↓
   │                  Ranking
   │                     ↓
   │            menor tempo = campeão
   │
   └──────────────── SUMO
                         ↓
                     Inspeção
                         ↓
                       Apta
                         ↓
                      Bracket
                         ↓
                       Match
                         ↓
                     RoundSumo
                         ↓
                MatchResult automático
                         ↓
                      avanço
                         ↓
                      campeão
```
