# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-24T21:34:00-03:00

## 1. Marco atual

O backend competitivo e a nova arquitetura de usuários/acesso estão integrados na `main`.

O PR #4 — **Arquitetura de usuários, ownership e acesso** — foi mergeado com sucesso no commit:

```text
fac45cffc07bdfaab8ba07a12a49836b0c4a90d0
```

O workflow pós-merge também passou:

```text
Backend Tests #43
status: completed
conclusion: success
```

Estado atual:

```text
NÚCLEO COMPETITIVO
MySQL/Flyway V1–V4                    ✅
FOLLOW_LINE                           ✅ validado manualmente
SUMO                                  ✅ validado manualmente
testes automatizados                 ✅

ARQUITETURA DE ACESSO
UserAccount                           ✅
PARTICIPANTE / ORGANIZACAO            ✅
JWT + BCrypt                          ✅
responsável da equipe                 ✅
UserAccount ↔ Competitor opcional     ✅
Registration + competidores           ✅
auditoria solicitante/revisor         ✅
fotos dos robôs                       ✅
API participante + ownership          ✅
API pública sanitizada                ✅
Migration V5                          ✅

VALIDAÇÃO LOCAL DA NOVA ARQUITETURA
startup MySQL + Flyway V5             ✅
cadastro/login PARTICIPANTE           ✅
/auth/me                              ✅
401 sem autenticação                  ✅
403/ownership                         ✅
equipe / competidor / robô            ✅
upload de imagem                      ✅
inscrição do participante             ✅
competidor de outra equipe rejeitado  ✅
API pública sanitizada                ✅
fluxo manual aprovação ORGANIZACAO    ⚠️ repetir no fechamento final

PR #4                                 ✅ mergeado
CI pós-merge                          ✅ Backend Tests #43
novo congelamento da API              ✅
Swagger/OpenAPI                       ⏳ próxima etapa
Camunda BPMN funcional                ⏳ checkpoint pós-Swagger
Frontend de Gestão                    🔄 trabalho paralelo
Landing / Frontend Público            🔄 trabalho paralelo
```

> A validação manual específica de aprovação/rejeição usando um token `ORGANIZACAO` ficou pendente de repetição. Isso não bloqueou o merge porque a regra administrativa já existia no núcleo e será revalidada durante a revisão final do Swagger/backend.

---

## 2. Objetivo do Rascomp

O Rascomp é uma plataforma de gestão de competições de robótica no contexto da IEEE Robotics & Automation Society — UFRB.

Há dois clientes principais:

```text
Frontend de Gestão
  ├─ PARTICIPANTE
  └─ ORGANIZACAO

Frontend Público / Landing
  └─ consulta pública sanitizada
```

O backend é a fonte de verdade para autenticação, ownership, regras competitivas, inscrições, resultados e permissões.

---

## 3. Usuários e papéis

O projeto utiliza somente dois papéis globais:

```text
PARTICIPANTE
ORGANIZACAO
```

### PARTICIPANTE

Conta criada pelo próprio usuário.

Pode:

- registrar conta e fazer login;
- consultar o próprio usuário em `/api/v1/auth/me`;
- criar e gerenciar apenas equipes pelas quais é responsável;
- cadastrar competidores da equipe;
- opcionalmente vincular a própria conta a um `Competitor`;
- cadastrar robôs;
- enviar fotos dos robôs;
- criar e cancelar inscrições da própria equipe;
- escolher os competidores que participarão de cada inscrição;
- acompanhar status das inscrições.

### ORGANIZACAO

Conta administrativa do evento.

Pode operar:

- usuários;
- instituições;
- equipes;
- competidores;
- robôs e fotos;
- competições e categorias;
- inscrições e análise;
- inspeção Sumô;
- chaveamentos, partidas e rounds;
- tentativas e ranking Follow;
- Camunda REST;
- demais endpoints internos de gestão.

Responsabilidade de equipe **não é role global**. É ownership:

```text
Team.responsibleUser -> UserAccount(PARTICIPANTE)
```

---

## 4. Identidade e segurança

Entidade:

```text
UserAccount
- id
- nome
- email unique
- passwordHash
- telefone
- role
- ativo
- ultimoLogin
- dataCadastro
```

Senha:

```text
senha recebida
 -> BCryptPasswordEncoder(12)
 -> password_hash
```

Regras:

- senha original nunca é persistida;
- nenhum DTO de resposta expõe `passwordHash`;
- autenticação é stateless com JWT;
- o segredo JWT vem da variável `JWT_SECRET`;
- produção deve usar HTTPS.

Endpoints:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

Header:

```text
Authorization: Bearer <token>
```

Primeiro usuário `ORGANIZACAO` pode ser criado por bootstrap opcional quando ainda não existir conta administrativa.

Não foi implementado public ID/UUID. IDs numéricos permanecem adequados ao escopo atual; a segurança depende de autenticação/autorização e ownership, não da ocultação do ID.

---

## 5. Ownership e modelo operacional

### Team

`Team` possui `responsibleUser`.

Nova equipe criada pelo portal participante recebe automaticamente o usuário autenticado como responsável.

```text
UserAccount
    ↓ responsibleUser
Team
 ├─ Competitors
 ├─ Robots
 └─ Registrations
```

### Competitor

`Competitor` representa a pessoa que compete, não necessariamente uma conta de login.

Pode existir sem `UserAccount`.

Opcionalmente:

```text
Competitor.userAccount -> UserAccount
```

Isso permite:

```text
Professor responsável  -> UserAccount ✅ / Competitor ❌
Competidor líder        -> UserAccount ✅ / Competitor ✅
Competidor da equipe    -> UserAccount opcional / Competitor ✅
```

### Registration

A inscrição agora registra:

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

O participante não escolhe `teamId`, responsável, status ou revisor livremente pelo payload do portal; o backend deriva/valida esses dados.

A organização pode aprovar/rejeitar e fica registrada como revisora.

---

## 6. Fotos dos robôs

Metadados ficam no MySQL e o arquivo fica fora do banco.

```text
Robot
  └─ RobotImage
       ├─ storageKey
       ├─ originalFilename
       ├─ contentType
       ├─ principal
       ├─ ordem
       └─ ativo
```

Storage atual:

```text
./uploads/robots
```

Configurável por `ROBOT_IMAGES_DIR`.

Limite atual: **5 MB**.

Formatos:

```text
JPEG
PNG
WEBP
```

Durante o smoke foi encontrada e corrigida uma fragilidade: o upload não depende mais apenas do `Content-Type` enviado pelo cliente. O backend detecta a assinatura real PNG/JPEG/WEBP e persiste o MIME detectado.

---

## 7. Separação das APIs

### Público

```text
/api/v1/public/**
```

Sem login e somente leitura. Usa DTOs sanitizados.

Principais consultas públicas:

```text
competicoes
categorias
instituicoes
equipes
competidores
robos
fotos
inscricoes
ranking Follow
chaveamentos
partidas
resultados
```

### Participante

```text
/api/v1/participante/**
```

Requer `ROLE_PARTICIPANTE` e ownership.

### Organização

```text
/api/v1/**
/engine-rest/**
```

Fora das exceções de auth/public, exige `ROLE_ORGANIZACAO`.

---

## 8. FOLLOW_LINE — contrato congelado

```text
Competition
 -> Category FOLLOW_LINE + ConfigFollow
 -> Registration APROVADA
 -> TentativaSeguidorLinha
 -> RankingFollowService
 -> classificação/campeão
```

Regras principais:

- 3 tomadas no cenário atual;
- até 3 tentativas por tomada;
- tempo máximo configurável;
- checkpoints configuráveis;
- `tempoFinal = tempoSegundos + penalidadeSegundos`;
- ranking usa a melhor tentativa válida e concluída por inscrição;
- inválidas/não concluídas não entram no ranking;
- FOLLOW_LINE não usa `Bracket`, `Match`, `RoundSumo` ou `MatchResult`.

---

## 9. SUMO — contrato congelado

```text
Competition
 -> Category SUMO + ConfigSumo
 -> Registration APROVADA
 -> InspecaoSumo / aptidão
 -> Bracket
 -> Match
 -> RoundSumo(s)
 -> MatchResult automático
 -> progressão/campeão
```

Regras principais:

- apenas inscrições ativas, aprovadas e aptas entram no bracket;
- `MatchResult` é criado automaticamente a partir dos rounds;
- `MatchResult` continua read-only externamente;
- vencedor progride automaticamente;
- bracket pode finalizar automaticamente.

---

## 10. Persistência

```text
V1 — schema competitivo principal
V2 — inspeções Sumô
V3 — rounds Sumô
V4 — remoção de artefatos legados de Follow em bracket
V5 — usuários, ownership, participantes da inscrição e fotos
```

Migrations já aplicadas são imutáveis. Novas alterações estruturais usam `V6+`.

---

## 11. Testes e qualidade

Validações consolidadas:

```text
FOLLOW_LINE bateria manual completa ✅
SUMO bateria manual completa        ✅
JUnit/Mockito                       ✅
GitHub Actions                      ✅
Smoke arquitetura de acesso         ✅ com ressalva ORGANIZACAO
```

A arquitetura de acesso adicionou cobertura para BCrypt, usuários/roles, ownership, recursos de outra equipe, inscrição pelo participante, competidores da equipe e regressão Follow/Sumô.

Suíte automatizada:

```text
28 testes
0 falhas
```

CI pós-merge:

```text
Backend Tests #43
commit fac45cff...
completed / success
```

### Pendência manual conhecida

Repetir no fechamento final:

```text
login ORGANIZACAO
 -> /auth/me role ORGANIZACAO
 -> endpoint administrativo 200
 -> aprovação/rejeição de Registration
 -> reviewedByUser/reviewedAt preenchidos
```

---

## 12. Camunda

```text
Camunda 7.22 embedded     ✅
Process Engine            ✅
JobExecutor               ✅
ACT_* MySQL               ✅
REST starter              ✅
BPMN Rascomp              ⏳
```

Primeiro processo candidato:

```text
PARTICIPANTE envia inscrição
        ↓
      PENDENTE
        ↓
tarefa da ORGANIZACAO
     ↙       ↘
APROVADA   REJEITADA
```

Camunda deve orquestrar processos/human tasks. Regras competitivas permanecem nos services Java.

---

## 13. Próxima etapa — Swagger/OpenAPI

Critério de aceite:

```text
/v3/api-docs                         ✅
/swagger-ui/index.html               ✅
metadata da API                      ✅
tags por domínio                     ✅
endpoints descritos                  ✅
path/query params                    ✅
request bodies                       ✅
200/201/204                          ✅
400/401/403/404/405/409/415          ✅
DTO schemas                          ✅
JSON examples                        ✅
Bearer JWT                           ✅
upload multipart                    ✅
API pública/participante/admin       ✅ separadas visualmente
MatchResult read-only                ✅ explícito
```

O Swagger não deve mudar regras de domínio, nomes de endpoints ou semântica HTTP apenas para melhorar a apresentação.

---

## 14. Sequência daqui para frente

```text
Arquitetura usuários/acesso          ✅ mergeada
          ↓
CI pós-merge                         ✅
          ↓
novo congelamento da API             ✅
          ↓
Swagger / OpenAPI                    ◀ PRÓXIMA ETAPA
          ↓
revalidação ORGANIZACAO              ⏳ no fechamento
          ↓
revisão final do backend
          ↓
checkpoint Camunda
          ↓
Frontend de Gestão + Landing
```

O frontend pode avançar em paralelo usando:

```text
Gestão:
/api/v1/auth/**
/api/v1/participante/**
/api/v1/** -> ORGANIZACAO

Landing:
/api/v1/public/**
```

`CONTINUIDADE.md` permanece a fonte principal para qualquer nova sessão/agente que assuma o projeto.