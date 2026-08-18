# Testes Postman — Rascomp

Última atualização: 2026-08-18

## 1. Objetivo

Bateria manual final do backend Rascomp antes de congelar o contrato da API e iniciar os dois frontends.

Base URL:

```text
http://localhost:8080
```

Header para requests com body:

```text
Content-Type: application/json
```

Os IDs abaixo são referências. Sempre prefira os IDs retornados pela sua execução.

---

## 2. Pré-teste de infraestrutura — JÁ VALIDADO EM 18/08

Status atual:

- MySQL 9.6 conectado;
- Flyway executou V1, V2 e V3 com `success=1`;
- Hibernate validou o schema com `ddl-auto=validate`;
- Camunda criou o Process Engine e as tabelas `ACT_*`;
- Tomcat iniciou em `8080`;
- reinicialização reaproveitou o mesmo banco persistente.

Para iniciar localmente sem redefinir `$env:DB_PASSWORD` a cada terminal:

```powershell
.\run-local.ps1
```

Na primeira execução o script cria `.env.local`, ignorado pelo Git, e pede usuário/senha do MySQL. Nas próximas execuções basta usar o mesmo comando.

---

## 3. Dados iniciais úteis

Em banco novo, o `DataInitializer` cria o cenário RRC 2026 e depois passa a ignorar novas cargas se já existirem categorias.

IDs mostrados no primeiro startup:

```text
Categoria Seguidor de Linha: 3
Instituição UFRB: 1
Instituição IFBA: 2
Equipe RAS UFRB: 1
Equipe Robótica IFBA: 2
Competidor Gabriel: 1
Competidor Ana: 2
Robô Vespa: 1
Robô Trilho: 2
Competição RRC 2026: 1
Inscrição Vespa: 1
Inscrição Trilho: 2
Bracket: 1
Match: 1
MatchResult: 1
```

Para descobrir os IDs atuais das categorias de Sumô:

```http
GET /api/v1/categorias/por-modalidade?modalidade=SUMO
```

Use uma categoria ativa com `ConfigSumo` nos testes de Sumô.

---

# PARTE A — CRUDs e contrato básico

## 4. CompetitionCategory

Base: `/api/v1/categorias`

Criar:

```http
POST /api/v1/categorias
```

```json
{
  "nome": "Seguidor de Linha Teste",
  "descricao": "Categoria de teste",
  "modalidade": "FOLLOW_LINE",
  "ativo": true
}
```

Esperado: `201`.

Consultas:

```http
GET /api/v1/categorias
GET /api/v1/categorias/{id}
GET /api/v1/categorias/por-modalidade?modalidade=FOLLOW_LINE
GET /api/v1/categorias/por-modalidade/ativas?modalidade=FOLLOW_LINE
```

Atualizar/excluir:

```http
PUT /api/v1/categorias/{id}
DELETE /api/v1/categorias/{id}
```

---

## 5. ConfigSumo

Base: `/api/v1/categorias/{categoryId}/config-sumo`

```http
POST /api/v1/categorias/{categoryId}/config-sumo
GET  /api/v1/categorias/{categoryId}/config-sumo
PUT  /api/v1/categorias/{categoryId}/config-sumo
DELETE /api/v1/categorias/{categoryId}/config-sumo
```

Body:

```json
{
  "pesoMax": 3.000,
  "exigeInspecao": true,
  "maxTentativasInspecao": 3,
  "numeroRounds": 3,
  "roundsParaVencer": 2,
  "permiteRoundDesempate": true
}
```

---

## 6. ConfigFollow

Base: `/api/v1/categorias/{categoryId}/config-follow`

```http
POST /api/v1/categorias/{categoryId}/config-follow
GET  /api/v1/categorias/{categoryId}/config-follow
PUT  /api/v1/categorias/{categoryId}/config-follow
DELETE /api/v1/categorias/{categoryId}/config-follow
```

Body:

```json
{
  "numeroTomadas": 3,
  "tentativasPorTomada": 3,
  "maxTempoSegundos": 180,
  "numeroCheckpoints": 5
}
```

---

## 7. Institution

Base: `/api/v1/instituicoes`

Criar:

```http
POST /api/v1/instituicoes
```

```json
{
  "nome": "Universidade Teste",
  "sigla": "UT",
  "cidade": "Cruz das Almas",
  "estado": "BA",
  "ativo": true
}
```

Consultas e ciclo:

```http
GET /api/v1/instituicoes
GET /api/v1/instituicoes?apenasAtivas=true
GET /api/v1/instituicoes/{id}
GET /api/v1/instituicoes/por-sigla?sigla=UT
PUT /api/v1/instituicoes/{id}
DELETE /api/v1/instituicoes/{id}
PATCH /api/v1/instituicoes/{id}/reativar
```

---

## 8. Team

Base: `/api/v1/equipes`

Criar:

```json
{
  "nome": "Equipe Teste",
  "institutionId": 1,
  "ativo": true
}
```

Rotas:

```http
POST /api/v1/equipes
GET /api/v1/equipes
GET /api/v1/equipes?apenasAtivas=true
GET /api/v1/equipes/{id}
GET /api/v1/equipes/por-instituicao?institutionId=1
GET /api/v1/equipes/por-instituicao?institutionId=1&apenasAtivas=true
PUT /api/v1/equipes/{id}
DELETE /api/v1/equipes/{id}
PATCH /api/v1/equipes/{id}/reativar
```

---

## 9. Competitor

Base: `/api/v1/competidores`

Criar:

```json
{
  "nome": "Competidor Teste",
  "email": "competidor.teste@rascomp.dev",
  "telefone": "75999999999",
  "teamId": 1,
  "ativo": true
}
```

Rotas:

```http
POST /api/v1/competidores
GET /api/v1/competidores
GET /api/v1/competidores?apenasAtivos=true
GET /api/v1/competidores/{id}
GET /api/v1/competidores/por-email?email=competidor.teste@rascomp.dev
GET /api/v1/competidores/por-equipe?teamId=1
GET /api/v1/competidores/por-equipe?teamId=1&apenasAtivos=true
PUT /api/v1/competidores/{id}
DELETE /api/v1/competidores/{id}
PATCH /api/v1/competidores/{id}/reativar
```

---

## 10. Robot

Base: `/api/v1/robos`

Criar:

```json
{
  "nome": "Robot Teste",
  "descricao": "Robô de teste",
  "teamId": 1,
  "ativo": true
}
```

Rotas:

```http
POST /api/v1/robos
GET /api/v1/robos
GET /api/v1/robos?apenasAtivos=true
GET /api/v1/robos/{id}
GET /api/v1/robos/por-equipe?teamId=1
GET /api/v1/robos/por-equipe?teamId=1&apenasAtivos=true
PUT /api/v1/robos/{id}
DELETE /api/v1/robos/{id}
PATCH /api/v1/robos/{id}/reativar
```

---

## 11. Competition

Base: `/api/v1/competicoes`

Criar:

```json
{
  "nome": "RRC Teste",
  "descricao": "Competição para bateria final",
  "inicioInscricoes": "2026-08-01",
  "fimInscricoes": "2026-08-31",
  "dataInicio": "2026-09-05",
  "dataFim": "2026-09-06",
  "status": "INSCRICOES_ABERTAS",
  "ativo": true
}
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

Rotas:

```http
POST /api/v1/competicoes
GET /api/v1/competicoes
GET /api/v1/competicoes?apenasAtivas=true
GET /api/v1/competicoes/{id}
GET /api/v1/competicoes/por-status?status=INSCRICOES_ABERTAS
PUT /api/v1/competicoes/{id}
DELETE /api/v1/competicoes/{id}
PATCH /api/v1/competicoes/{id}/reativar
```

---

## 12. Registration

Base: `/api/v1/inscricoes`

Criar:

```json
{
  "competitionId": 1,
  "categoryId": 3,
  "teamId": 1,
  "robotId": 1,
  "status": "APROVADA",
  "observacao": "Inscrição de teste",
  "ativo": true
}
```

Status atuais:

```text
PENDENTE
APROVADA
REJEITADA
CANCELADA
DESCLASSIFICADA
```

Rotas:

```http
POST /api/v1/inscricoes
GET /api/v1/inscricoes
GET /api/v1/inscricoes?apenasAtivas=true
GET /api/v1/inscricoes/{id}
GET /api/v1/inscricoes/por-competicao?competitionId=1
GET /api/v1/inscricoes/por-status?status=APROVADA
PUT /api/v1/inscricoes/{id}
DELETE /api/v1/inscricoes/{id}
PATCH /api/v1/inscricoes/{id}/reativar
```

Negativo obrigatório: repetir `competitionId + categoryId + robotId`. Deve ser rejeitado.

---

# PARTE B — Seguidor de Linha

## 13. TentativaSeguidorLinha + ConfigFollow

Base: `/api/v1/tentativas-seguidor-linha`

Criar válida:

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

Consultas/ciclo:

```http
GET /api/v1/tentativas-seguidor-linha/{id}
GET /api/v1/tentativas-seguidor-linha/por-inscricao?registrationId=1
PUT /api/v1/tentativas-seguidor-linha/{id}
DELETE /api/v1/tentativas-seguidor-linha/{id}
```

Com ConfigFollow 3 tomadas / 3 tentativas / 180 segundos / 5 checkpoints:

- `tomada=4` -> rejeitar;
- `numeroTentativa=4` -> rejeitar;
- `checkpointsAlcancados=6` -> rejeitar;
- `tempoSegundos=181.500` -> persistir tentativa, porém `valida=false`.

---

## 14. Ranking Seguidor de Linha

```http
GET /api/v1/ranking/seguidor-linha?competitionId=1&categoryId=3
```

Esperado: `200` e lista ordenada.

No cenário inicial da Vespa:

```text
Tentativa 1: 42.315 + 0 = 42.315
Tentativa 2: 40.870 + 2 = 42.870
```

Portanto, a tentativa 1 deve ser a melhor da Vespa.

Validar:

- apenas inscrições ativas e `APROVADA`;
- apenas tentativas válidas e concluídas;
- tempo final = bruto + penalidade;
- uma melhor tentativa por inscrição;
- desempate por tempo final, tempo bruto e `registrationId`.

---

# PARTE C — Chaveamento e progressão

## 15. Bracket CRUD

Base: `/api/v1/chaveamentos`

Rotas:

```http
POST /api/v1/chaveamentos
POST /api/v1/chaveamentos/gerar?competitionId={id}&categoryId={id}
GET /api/v1/chaveamentos
GET /api/v1/chaveamentos?apenasAtivos=true
GET /api/v1/chaveamentos/{id}
GET /api/v1/chaveamentos/por-competicao?competitionId={id}
PUT /api/v1/chaveamentos/{id}
DELETE /api/v1/chaveamentos/{id}
PATCH /api/v1/chaveamentos/{id}/reativar
```

Não gere segundo bracket para a mesma combinação competição/categoria.

---

## 16. Geração automática da árvore — ETAPAS C/D

```http
POST /api/v1/chaveamentos/gerar?competitionId={competitionId}&categoryId={categoryId}
```

Esperado: `201`, bracket `GERADO`.

Validar:

- só entram inscrições ativas e aprovadas;
- mínimo de 2 participantes;
- tamanho = próxima potência de 2;
- participantes são embaralhados antes da distribuição;
- BYEs completam os slots;
- rodadas futuras começam `AGUARDANDO_PARTICIPANTES`;
- árvore completa é criada numa única chamada.

Conferir:

```http
GET /api/v1/partidas/por-chaveamento?bracketId={bracketId}
```

Casos de referência:

```text
2 participantes -> 1 partida
3 participantes -> chave 4 -> 2 + 1
4 participantes -> 2 + 1
5 a 8 -> chave 8 -> 4 + 2 + 1
9 a 16 -> chave 16 -> 8 + 4 + 2 + 1
```

Para validar BYE de verdade, faça ao menos um cenário com 3 participantes.

---

## 17. Match

Base: `/api/v1/partidas`

Rotas principais:

```http
POST /api/v1/partidas
GET /api/v1/partidas/{id}
GET /api/v1/partidas/por-chaveamento?bracketId={id}
PUT /api/v1/partidas/{id}
DELETE /api/v1/partidas/{id}
PATCH /api/v1/partidas/{id}/reativar
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

Validar participantes diferentes, inscrições aprovadas, mesma competição/categoria do bracket e unicidade de `bracket + rodada + ordem`.

---

## 18. MatchResult + avanço automático — ETAPA E

Base: `/api/v1/resultados-partida`

Rotas:

```http
POST /api/v1/resultados-partida
GET /api/v1/resultados-partida
GET /api/v1/resultados-partida/{id}
GET /api/v1/resultados-partida/por-partida?matchId={id}
GET /api/v1/resultados-partida/por-chaveamento?bracketId={id}
GET /api/v1/resultados-partida/por-competicao?competitionId={id}
PUT /api/v1/resultados-partida/{id}
DELETE /api/v1/resultados-partida/{id}
```

Body para modalidade não-Sumô:

```json
{
  "matchId": 10,
  "winnerRegistrationId": 4,
  "pontosA": 2,
  "pontosB": 1,
  "observacao": "Resultado de teste"
}
```

Validar:

- uma partida possui no máximo um resultado;
- vencedor precisa ser participante;
- empate não aceita vencedor;
- pontuação diferente exige vencedor;
- BYE não recebe resultado manual;
- salvar resultado finaliza a partida;
- vencedor da ordem ímpar entra em `registrationA` da próxima partida;
- vencedor da ordem par entra em `registrationB`;
- próxima partida vira `AGENDADA` ao receber os dois participantes;
- BYE avança automaticamente sem criar `MatchResult` manual;
- vencedor da final encerra o bracket.

Importante: `POST`, `PUT` e `DELETE` manuais de resultado de partida `SUMO` devem ser rejeitados, pois no Sumô o resultado é consolidado automaticamente pelos rounds.

---

# PARTE D — Sumô

## 19. Preparação rápida do cenário Sumô

O initializer não cria inscrições de Sumô. Para testar, reutilize a competição e os robôs já existentes.

Primeiro descubra uma categoria SUMO ativa:

```http
GET /api/v1/categorias/por-modalidade/ativas?modalidade=SUMO
```

Use o ID retornado como `{sumoCategoryId}`.

Crie duas inscrições aprovadas, uma para cada robô:

```http
POST /api/v1/inscricoes
```

Exemplo A:

```json
{
  "competitionId": 1,
  "categoryId": "{sumoCategoryId}",
  "teamId": 1,
  "robotId": 1,
  "status": "APROVADA",
  "observacao": "Sumô A",
  "ativo": true
}
```

Exemplo B:

```json
{
  "competitionId": 1,
  "categoryId": "{sumoCategoryId}",
  "teamId": 2,
  "robotId": 2,
  "status": "APROVADA",
  "observacao": "Sumô B",
  "ativo": true
}
```

Substitua `{sumoCategoryId}` por número no Postman, sem aspas se preferir enviar como JSON numérico.

Guarde os dois IDs de inscrição retornados.

---

## 20. Inspeção do Sumô — ETAPA F

Base: `/api/v1/inspecoes-sumo`

Registrar:

```http
POST /api/v1/inspecoes-sumo
```

```json
{
  "registrationId": 3,
  "pesoMedido": 2.875,
  "observacao": "Inspeção oficial"
}
```

O backend calcula `numeroTentativa` e `aprovada`.

Consultas:

```http
GET /api/v1/inspecoes-sumo/{id}
GET /api/v1/inspecoes-sumo/por-inscricao?registrationId={id}
GET /api/v1/inspecoes-sumo/ultima?registrationId={id}
GET /api/v1/inspecoes-sumo/aptidao?registrationId={id}
```

Validar:

- somente inscrição ativa, aprovada e SUMO pode ser inspecionada;
- peso `<= pesoMax` aprova;
- peso `> pesoMax` reprova;
- numeração das tentativas é automática;
- não permite nova inspeção depois de uma aprovação;
- respeita `maxTentativasInspecao`;
- reprovação na última tentativa permitida muda a inscrição para `DESCLASSIFICADA`;
- com `exigeInspecao=true`, aptidão só é `true` após aprovação;
- com `exigeInspecao=false`, inscrição ativa e aprovada pode estar apta sem inspeção.

Para o fluxo de rounds, aprove as duas inscrições que participarão da partida.

---

## 21. Gerar chave do Sumô

Com pelo menos duas inscrições Sumô aprovadas:

```http
POST /api/v1/chaveamentos/gerar?competitionId=1&categoryId={sumoCategoryId}
```

Guarde o `bracketId`.

Depois:

```http
GET /api/v1/partidas/por-chaveamento?bracketId={bracketId}
```

Com exatamente dois participantes deve existir uma partida com ambos preenchidos e status pronto para disputa.

Guarde o `matchId`.

---

## 22. Rounds do Sumô — ETAPAS G/H

Base: `/api/v1/rounds-sumo`

Round com vencedor:

```http
POST /api/v1/rounds-sumo
```

```json
{
  "matchId": 10,
  "winnerRegistrationId": 3,
  "status": "FINALIZADO",
  "observacao": "Vitória no primeiro round"
}
```

Round sem vencedor:

```json
{
  "matchId": 10,
  "winnerRegistrationId": null,
  "status": "EMPATADO",
  "observacao": "Round empatado"
}
```

Status válidos:

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

Validar:

- partida precisa ser SUMO, ativa e possuir os dois participantes;
- ambos precisam estar aptos pela inspeção;
- `numeroRound` é automático;
- primeiro round muda partida `AGENDADA` para `EM_ANDAMENTO`;
- `FINALIZADO` exige vencedor participante da partida;
- `EMPATADO`, `ANULADO` e `CANCELADO` não aceitam vencedor;
- rounds regulares respeitam `numeroRounds`;
- round adicional só pode ocorrer quando permitido pela configuração e ainda não há vencedor;
- rounds sem vencedor não contam como vitória.

### Consolidação automática do resultado

Com configuração padrão `numeroRounds=3` e `roundsParaVencer=2`, registre vitórias até um participante atingir duas.

Assim que atingir o limite, valide:

```http
GET /api/v1/resultados-partida/por-partida?matchId={matchId}
```

Esperado:

- `200`;
- `winnerRegistrationId` do vencedor;
- `pontosA/pontosB` iguais ao número de vitórias em rounds;
- observação indicando consolidação automática;
- partida `FINALIZADA`;
- vencedor avançado automaticamente se existir rodada seguinte;
- se for a final, bracket `FINALIZADO`.

Depois tente criar resultado manual para essa partida SUMO:

```http
POST /api/v1/resultados-partida
```

Esperado: rejeição por regra de negócio.

---

# PARTE E — Tratamento de erros

## 23. GlobalExceptionHandler

### 404

```http
GET /api/v1/inscricoes/999999
```

Esperado: `404` com resposta `ApiErrorResponse`.

### Bean Validation

```http
POST /api/v1/robos
```

```json
{
  "nome": "",
  "teamId": null
}
```

Esperado: `400` e mapa de campos inválidos.

### Parâmetro ausente

```http
GET /api/v1/inscricoes/por-competicao
```

Esperado: `400`.

### Enum inválido

```http
GET /api/v1/competicoes/por-status?status=INVALIDO
```

Esperado: `400`.

### JSON malformado

Enviar body JSON inválido em um POST.

Esperado: `400`.

### Conflito de integridade

Executar uma operação que viole restrição de banco não tratada antes pela regra de negócio.

Esperado pelo handler: `409`.

---

## 24. Ordem recomendada para hoje à noite

Para não gastar tempo demais antes do frontend:

1. Smoke test dos GETs principais;
2. CRUD de Institution/Team/Robot/Registration;
3. ConfigFollow + tentativa + ranking;
4. tratamento global de erros;
5. cenário de 3 participantes para bracket/BYE/progressão;
6. duas inscrições Sumô + inspeções aprovadas;
7. chave Sumô + rounds até resultado automático;
8. confirmar bloqueio de resultado manual no Sumô;
9. corrigir somente bugs que bloqueiem fluxo real.

---

## 25. Critério para congelar o backend

Backend base pronto para os frontends quando:

- CRUDs essenciais passam;
- erros principais retornam status coerentes;
- ConfigFollow e ranking passam;
- geração de bracket passa;
- BYE e avanço de vencedor passam;
- inspeção do Sumô passa;
- rounds e `MatchResult` automático passam;
- nenhum bug encontrado impede os fluxos dos dois frontends.

Depois disso, não adicionar novas regras de domínio antes de começar o frontend salvo bug crítico.
