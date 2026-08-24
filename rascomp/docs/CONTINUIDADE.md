# Continuidade do Projeto — Rascomp

Última atualização: 2026-08-23T23:40:00-03:00

## 1. Marco atual

O núcleo competitivo do backend já foi validado, mas o congelamento da API foi reaberto **antes do Swagger** para corrigir uma lacuna arquitetural indispensável: identidade, autenticação e ownership dos usuários reais do sistema.

Situação atual:

```text
MySQL/Flyway V1–V4                  ✅
FOLLOW_LINE                         ✅ validado
SUMO                                ✅ validado
testes anteriores                   ✅
branch testes-automatizados         ✅ mergeada

arquitetura de usuários             🔄 PR #4
JWT + BCrypt                        ✅ implementado
PARTICIPANTE / ORGANIZACAO          ✅ implementado
responsável da equipe               ✅ implementado
UserAccount ↔ Competitor opcional   ✅ implementado
Registration + competidores/auditoria ✅ implementado
fotos dos robôs                     ✅ implementado
API participante + ownership        ✅ implementado
API pública sanitizada              ✅ implementado
Migration V5                        ✅ criada
testes novos                        🔄 validação CI
smoke local auth + V5               ⏳ antes do merge
Swagger/OpenAPI                     ⏳ depois do novo congelamento
```

Branch atual:

```text
arquitetura-usuarios-acesso
```

PR atual:

```text
#4 — Arquitetura de usuários, ownership e acesso
```

---

## 2. Dois tipos de usuário

O projeto utiliza somente dois papéis globais:

```text
PARTICIPANTE
ORGANIZACAO
```

### PARTICIPANTE

Conta criada pelo próprio usuário.

Pode:

- fazer login;
- ser responsável por uma ou mais equipes;
- gerenciar apenas as equipes pelas quais é responsável;
- cadastrar competidores da equipe;
- opcionalmente se vincular como `Competitor`;
- cadastrar robôs;
- enviar fotos dos robôs;
- enviar e cancelar inscrições da equipe;
- acompanhar o status das inscrições.

### ORGANIZACAO

Conta administrativa do evento.

Pode operar:

- usuários da organização;
- instituições;
- equipes;
- competidores;
- robôs/fotos;
- competições e categorias;
- inscrições e análise;
- inspeção Sumô;
- chaveamentos/partidas/rounds;
- tentativas e ranking Follow;
- demais endpoints internos de gestão.

Responsabilidade de equipe **não é role global**. É relacionamento:

```text
Team.responsibleUser -> UserAccount(PARTICIPANTE)
```

---

## 3. Identidade e segurança

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

A senha original não é persistida e nenhum DTO de resposta expõe `passwordHash`.

Autenticação:

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

Não versionar segredos.

Primeiro usuário `ORGANIZACAO` pode ser criado por bootstrap opcional via variáveis de ambiente quando ainda não existir conta administrativa.

---

## 4. Ownership e modelo operacional

### Team

Agora possui `responsibleUser` opcional para compatibilidade com dados legados.

Novas equipes criadas pelo portal do participante recebem automaticamente o usuário autenticado como responsável.

### Competitor

Continua representando a pessoa que compete.

Pode existir sem conta de login.

Opcionalmente:

```text
Competitor.userAccount -> UserAccount(PARTICIPANTE)
```

Isso permite:

```text
professor responsável: UserAccount ✅ / Competitor ❌
competidor líder:       UserAccount ✅ / Competitor ✅
competidor cadastrado:  UserAccount opcional / Competitor ✅
```

### Registration

Agora registra:

- robô;
- equipe;
- competição/categoria;
- competidores participantes;
- `requestedByUser`;
- `reviewedByUser`;
- `reviewedAt`;
- status.

Toda inscrição criada pelo portal participante nasce `PENDENTE`.

Aprovação/rejeição fica sob `ORGANIZACAO`.

---

## 5. Fotos dos robôs

Nova entidade `RobotImage` armazena apenas metadados:

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

MVP atual:

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

---

## 6. Separação das APIs

### Público — sem login

```text
/api/v1/public/**
```

Somente leitura e DTOs sanitizados.

Não expõe:

- senha/hash;
- e-mail/telefone de competidores;
- responsável da equipe;
- observações administrativas;
- usuário solicitante/revisor.

### Participante — JWT PARTICIPANTE

```text
/api/v1/participante/**
```

Ownership validado pelo backend. Um participante não pode alterar dados de equipe de outro responsável.

### Organização — JWT ORGANIZACAO

```text
/api/v1/**
```

Endpoints internos existentes passam a ser administrativos.

Camunda REST:

```text
/engine-rest/** -> ORGANIZACAO
```

Swagger/OpenAPI continua acessível sem login durante desenvolvimento.

---

## 7. Persistência

Migrations:

```text
V1__create_rascomp_schema.sql
V2__create_inspecoes_sumo.sql
V3__create_rounds_sumo.sql
V4__remove_follow_line_brackets.sql
V5__add_users_team_ownership_and_robot_images.sql
```

Nunca editar V1–V5 depois de aplicadas. Próxima mudança estrutural usa V6.

Não foi adicionado public ID; IDs numéricos continuam sendo usados no escopo atual.

---

## 8. Domínio competitivo continua intacto

### FOLLOW_LINE

```text
Registration APROVADA
 -> ConfigFollow
 -> TentativaSeguidorLinha
 -> melhor tentativa válida/concluída
 -> RankingFollowService
```

Não usa `Bracket`, `Match`, `RoundSumo` ou `MatchResult`.

### SUMO

```text
Registration APROVADA
 -> InspecaoSumo
 -> aptidão
 -> Bracket
 -> Match
 -> RoundSumo
 -> MatchResult automático
```

`MatchResult` continua somente leitura externamente.

---

## 9. Camunda

Estado:

```text
Engine          ✅
JobExecutor     ✅
ACT_*           ✅
REST starter    ✅
BPMN Rascomp    ⏳
```

A nova arquitetura torna o primeiro BPMN concreto:

```text
PARTICIPANTE envia Registration
        -> PENDENTE
        -> tarefa ORGANIZACAO
        -> APROVADA ou REJEITADA
```

Camunda orquestra processo humano. Regras de Follow/Sumô permanecem nos services Java.

---

## 10. Critério de saída desta correção

Antes do merge do PR #4:

```text
mvn test / GitHub Actions             ✅ necessário
BCrypt sem senha em texto puro        ✅ teste
ownership                             ✅ teste
Registration/autoria/competidores     ✅ teste
regressão Follow/Sumô                  ✅ suíte existente
startup MySQL + Flyway V5              ⏳ smoke manual
register/login/me                      ⏳ smoke manual
PARTICIPANTE x ORGANIZACAO             ⏳ smoke manual
upload de uma foto                     ⏳ smoke manual
```

Depois:

```text
merge PR #4
 -> novo congelamento da API
 -> Swagger / OpenAPI
 -> checkpoint Camunda x Frontend de Gestão
```

---

## 11. Forma de trabalho

O projeto agora é conduzido por orquestração:

```text
objetivo
 -> implementação delegada
 -> revisão de arquitetura/diff
 -> CI
 -> smoke objetivo
 -> documentação
 -> merge
```

A documentação deve permitir continuidade sem depender da memória da conversa.
