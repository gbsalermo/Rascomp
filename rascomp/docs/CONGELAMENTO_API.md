# Congelamento da API — Rascomp

## Estado do marco

O contrato do backend está **congelado novamente** após a correção arquitetural de usuários/acesso.

Histórico:

```text
23/08/2026  núcleo competitivo validado e congelado
24/08/2026  congelamento reaberto por lacuna de usuários/autenticação
24/08/2026  arquitetura corrigida, smoke executado e PR #4 mergeado
24/08/2026  contrato congelado novamente antes do Swagger
```

Merge principal da correção:

```text
PR #4 — Arquitetura de usuários, ownership e acesso
commit: fac45cffc07bdfaab8ba07a12a49836b0c4a90d0
```

---

## Escopo congelado

### Identidade e acesso

```text
UserAccount
PARTICIPANTE
ORGANIZACAO
JWT Bearer
BCryptPasswordEncoder(12)
ownership por Team.responsibleUser
```

Regras:

- `UserAccount` é separado de `Competitor`;
- um `Competitor` pode existir sem conta;
- um `PARTICIPANTE` pode também ser `Competitor`;
- responsabilidade por equipe é relacionamento/ownership, não role global;
- senha nunca é persistida em texto puro;
- `passwordHash` nunca deve aparecer em DTOs externos;
- `JWT_SECRET` permanece externo ao Git;
- IDs seguem numéricos; public ID/UUID não faz parte do escopo atual.

### Autenticação

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

### API do participante

```text
/api/v1/participante/**
```

Requer `ROLE_PARTICIPANTE` + ownership do recurso.

O participante pode operar as próprias:

- equipes;
- pessoas/competidores;
- robôs;
- fotos;
- inscrições.

### API da organização

```text
/api/v1/**
/engine-rest/**
```

Fora das rotas explicitamente públicas/auth, exige `ROLE_ORGANIZACAO`.

A organização opera a competição, incluindo análise de inscrições, inspeções, chaveamentos, partidas, rounds, tentativas e ranking.

### API pública

```text
/api/v1/public/**
```

Sem autenticação e somente leitura.

DTOs públicos devem continuar sanitizados e não podem expor dados privados de usuário/competidor nem campos administrativos desnecessários.

---

## Inscrições

`Registration` passa a consolidar:

```text
competition
category
team
robot
competitors[]
requestedByUser
reviewedByUser
reviewedAt
status
observacao
ativo
```

Regras congeladas:

- inscrição enviada por participante inicia `PENDENTE`;
- participante não escolhe livremente status/revisor;
- competidores da inscrição devem pertencer à equipe informada;
- robô deve pertencer à equipe;
- aprovação/rejeição é operação administrativa;
- quando houver revisão, `reviewedByUser` e `reviewedAt` devem registrar o ator/data.

---

## Fotos dos robôs

Arquivos não são armazenados como BLOB no MySQL.

Banco:

```text
RobotImage
- id
- robotId
- storageKey
- originalFilename
- contentType
- principal
- ordem
- ativo
- dataCadastro
```

Storage atual:

```text
./uploads/robots
```

Configurável por `ROBOT_IMAGES_DIR`.

Regras congeladas:

- limite 5 MB;
- formatos JPEG, PNG e WEBP;
- validação usa assinatura real do arquivo, não apenas MIME informado pelo cliente;
- primeira imagem ativa pode se tornar principal;
- arquivo físico fica fora do banco.

---

## Persistência

Migrations aplicadas são imutáveis:

```text
V1
V2
V3
V4
V5
```

A arquitetura de acesso entrou por:

```text
V5__add_users_team_ownership_and_robot_images.sql
```

Qualquer alteração estrutural futura deve usar `V6+`.

---

## FOLLOW_LINE — congelado

```text
Competition
 -> Category FOLLOW_LINE + ConfigFollow
 -> Registration APROVADA
 -> TentativaSeguidorLinha
 -> RankingFollowService
 -> classificação/campeão
```

Regras:

- melhor tentativa válida e concluída por inscrição;
- `tempoFinal = tempoSegundos + penalidadeSegundos`;
- tentativas inválidas/não concluídas não entram no ranking;
- FOLLOW_LINE não usa `Bracket`, `Match`, `RoundSumo` ou `MatchResult`.

---

## SUMO — congelado

```text
Competition
 -> Category SUMO + ConfigSumo
 -> Registration APROVADA
 -> InspecaoSumo / aptidão
 -> Bracket
 -> Match
 -> RoundSumo
 -> MatchResult automático
 -> progressão/campeão
```

Regras:

- bracket considera apenas inscrições ativas, aprovadas e aptas;
- rounds consolidam o resultado automaticamente;
- `MatchResult` continua read-only externamente;
- progressão/campeão continuam automáticos conforme o domínio já validado.

---

## O que o Swagger pode alterar

Permitido:

- configuração OpenAPI;
- metadados da API;
- `@Tag`;
- `@Operation`;
- `@ApiResponse`;
- `@Parameter`;
- `@Schema`;
- exemplos de payload;
- descrição de enums/erros;
- security scheme Bearer JWT;
- documentação multipart;
- organização visual do Swagger UI;
- testes/documentação OpenAPI.

---

## O que o Swagger NÃO pode alterar

Sem nova decisão arquitetural explícita, não modificar:

- regras de negócio;
- relacionamentos JPA;
- migrations V1–V5;
- nomes/semântica dos endpoints;
- ownership;
- roles;
- autenticação JWT/BCrypt;
- status HTTP já consolidados;
- comportamento Follow/Sumô;
- regra read-only de `MatchResult`;
- BPMN/Camunda funcional.

Se a documentação revelar inconsistência real, registrar a exceção antes de mudar o contrato.

---

## Critério de aceite do Swagger

```text
/v3/api-docs                         ✅
/swagger-ui/index.html               ✅
metadata da API                      ✅
controllers por tags                 ✅
endpoints descritos                  ✅
path/query params                    ✅
request bodies                       ✅
200/201/204                          ✅
400/401/403/404/405/409/415          ✅
DTO schemas                          ✅
JSON examples                        ✅
Bearer JWT                           ✅
upload multipart                    ✅
API pública                          ✅ identificada
API participante                     ✅ identificada
API organização                      ✅ identificada
MatchResult read-only                ✅ explícito
Follow/Sumô                          ✅ separados
```

---

## Validação conhecida

Smoke local concluído para:

```text
Flyway V5
PARTICIPANTE register/login
/auth/me
401
403/ownership
equipe
competidor
robô
upload
inscrição
validação de competidor da equipe
API pública/sanitização
```

Pendência manual não bloqueante para o fechamento final:

```text
repetir login ORGANIZACAO
validar /auth/me com role ORGANIZACAO
aprovar/rejeitar uma Registration autenticado
confirmar reviewedByUser/reviewedAt
```

Essa pendência deve ser encerrada antes de declarar o backend definitivamente finalizado.

---

## Camunda

Estado atual:

```text
Engine          ✅
JobExecutor     ✅
ACT_*           ✅
REST starter    ✅
BPMN Rascomp    ⏳
```

Primeiro processo candidato:

```text
PARTICIPANTE envia inscrição
        -> PENDENTE
        -> tarefa da ORGANIZACAO
        -> APROVADA ou REJEITADA
```

Camunda deve orquestrar processos/human tasks; regras competitivas permanecem nos services Java.

---

## Próxima etapa

```text
Swagger / OpenAPI
```

Após o Swagger:

```text
revalidação ORGANIZACAO
 -> revisão final do backend
 -> decisão sobre BPMN/Camunda
 -> continuidade dos frontends
```