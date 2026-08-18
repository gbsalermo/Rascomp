# Testes Postman — Rascomp

Última atualização: 2026-08-17

## 1. Objetivo

Este arquivo concentra os endpoints atuais do backend Rascomp, exemplos de JSON para criação/atualização e um roteiro de testes manuais para execução ao final da implementação das regras de negócio.

Base URL:

```text
http://localhost:8080
```

Header para requisições com body:

```text
Content-Type: application/json
```

> Os IDs abaixo são exemplos. Sempre prefira os IDs impressos pelo `DataInitializer` ou retornados pelos POSTs executados durante o teste.

---

## 2. Ordem recomendada dos testes

1. Categorias
2. Configuração Sumô
3. Configuração Seguidor de Linha
4. Instituições
5. Equipes
6. Competidores
7. Robôs
8. Competições
9. Inscrições
10. Tentativas do Seguidor de Linha
11. Ranking do Seguidor de Linha
12. Chaveamentos
13. Partidas
14. Resultados de partida
15. Tratamento global de exceções
16. Regras avançadas implementadas posteriormente

---

# 3. CompetitionCategory

Base:

```text
/api/v1/categorias
```

### Criar categoria

```http
POST /api/v1/categorias
```

```json
{
  "nome": "Seguidor de Linha Teste",
  "descricao": "Categoria criada para os testes finais.",
  "modalidade": "FOLLOW_LINE",
  "ativo": true
}
```

Esperado: `201 Created`.

Modalidades atuais:

```text
SUMO
FOLLOW_LINE
```

### Listar todas

```http
GET /api/v1/categorias
```

Esperado: `200 OK`.

### Buscar por ID

```http
GET /api/v1/categorias/{id}
```

### Listar por modalidade

```http
GET /api/v1/categorias/por-modalidade?modalidade=FOLLOW_LINE
```

### Listar categorias ativas por modalidade

```http
GET /api/v1/categorias/por-modalidade/ativas?modalidade=FOLLOW_LINE
```

### Atualizar

```http
PUT /api/v1/categorias/{id}
```

```json
{
  "nome": "Seguidor de Linha Atualizado",
  "descricao": "Categoria atualizada no teste.",
  "modalidade": "FOLLOW_LINE",
  "ativo": true
}
```

### Desativar

```http
DELETE /api/v1/categorias/{id}
```

Esperado: `204 No Content`.

---

# 4. ConfigSumo

Base:

```text
/api/v1/categorias/{categoryId}/config-sumo
```

### Criar configuração

```http
POST /api/v1/categorias/{categoryId}/config-sumo
```

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

### Buscar configuração da categoria

```http
GET /api/v1/categorias/{categoryId}/config-sumo
```

### Atualizar

```http
PUT /api/v1/categorias/{categoryId}/config-sumo
```

```json
{
  "pesoMax": 3.000,
  "exigeInspecao": true,
  "maxTentativasInspecao": 2,
  "numeroRounds": 3,
  "roundsParaVencer": 2,
  "permiteRoundDesempate": true
}
```

### Excluir configuração

```http
DELETE /api/v1/categorias/{categoryId}/config-sumo
```

---

# 5. ConfigFollow

Base:

```text
/api/v1/categorias/{categoryId}/config-follow
```

### Criar configuração

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

### Buscar configuração

```http
GET /api/v1/categorias/{categoryId}/config-follow
```

### Atualizar

```http
PUT /api/v1/categorias/{categoryId}/config-follow
```

```json
{
  "numeroTomadas": 3,
  "tentativasPorTomada": 3,
  "maxTempoSegundos": 150,
  "numeroCheckpoints": 5
}
```

### Excluir

```http
DELETE /api/v1/categorias/{categoryId}/config-follow
```

---

# 6. Institution

Base:

```text
/api/v1/instituicoes
```

### Criar

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

Esperado: `201 Created`.

### Listar todas

```http
GET /api/v1/instituicoes
```

### Listar somente ativas

```http
GET /api/v1/instituicoes?apenasAtivas=true
```

### Buscar por ID

```http
GET /api/v1/instituicoes/{id}
```

### Buscar por sigla

```http
GET /api/v1/instituicoes/por-sigla?sigla=UFRB
```

### Atualizar

```http
PUT /api/v1/instituicoes/{id}
```

```json
{
  "nome": "Universidade Teste Atualizada",
  "sigla": "UT",
  "cidade": "Cruz das Almas",
  "estado": "BA",
  "ativo": true
}
```

### Desativar

```http
DELETE /api/v1/instituicoes/{id}
```

### Reativar

```http
PATCH /api/v1/instituicoes/{id}/reativar
```

---

# 7. Team

Base:

```text
/api/v1/equipes
```

### Criar

```http
POST /api/v1/equipes
```

```json
{
  "nome": "Equipe Teste",
  "institutionId": 1,
  "ativo": true
}
```

### Listar

```http
GET /api/v1/equipes
```

```http
GET /api/v1/equipes?apenasAtivas=true
```

### Buscar por ID

```http
GET /api/v1/equipes/{id}
```

### Por instituição

```http
GET /api/v1/equipes/por-instituicao?institutionId=1
```

```http
GET /api/v1/equipes/por-instituicao?institutionId=1&apenasAtivas=true
```

### Atualizar

```http
PUT /api/v1/equipes/{id}
```

```json
{
  "nome": "Equipe Teste Atualizada",
  "institutionId": 1,
  "ativo": true
}
```

### Desativar / reativar

```http
DELETE /api/v1/equipes/{id}
PATCH /api/v1/equipes/{id}/reativar
```

---

# 8. Competitor

Base:

```text
/api/v1/competidores
```

### Criar

```http
POST /api/v1/competidores
```

```json
{
  "nome": "Competidor Teste",
  "email": "competidor.teste@rascomp.dev",
  "telefone": "75999999999",
  "teamId": 1,
  "ativo": true
}
```

### Listar

```http
GET /api/v1/competidores
GET /api/v1/competidores?apenasAtivos=true
```

### Buscar por ID

```http
GET /api/v1/competidores/{id}
```

### Buscar por e-mail

```http
GET /api/v1/competidores/por-email?email=competidor.teste@rascomp.dev
```

### Listar por equipe

```http
GET /api/v1/competidores/por-equipe?teamId=1
GET /api/v1/competidores/por-equipe?teamId=1&apenasAtivos=true
```

### Atualizar

```http
PUT /api/v1/competidores/{id}
```

```json
{
  "nome": "Competidor Teste Atualizado",
  "email": "competidor.teste@rascomp.dev",
  "telefone": "75999999998",
  "teamId": 1,
  "ativo": true
}
```

### Desativar / reativar

```http
DELETE /api/v1/competidores/{id}
PATCH /api/v1/competidores/{id}/reativar
```

---

# 9. Robot

Base:

```text
/api/v1/robos
```

### Criar

```http
POST /api/v1/robos
```

```json
{
  "nome": "Robot Teste",
  "descricao": "Robô criado para validação final.",
  "teamId": 1,
  "ativo": true
}
```

### Listar

```http
GET /api/v1/robos
GET /api/v1/robos?apenasAtivos=true
```

### Buscar por ID

```http
GET /api/v1/robos/{id}
```

### Por equipe

```http
GET /api/v1/robos/por-equipe?teamId=1
GET /api/v1/robos/por-equipe?teamId=1&apenasAtivos=true
```

### Atualizar

```http
PUT /api/v1/robos/{id}
```

```json
{
  "nome": "Robot Teste Atualizado",
  "descricao": "Descrição atualizada.",
  "teamId": 1,
  "ativo": true
}
```

### Desativar / reativar

```http
DELETE /api/v1/robos/{id}
PATCH /api/v1/robos/{id}/reativar
```

---

# 10. Competition

Base:

```text
/api/v1/competicoes
```

### Criar

```http
POST /api/v1/competicoes
```

```json
{
  "nome": "RRC Teste",
  "descricao": "Competição para testes finais.",
  "inicioInscricoes": "2026-08-01",
  "fimInscricoes": "2026-08-31",
  "dataInicio": "2026-09-05",
  "dataFim": "2026-09-06",
  "status": "INSCRICOES_ABERTAS",
  "ativo": true
}
```

Status atuais:

```text
PLANEJADA
INSCRICOES_ABERTAS
INSCRICOES_ENCERRADAS
EM_ANDAMENTO
FINALIZADA
CANCELADA
```

### Listar

```http
GET /api/v1/competicoes
GET /api/v1/competicoes?apenasAtivas=true
```

### Buscar por ID

```http
GET /api/v1/competicoes/{id}
```

### Por status

```http
GET /api/v1/competicoes/por-status?status=INSCRICOES_ABERTAS
```

### Atualizar

```http
PUT /api/v1/competicoes/{id}
```

Use o mesmo formato JSON do POST.

### Desativar / reativar

```http
DELETE /api/v1/competicoes/{id}
PATCH /api/v1/competicoes/{id}/reativar
```

---

# 11. Registration

Base:

```text
/api/v1/inscricoes
```

### Criar

```http
POST /api/v1/inscricoes
```

```json
{
  "competitionId": 1,
  "categoryId": 3,
  "teamId": 1,
  "robotId": 1,
  "status": "PENDENTE",
  "observacao": "Inscrição criada durante os testes.",
  "ativo": true
}
```

Status:

```text
PENDENTE
APROVADA
REJEITADA
CANCELADA
```

### Listar

```http
GET /api/v1/inscricoes
GET /api/v1/inscricoes?apenasAtivas=true
```

### Buscar por ID

```http
GET /api/v1/inscricoes/{id}
```

### Por competição

```http
GET /api/v1/inscricoes/por-competicao?competitionId=1
```

### Por status

```http
GET /api/v1/inscricoes/por-status?status=APROVADA
```

### Atualizar

```http
PUT /api/v1/inscricoes/{id}
```

Use o mesmo formato JSON do POST.

### Desativar / reativar

```http
DELETE /api/v1/inscricoes/{id}
PATCH /api/v1/inscricoes/{id}/reativar
```

### Teste obrigatório de regra

Tentar cadastrar novamente o mesmo `competitionId + categoryId + robotId`.

Esperado: erro de regra de negócio.

---

# 12. TentativaSeguidorLinha

Base:

```text
/api/v1/tentativas-seguidor-linha
```

### Criar tentativa válida

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
  "observacao": "Tentativa válida de teste."
}
```

### Buscar por ID

```http
GET /api/v1/tentativas-seguidor-linha/{id}
```

### Listar por inscrição

```http
GET /api/v1/tentativas-seguidor-linha/por-inscricao?registrationId=1
```

> Atualmente não existe `GET` na rota base para listar todas as tentativas.

### Atualizar

```http
PUT /api/v1/tentativas-seguidor-linha/{id}
```

Use o mesmo formato JSON do POST.

### Excluir fisicamente

```http
DELETE /api/v1/tentativas-seguidor-linha/{id}
```

## Testes obrigatórios do ConfigFollow

Considerando a configuração padrão `3 tomadas`, `3 tentativas por tomada`, `180 segundos` e `5 checkpoints`:

### Tomada acima do limite

```json
{
  "registrationId": 1,
  "tomada": 4,
  "numeroTentativa": 1,
  "tempoSegundos": 40.500,
  "checkpointsAlcancados": 5,
  "penalidadeSegundos": 0,
  "concluida": true,
  "valida": true
}
```

Esperado: `400`.

### Tentativa acima do limite

Use `numeroTentativa: 4`.

Esperado: `400`.

### Checkpoints acima do limite

Use `checkpointsAlcancados: 6`.

Esperado: `400`.

### Tempo acima do limite

```json
{
  "registrationId": 1,
  "tomada": 2,
  "numeroTentativa": 2,
  "tempoSegundos": 181.500,
  "checkpointsAlcancados": 5,
  "penalidadeSegundos": 0,
  "concluida": true,
  "valida": true
}
```

Esperado: `201 Created`, porém a resposta deve conter:

```json
{
  "valida": false
}
```

---

# 13. Ranking Seguidor de Linha

```http
GET /api/v1/ranking/seguidor-linha?competitionId=1&categoryId=3
```

Esperado: `200 OK` e lista ordenada pelo menor tempo final.

Critérios atuais:

1. inscrição ativa e aprovada;
2. tentativa válida e concluída;
3. tempo registrado;
4. tempo final = tempo bruto + penalidade;
5. menor tempo final vence;
6. empate: menor tempo bruto;
7. novo empate: menor `registrationId`.

Teste recomendado: cadastrar tentativas para pelo menos três inscrições, incluindo uma com penalidade, e conferir a ordenação.

---

# 14. Bracket

Base:

```text
/api/v1/chaveamentos
```

### Criar

```http
POST /api/v1/chaveamentos
```

```json
{
  "competitionId": 1,
  "categoryId": 1,
  "nome": "Chave Teste",
  "status": "RASCUNHO",
  "ativo": true
}
```

Status atuais:

```text
RASCUNHO
GERADO
EM_ANDAMENTO
FINALIZADO
CANCELADO
```

### Listar

```http
GET /api/v1/chaveamentos
GET /api/v1/chaveamentos?apenasAtivos=true
```

### Buscar por ID

```http
GET /api/v1/chaveamentos/{id}
```

### Por competição

```http
GET /api/v1/chaveamentos/por-competicao?competitionId=1
```

### Atualizar

```http
PUT /api/v1/chaveamentos/{id}
```

Use o mesmo formato JSON do POST.

### Desativar / reativar

```http
DELETE /api/v1/chaveamentos/{id}
PATCH /api/v1/chaveamentos/{id}/reativar
```

Teste de regra: tentar criar segundo chaveamento para a mesma competição/categoria.

---

# 15. Match

Base:

```text
/api/v1/partidas
```

### Criar

```http
POST /api/v1/partidas
```

```json
{
  "bracketId": 1,
  "rodada": 1,
  "ordem": 2,
  "registrationAId": 1,
  "registrationBId": 2,
  "dataHora": "2026-09-05T11:00:00",
  "status": "AGENDADA",
  "ativo": true
}
```

Status atuais:

```text
AGENDADA
BYE
EM_ANDAMENTO
FINALIZADA
CANCELADA
```

### Buscar por ID

```http
GET /api/v1/partidas/{id}
```

### Listar por chaveamento

```http
GET /api/v1/partidas/por-chaveamento?bracketId=1
```

> Atualmente não existe `GET /api/v1/partidas` para listagem global.

### Atualizar

```http
PUT /api/v1/partidas/{id}
```

Use o mesmo formato JSON do POST.

### Desativar / reativar

```http
DELETE /api/v1/partidas/{id}
PATCH /api/v1/partidas/{id}/reativar
```

Testes de regra:

- inscrições A e B devem ser diferentes;
- ambas devem pertencer à competição/categoria do chaveamento;
- ambas devem estar aprovadas;
- `registrationBId` nulo deve permitir cenário de `BYE` conforme regra atual;
- combinação `bracket + rodada + ordem` não pode duplicar.

---

# 16. MatchResult

Base:

```text
/api/v1/resultados-partida
```

### Criar

```http
POST /api/v1/resultados-partida
```

```json
{
  "matchId": 1,
  "winnerRegistrationId": 1,
  "pontosA": 2,
  "pontosB": 1,
  "observacao": "Resultado registrado durante os testes."
}
```

### Buscar por ID

```http
GET /api/v1/resultados-partida/{id}
```

### Buscar por partida

```http
GET /api/v1/resultados-partida/por-partida?matchId=1
```

> A versão atual deve ser conferida antes dos testes finais caso seja adicionada a listagem global de resultados discutida durante o desenvolvimento.

### Atualizar

```http
PUT /api/v1/resultados-partida/{id}
```

Use o mesmo formato JSON do POST.

### Excluir

```http
DELETE /api/v1/resultados-partida/{id}
```

Regras obrigatórias:

- uma partida não pode possuir dois resultados;
- vencedor deve ser participante da partida;
- pontos diferentes exigem vencedor;
- empate não aceita vencedor;
- criação do resultado deve finalizar a partida;
- exclusão do resultado deve restaurar o estado da partida conforme regra do service.

---

# 17. Tratamento global de exceções

Executar estes testes depois que o `GlobalExceptionHandler` estiver confirmado no repositório/local.

### Recurso inexistente

```http
GET /api/v1/inscricoes/999999
```

Esperado: `404`, com mensagem própria da API.

### Regra de negócio inválida

Tentar inscrição duplicada.

Esperado: `400`.

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

Esperado: `400` com os campos inválidos detalhados.

### Parâmetro obrigatório ausente

```http
GET /api/v1/inscricoes/por-competicao
```

Esperado: `400`.

### Enum inválido

```http
GET /api/v1/competicoes/por-status?status=STATUS_INEXISTENTE
```

Esperado: `400`.

### JSON inválido

Enviar body JSON malformado em qualquer POST.

Esperado: `400`.

---

# 18. Testes das próximas regras de domínio

Adicionar nesta seção à medida que as implementações forem concluídas.

## Geração automática de chaveamento

Pendente.

Validar posteriormente:

- somente inscrições ativas/aprovadas entram no chaveamento;
- somente inscrições da competição/categoria informadas;
- quantidade mínima de participantes;
- criação das partidas da primeira rodada;
- geração correta de `BYE`;
- impedir segundo chaveamento da mesma competição/categoria.

## Avanço automático de vencedor

Pendente.

Validar posteriormente:

- vencedor ocupa o slot correto da próxima partida;
- ordem ímpar avança para participante A da próxima partida;
- ordem par avança para participante B;
- BYE avança automaticamente;
- final encerra o chaveamento.

## Inspeção e rounds do Sumô

Pendente.

Validar posteriormente:

- peso máximo;
- número máximo de tentativas de inspeção;
- exigência de inspeção quando configurada;
- rounds necessários para vencer;
- round de desempate quando permitido;
- consolidação automática em `MatchResult`.

---

# 19. Critério para encerrar a fase de testes

A etapa de backend só deve ser marcada como validada quando:

- aplicação sobe sem erro;
- todos os endpoints deste arquivo foram conferidos;
- casos positivos retornam os status esperados;
- regras inválidas são rejeitadas;
- soft delete/reativação funcionam;
- relacionamentos permanecem íntegros;
- ranking está correto;
- regras avançadas futuras foram incorporadas a este roteiro e testadas;
- persistência final foi validada no banco escolhido para execução real da competição.
