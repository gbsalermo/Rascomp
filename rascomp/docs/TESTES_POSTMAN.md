# Testes Postman — Rascomp

Última atualização: 2026-08-17

## 1. Objetivo

Bateria manual final do backend Rascomp. Deve ser executada depois das regras avançadas, antes de congelar o contrato da API para os dois frontends.

Base URL:

```text
http://localhost:8080
```

Header para requests com body:

```text
Content-Type: application/json
```

Os IDs são exemplos. Use os IDs retornados pelo `DataInitializer` ou pelos POSTs executados durante a bateria.

---

## 2. Pré-teste de persistência MySQL + Flyway

Antes do Postman:

1. garantir MySQL ativo;
2. configurar `DB_USERNAME` e `DB_PASSWORD` quando diferentes dos defaults;
3. iniciar a aplicação;
4. confirmar execução da migration `V1__create_rascomp_schema.sql`;
5. confirmar que Hibernate executa com `ddl-auto=validate`;
6. reiniciar a aplicação e verificar que os dados continuam no banco.

Configuração padrão:

```text
DB_URL=jdbc:mysql://localhost:3306/rascomp?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bahia
DB_USERNAME=root
DB_PASSWORD=
```

---

## 3. CompetitionCategory

Base: `/api/v1/categorias`

### Criar FOLLOW_LINE

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

### Criar SUMO

```json
{
  "nome": "Sumô Teste",
  "descricao": "Categoria de teste",
  "modalidade": "SUMO",
  "ativo": true
}
```

### Consultas

```http
GET /api/v1/categorias
GET /api/v1/categorias/{id}
GET /api/v1/categorias/por-modalidade?modalidade=FOLLOW_LINE
GET /api/v1/categorias/por-modalidade/ativas?modalidade=FOLLOW_LINE
```

### Atualizar

```http
PUT /api/v1/categorias/{id}
```

```json
{
  "nome": "Seguidor de Linha Atualizado",
  "descricao": "Atualizada",
  "modalidade": "FOLLOW_LINE",
  "ativo": true
}
```

### Excluir

```http
DELETE /api/v1/categorias/{id}
```

---

## 4. ConfigSumo

Base: `/api/v1/categorias/{categoryId}/config-sumo`

### Criar

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

### Consultar / atualizar / excluir

```http
GET /api/v1/categorias/{categoryId}/config-sumo
PUT /api/v1/categorias/{categoryId}/config-sumo
DELETE /api/v1/categorias/{categoryId}/config-sumo
```

No PUT, use o mesmo formato JSON do POST.

---

## 5. ConfigFollow

Base: `/api/v1/categorias/{categoryId}/config-follow`

### Criar

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

### Consultar / atualizar / excluir

```http
GET /api/v1/categorias/{categoryId}/config-follow
PUT /api/v1/categorias/{categoryId}/config-follow
DELETE /api/v1/categorias/{categoryId}/config-follow
```

---

## 6. Institution

Base: `/api/v1/instituicoes`

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

### Consultas

```http
GET /api/v1/instituicoes
GET /api/v1/instituicoes?apenasAtivas=true
GET /api/v1/instituicoes/{id}
GET /api/v1/instituicoes/por-sigla?sigla=UT
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

### Desativar / reativar

```http
DELETE /api/v1/instituicoes/{id}
PATCH /api/v1/instituicoes/{id}/reativar
```

---

## 7. Team

Base: `/api/v1/equipes`

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

### Consultas

```http
GET /api/v1/equipes
GET /api/v1/equipes?apenasAtivas=true
GET /api/v1/equipes/{id}
GET /api/v1/equipes/por-instituicao?institutionId=1
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

## 8. Competitor

Base: `/api/v1/competidores`

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

### Consultas

```http
GET /api/v1/competidores
GET /api/v1/competidores?apenasAtivos=true
GET /api/v1/competidores/{id}
GET /api/v1/competidores/por-email?email=competidor.teste@rascomp.dev
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

## 9. Robot

Base: `/api/v1/robos`

### Criar

```http
POST /api/v1/robos
```

```json
{
  "nome": "Robot Teste",
  "descricao": "Robô de teste",
  "teamId": 1,
  "ativo": true
}
```

### Consultas

```http
GET /api/v1/robos
GET /api/v1/robos?apenasAtivos=true
GET /api/v1/robos/{id}
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
  "descricao": "Robô atualizado",
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

## 10. Competition

Base: `/api/v1/competicoes`

### Criar

```http
POST /api/v1/competicoes
```

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

### Consultas

```http
GET /api/v1/competicoes
GET /api/v1/competicoes?apenasAtivas=true
GET /api/v1/competicoes/{id}
GET /api/v1/competicoes/por-status?status=INSCRICOES_ABERTAS
```

### Atualizar / desativar / reativar

```http
PUT /api/v1/competicoes/{id}
DELETE /api/v1/competicoes/{id}
PATCH /api/v1/competicoes/{id}/reativar
```

---

## 11. Registration

Base: `/api/v1/inscricoes`

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
  "status": "APROVADA",
  "observacao": "Inscrição de teste",
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

### Consultas

```http
GET /api/v1/inscricoes
GET /api/v1/inscricoes?apenasAtivas=true
GET /api/v1/inscricoes/{id}
GET /api/v1/inscricoes/por-competicao?competitionId=1
GET /api/v1/inscricoes/por-status?status=APROVADA
```

### Atualizar / desativar / reativar

```http
PUT /api/v1/inscricoes/{id}
DELETE /api/v1/inscricoes/{id}
PATCH /api/v1/inscricoes/{id}/reativar
```

Teste negativo obrigatório: repetir o mesmo `competitionId + categoryId + robotId`. Esperado: `400`.

---

## 12. TentativaSeguidorLinha + ConfigFollow

Base: `/api/v1/tentativas-seguidor-linha`

### Criar válida

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

### Consultas

```http
GET /api/v1/tentativas-seguidor-linha/{id}
GET /api/v1/tentativas-seguidor-linha/por-inscricao?registrationId=1
```

### Atualizar / excluir

```http
PUT /api/v1/tentativas-seguidor-linha/{id}
DELETE /api/v1/tentativas-seguidor-linha/{id}
```

### Testes ConfigFollow

Com configuração 3 tomadas / 3 tentativas / 180 segundos / 5 checkpoints:

- `tomada=4` -> esperado `400`;
- `numeroTentativa=4` -> esperado `400`;
- `checkpointsAlcancados=6` -> esperado `400`;
- `tempoSegundos=181.500` -> esperado `201`, mas `valida=false`.

---

## 13. Ranking Seguidor de Linha

```http
GET /api/v1/ranking/seguidor-linha?competitionId=1&categoryId=3
```

Esperado: `200` e lista ordenada.

Regras a conferir:

1. somente inscrições ativas e aprovadas;
2. somente tentativas válidas e concluídas;
3. tempo final = tempo bruto + penalidade;
4. melhor tentativa por inscrição;
5. menor tempo final primeiro;
6. empate por menor tempo bruto;
7. persistindo empate, menor `registrationId`.

---

## 14. Bracket CRUD

Base: `/api/v1/chaveamentos`

### Criar manualmente

```http
POST /api/v1/chaveamentos
```

```json
{
  "competitionId": 1,
  "categoryId": 1,
  "nome": "Chave Manual Teste",
  "status": "RASCUNHO",
  "ativo": true
}
```

### Consultas

```http
GET /api/v1/chaveamentos
GET /api/v1/chaveamentos?apenasAtivos=true
GET /api/v1/chaveamentos/{id}
GET /api/v1/chaveamentos/por-competicao?competitionId=1
```

### Atualizar / desativar / reativar

```http
PUT /api/v1/chaveamentos/{id}
DELETE /api/v1/chaveamentos/{id}
PATCH /api/v1/chaveamentos/{id}/reativar
```

---

## 15. ETAPAS C e D — geração automática da árvore do chaveamento

Não crie manualmente um bracket para a mesma competição/categoria antes deste teste.

### Gerar automaticamente

```http
POST /api/v1/chaveamentos/gerar?competitionId=1&categoryId=1
```

Esperado: `201 Created` e bracket com `status=GERADO`.

Regras obrigatórias:

- somente inscrições `APROVADA` e `ativo=true` participam;
- mínimo de 2 inscrições;
- segundo bracket para a mesma competição/categoria deve ser rejeitado;
- tamanho da chave deve ser a próxima potência de 2;
- BYEs devem completar os slots ausentes;
- primeira rodada: partidas completas `AGENDADA`, partidas com um participante `BYE`;
- rodadas seguintes: `AGUARDANDO_PARTICIPANTES`;
- todas as rodadas até a final devem existir após uma única chamada.

### Conferir árvore gerada

```http
GET /api/v1/partidas/por-chaveamento?bracketId={bracketId}
```

Casos esperados:

```text
2 participantes -> 1 partida total
3 participantes -> chave 4 -> 2 partidas na rodada 1 + 1 final
4 participantes -> 2 partidas na rodada 1 + 1 final
5 a 8 participantes -> chave 8 -> 4 + 2 + 1 partidas
9 a 16 participantes -> chave 16 -> 8 + 4 + 2 + 1 partidas
```

Teste com 3 ou 5 participantes para obrigar a existência de BYE.

---

## 16. Match

Base: `/api/v1/partidas`

### Criar manualmente

```http
POST /api/v1/partidas
```

```json
{
  "bracketId": 1,
  "rodada": 1,
  "ordem": 1,
  "registrationAId": 1,
  "registrationBId": 2,
  "dataHora": "2026-09-05T11:00:00",
  "status": "AGENDADA",
  "ativo": true
}
```

Status atuais:

```text
AGUARDANDO_PARTICIPANTES
AGENDADA
EM_ANDAMENTO
FINALIZADA
CANCELADA
BYE
```

### Consultas

```http
GET /api/v1/partidas/{id}
GET /api/v1/partidas/por-chaveamento?bracketId=1
```

### Atualizar / desativar / reativar

```http
PUT /api/v1/partidas/{id}
DELETE /api/v1/partidas/{id}
PATCH /api/v1/partidas/{id}/reativar
```

Regras: participantes diferentes, inscrições aprovadas, mesma competição/categoria do bracket e `bracket + rodada + ordem` único.

---

## 17. MatchResult

Base: `/api/v1/resultados-partida`

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
  "observacao": "Resultado de teste"
}
```

### Consultas

```http
GET /api/v1/resultados-partida/{id}
GET /api/v1/resultados-partida/por-partida?matchId=1
```

### Atualizar / excluir

```http
PUT /api/v1/resultados-partida/{id}
DELETE /api/v1/resultados-partida/{id}
```

Regras:

- uma partida possui no máximo um resultado;
- vencedor deve ser participante;
- pontos diferentes exigem vencedor;
- empate não aceita vencedor;
- salvar resultado finaliza partida;
- excluir resultado restaura o status inicial conforme regra atual.

---

## 18. Tratamento global de exceções

### 404

```http
GET /api/v1/inscricoes/999999
```

Esperado: resposta padronizada com `404`.

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

Esperado: `400` com erros dos campos.

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

Enviar body inválido em qualquer POST. Esperado: `400`.

---

## 19. ETAPA E — avanço de vencedor e BYE

Pendente de implementação.

Quando concluída, validar:

- vencedor da partida ordem ímpar entra em `registrationA` da próxima partida;
- vencedor da ordem par entra em `registrationB`;
- próxima partida muda para `AGENDADA` quando receber os dois participantes;
- BYE avança automaticamente sem `MatchResult` manual;
- vencedor da final encerra o bracket.

---

## 20. Sumô — inspeção e rounds

Pendente de implementação.

Adicionar à bateria final:

- inspeção obrigatória quando `exigeInspecao=true`;
- peso máximo;
- máximo de tentativas de inspeção;
- bloqueio/desclassificação quando necessário;
- rounds regulares;
- `roundsParaVencer`;
- round adicional quando permitido;
- rounds sem vencedor não contam;
- consolidação automática do `MatchResult`.

---

## 21. Critério de fechamento do backend base

Antes de iniciar os frontends:

- MySQL persiste dados após reinício;
- Flyway cria/valida o schema sem `ddl-auto=create-drop`;
- CRUDs essenciais passam;
- erros principais retornam resposta padronizada;
- ConfigFollow passa;
- ranking passa;
- geração automática de bracket passa;
- avanço de vencedor/BYE passa;
- versão mínima das regras do Sumô passa;
- endpoints consumidos pelos frontends ficam congelados.
