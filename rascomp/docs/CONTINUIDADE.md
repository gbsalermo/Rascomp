# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-24T00:08:00-03:00

## 1. Marco atual

O núcleo competitivo do backend já foi validado. Antes do Swagger, o congelamento da API foi reaberto de forma excepcional para corrigir uma lacuna arquitetural indispensável: **usuários reais, autenticação, ownership, participantes das inscrições e separação entre operação administrativa e portal do participante**.

A implementação dessa correção está concentrada na branch:

```text
arquitetura-usuarios-acesso
```

PR:

```text
#4 — Arquitetura de usuários, ownership e acesso
```

Estado atual:

```text
NÚCLEO COMPETITIVO
MySQL/Flyway V1–V4                    ✅
FOLLOW_LINE                           ✅ validado manualmente
SUMO                                  ✅ validado manualmente
testes anteriores                     ✅
branch testes-automatizados           ✅ mergeada na main

ARQUITETURA DE ACESSO
UserAccount                           ✅ implementado
PARTICIPANTE / ORGANIZACAO            ✅ implementado
JWT + BCrypt                          ✅ implementado
responsável da equipe                 ✅ implementado
UserAccount ↔ Competitor opcional     ✅ implementado
Registration + competidores           ✅ implementado
auditoria solicitante/revisor         ✅ implementado
fotos dos robôs                       ✅ implementado
API participante + ownership          ✅ implementado
API pública sanitizada                ✅ implementado
Migration V5                          ✅ criada

QUALIDADE
novos testes                          ✅
GitHub Actions                        ✅ 28 testes / 0 falhas
smoke local MySQL + V5 + JWT          ⏳ próximo checkpoint
upload de foto local                  ⏳ próximo checkpoint
PR #4                                 🔄 draft até smoke

PRÓXIMA FASE
novo congelamento da API              ⏳ após merge
Swagger/OpenAPI                       ⏳ depois do congelamento
Camunda BPMN funcional                ⏳ checkpoint pós-Swagger
Frontend de Gestão                    ⏳ trabalho paralelo
Landing / Frontend Público            ⏳ trabalho paralelo
```

O README foi atualizado no mesmo marco com a identidade visual oficial do projeto e o estado arquitetural atual.

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

## 3. Dois tipos de usuário

O projeto utiliza somente dois papéis globais:

```text
PARTICIPANTE
ORGANIZACAO
```

### PARTICIPANTE

Conta criada pelo próprio usuário.

Pode:

- registrar conta e fazer login;
- consultar seu próprio usuário em `/auth/me`;
- criar e gerenciar apenas as equipes pelas quais é responsável;
- cadastrar competidores da equipe;
- opcionalmente se vincular como `Competitor`;
- cadastrar robôs;
- enviar fotos dos robôs;
- criar e cancelar inscrições da própria equipe;
- escolher os competidores que participarão daquela inscrição;
- acompanhar status das inscrições.

### ORGANIZACAO

Conta administrativa do evento.

Pode operar:

- usuários da organização;
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
- BCrypt é irreversível para o uso normal da aplicação;
- autenticação é stateless com JWT.

Endpoints:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

JWT:

```text
Authorization: Bearer <token>
```

Segredo externo:

```text
JWT_SECRET
```

Nunca versionar segredos.

Primeiro usuário `ORGANIZACAO` pode ser criado por bootstrap opcional quando ainda não existir conta administrativa.

Não foi implementado public ID/UUID. IDs numéricos permanecem adequados ao escopo atual; segurança depende de autenticação e autorização, não da ocultação do ID.

---

## 5. Ownership e modelo operacional

### Team

`Team` possui `responsibleUser` opcional para compatibilidade com dados legados.

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

`Competitor` continua representando a pessoa que compete, não a conta de login.

Pode existir sem conta.

Opcionalmente:

```text
Competitor.userAccount -> UserAccount(PARTICIPANTE)
```

Cenários suportados:

```text
professor responsável: UserAccount ✅ / Competitor ❌
competidor líder:       UserAccount ✅ / Competitor ✅
competidor cadastrado:  UserAccount opcional / Competitor ✅
```

### Registration

A inscrição agora registra:

```text
competition
category
team
robot
competitors participantes
requestedByUser
reviewedByUser
reviewedAt
status
observacao
ativo
```

Toda inscrição criada pelo portal participante nasce:

```text
PENDENTE
```

Aprovação/rejeição é responsabilidade de `ORGANIZACAO`.

O backend valida que os competidores informados pertencem à mesma equipe da inscrição.

---

## 6. Fotos dos robôs

Nova entidade `RobotImage` armazena metadados:

```text
robotId
storageKey
originalFilename
contentType
principal
ordem
ativo
dataCadastro
```

Os bytes ficam fora do MySQL.

MVP:

```text
armazenamento local
JPEG / PNG / WEBP
máximo 5 MB
```

Diretório configurável:

```text
ROBOT_IMAGES_DIR
```

`/uploads/` está ignorado pelo Git.

A arquitetura do storage permite substituição futura por S3/cloud sem mudar o domínio de `Robot`.

---

## 7. Separação das APIs

### Público — sem login

```text
/api/v1/public/**
```

Somente leitura e DTOs sanitizados.

Pode alimentar a landing page com:

- competições;
- equipes;
- competidores com dados públicos;
- robôs;
- fotos;
- inscrições aprovadas;
- ranking Follow;
- chaveamentos Sumô;
- partidas/resultados.

Não expõe:

- senha/hash;
- e-mail/telefone de competidores;
- responsável da equipe;
- observações administrativas;
- usuário solicitante/revisor.

### Participante — JWT `PARTICIPANTE`

```text
/api/v1/participante/**
```

Ownership validado pelo backend.

O frontend não escolhe livremente:

- `teamId` de outra equipe;
- responsável;
- role;
- status administrativo;
- usuário solicitante;
- `ativo` administrativo.

Esses valores são determinados ou validados pelo backend.

### Organização — JWT `ORGANIZACAO`

```text
/api/v1/**
```

Endpoints internos de operação administrativa.

Camunda REST:

```text
/engine-rest/** -> ORGANIZACAO
```

Swagger/OpenAPI permanece liberado durante desenvolvimento.

---

## 8. Persistência

Migrations atuais:

```text
V1__create_rascomp_schema.sql
V2__create_inspecoes_sumo.sql
V3__create_rounds_sumo.sql
V4__remove_follow_line_brackets.sql
V5__add_users_team_ownership_and_robot_images.sql
```

Nunca editar V1–V5 depois de aplicadas.

Próxima alteração estrutural:

```text
V6+
```

---

## 9. Domínio competitivo — contrato já validado

### FOLLOW_LINE

Fluxo oficial:

```text
Registration APROVADA
    -> ConfigFollow
    -> 3 tomadas
    -> até 3 tentativas por tomada
    -> TentativaSeguidorLinha
    -> melhor tentativa válida e concluída
    -> RankingFollowService
    -> classificação por menor tempo final
```

Cálculo:

```text
tempoFinal = tempoSegundos + penalidadeSegundos
```

Regras manualmente validadas:

- 3 tomadas × 3 tentativas;
- limites de tomada e tentativa;
- checkpoints;
- duplicidade;
- tempo máximo;
- tentativa inválida ignorada;
- tentativa não concluída ignorada;
- penalidade aplicada;
- ranking correto;
- inscrição SUMO rejeitada no Follow;
- categoria SUMO rejeitada no ranking Follow;
- bracket bloqueado para Follow.

`FOLLOW_LINE` não usa:

```text
Bracket
Match
MatchResult
RoundSumo
```

### SUMO

Fluxo oficial:

```text
Registration APROVADA
    -> InspecaoSumo
    -> aptidão
    -> Bracket
    -> Match
    -> RoundSumo
    -> MatchResult automático
    -> avanço do vencedor
    -> encerramento do Bracket
```

Regras manualmente validadas:

- ConfigSumo;
- inspeções;
- reprovação/desclassificação;
- aptidão;
- filtro de participantes aptos;
- bracket exclusivo Sumô;
- BYE;
- rounds;
- empate;
- `roundsParaVencer`;
- MatchResult automático;
- Match FINALIZADA;
- Bracket FINALIZADO;
- MatchResult somente leitura;
- POST/PUT/DELETE externos retornando 405.

A introdução de usuários e ownership não altera essas regras.

---

## 10. Testes automatizados

A regressão anterior permanece ativa e foram adicionados testes para a nova arquitetura.

Última execução validada no GitHub Actions após a correção do teste de ownership:

```text
Backend Tests
28 testes
0 falhas
0 erros
SUCCESS
```

Cobertura relevante:

- `CompetitionCategoryService`;
- `TentativaSeguidorLinhaService`;
- `RankingFollowService`;
- `InspecaoSumoService`;
- `BracketService`;
- `BracketGenerationService`;
- `MatchService`;
- `MatchResultService`;
- `UserAccountService`;
- `AccessPolicyService`;
- ownership de equipe;
- inscrição com competidores/ownership.

CI sozinho não encerra a etapa: ainda é necessário smoke com MySQL real, V5, JWT e arquivo.

---

## 11. Camunda

Infraestrutura:

```text
Camunda 7.22         ✅
Process Engine       ✅
JobExecutor          ✅
tabelas ACT_*        ✅
REST starter         ✅
BPMN Rascomp         ⏳
```

A arquitetura de usuários tornou o primeiro BPMN mais concreto:

```text
PARTICIPANTE
    ↓
envia Registration
    ↓
PENDENTE
    ↓
tarefa humana da ORGANIZACAO
   ↙                       ↘
APROVADA                 REJEITADA
```

Camunda deverá orquestrar processo humano/auditoria.

As regras de competição permanecem em Java:

```text
RankingFollowService
InspecaoSumoService
BracketGenerationService
RoundSumoService
BracketProgressionService
...
```

Decisão sobre BPMN funcional permanece para o checkpoint depois do Swagger.

---

## 12. Próximo checkpoint — sessão seguinte

Antes de qualquer trabalho de Swagger:

```text
1. checkout/pull da branch arquitetura-usuarios-acesso
2. subir aplicação com MySQL
3. confirmar Flyway V5
4. registrar PARTICIPANTE
5. confirmar password_hash BCrypt no banco, nunca senha pura
6. login -> JWT
7. GET /api/v1/auth/me
8. criar equipe pelo participante
9. confirmar responsibleUser automático
10. cadastrar competidor e robô
11. testar vínculo "eu como competidor"
12. subir uma foto do robô
13. criar inscrição com competidores
14. confirmar inscrição PENDENTE + requestedByUser
15. testar ownership negativo com outro participante
16. testar 401 sem token
17. testar 403 com perfil incorreto
18. acessar fluxo administrativo como ORGANIZACAO
19. confirmar endpoint público sem dados sensíveis
20. smoke rápido Follow/Sumô para regressão
```

Se todos passarem:

```text
PR #4 -> ready
      ↓
merge em main
      ↓
novo congelamento definitivo da API
      ↓
SWAGGER / OPENAPI
```

Se algum teste falhar, parar no primeiro problema, registrar request/response/log e corrigir antes do merge.

---

## 13. Frontends em paralelo

O Frontend de Gestão e o esqueleto da Landing Page podem avançar em paralelo em outro fluxo de trabalho.

Orientação para esses trabalhos:

```text
Frontend de Gestão
 -> consumir /api/v1/auth/**
 -> PARTICIPANTE usa /api/v1/participante/**
 -> ORGANIZACAO usa endpoints administrativos
 -> preparar integração futura com Swagger/OpenAPI

Landing / Frontend Público
 -> consumir exclusivamente /api/v1/public/**
 -> nenhuma dependência de dados administrativos/sensíveis
```

Até o merge do PR #4, considerar a API ainda em validação arquitetural. Evitar fixar contratos frontend irreversíveis antes do novo congelamento.

---

## 14. Roadmap atualizado

```text
NÚCLEO COMPETITIVO                    ✅
Follow + Sumô                         ✅
Testes manuais                        ✅
Testes automatizados                  ✅

ARQUITETURA DE USUÁRIOS               ✅ implementação
JWT + BCrypt                          ✅
Ownership                             ✅
API participante/pública              ✅
V5                                    ✅
CI 28/28                              ✅
            ↓
SMOKE LOCAL                           ◀ PRÓXIMO
            ↓
MERGE PR #4
            ↓
NOVO CONGELAMENTO DA API
            ↓
SWAGGER / OPENAPI
            ↓
REVISÃO FINAL DO BACKEND
            ↓
CHECKPOINT CAMUNDA
   ├─ BPMN mínimo agora?
   ├─ integração com gestão?
   └─ adiar até MVP visual?
            ↓
Frontend de Gestão + Landing
            ↓
MVP
```

Objetivo imediato: **fechar a nova arquitetura, documentar no Swagger e considerar o backend definitivamente pronto para integração dos frontends**.

---

## 15. Forma de trabalho

O projeto é conduzido por orquestração:

```text
objetivo
 -> implementação delegada
 -> revisão de arquitetura/diff
 -> CI
 -> smoke objetivo
 -> documentação
 -> merge
 -> próxima etapa
```

`CONTINUIDADE.md` continua sendo a fonte principal do estado do projeto para novas sessões, IAs ou colaboradores.
