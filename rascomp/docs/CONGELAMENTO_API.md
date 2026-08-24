# Congelamento da API — Rascomp

## Histórico do marco

Em 23/08/2026 o núcleo competitivo do backend foi validado e congelado:

```text
FOLLOW_LINE                 ✅
SUMO                        ✅
MySQL/Flyway                ✅
testes manuais              ✅
testes automatizados        ✅
CI                          ✅
```

Logo antes do Swagger foi identificada uma lacuna arquitetural indispensável: o sistema possuía competidores, equipes e inscrições, mas não possuía identidade/autenticação dos atores que realmente utilizariam o Frontend de Gestão.

Por isso o congelamento foi **reaberto de forma excepcional** antes do Swagger.

---

## Correção arquitetural autorizada

Branch:

```text
arquitetura-usuarios-acesso
```

PR:

```text
#4 — Arquitetura de usuários, ownership e acesso
```

A correção inclui:

- `UserAccount` separado de `Competitor`;
- dois perfis globais: `PARTICIPANTE` e `ORGANIZACAO`;
- senha persistida somente como hash BCrypt;
- JWT stateless para autenticação;
- responsável da equipe ligado a uma conta `PARTICIPANTE`;
- vínculo opcional `Competitor -> UserAccount`;
- competidores efetivamente associados à `Registration`;
- autoria e revisão da inscrição;
- upload e metadados de fotos de robôs;
- ownership centralizado para impedir participante de alterar equipe alheia;
- API autenticada do participante;
- API de organização protegida;
- API pública separada e sanitizada;
- migration incremental `V5`.

Não foram adicionados public IDs. Os IDs numéricos atuais continuam suficientes para o escopo e prazo do projeto.

---

## Modelo de acesso consolidado

```text
PARTICIPANTE
  -> cria/login em conta própria
  -> pode ser responsável por uma ou mais equipes
  -> gerencia apenas equipes sob sua responsabilidade
  -> cadastra competidores da equipe
  -> pode vincular a própria conta a um Competitor
  -> cadastra robôs e fotos
  -> envia/cancela inscrições da própria equipe

ORGANIZACAO
  -> gerencia a operação completa do evento
  -> equipes, competidores, robôs e inscrições
  -> aprova/rejeita inscrições
  -> inspeção de Sumô
  -> chaveamentos, partidas e rounds
  -> tentativas/ranking Follow
  -> administra contas da organização

PUBLICO
  -> somente leitura
  -> sem dados privados de usuário/competidor
```

---

## Senhas e autenticação

O banco nunca recebe a senha em texto puro.

```text
senha recebida no cadastro
    -> BCryptPasswordEncoder(12)
    -> password_hash
    -> senha original descartada
```

`UserAccountDTO` nunca expõe `passwordHash`.

JWT usa segredo externo ao Git:

```text
JWT_SECRET
```

Produção deve utilizar HTTPS; JWT não substitui criptografia de transporte.

---

## Persistência

Migrations aplicadas anteriormente não devem ser alteradas:

```text
V1
V2
V3
V4
```

A nova arquitetura entra exclusivamente por:

```text
V5__add_users_team_ownership_and_robot_images.sql
```

Qualquer nova mudança estrutural posterior deve usar `V6`, `V7`, etc.

---

## Regra de domínio competitivo continua congelada

### FOLLOW_LINE

```text
ConfigFollow
 -> TentativaSeguidorLinha
 -> melhor tentativa válida/concluída
 -> RankingFollowService
```

FOLLOW_LINE não utiliza `Bracket`, `Match`, `RoundSumo` ou `MatchResult`.

### SUMO

```text
InspecaoSumo
 -> aptidão
 -> Bracket
 -> Match
 -> RoundSumo
 -> MatchResult automático
```

`MatchResult` continua somente leitura externamente.

A correção de usuários não autoriza redesenhar essas regras.

---

## Novo critério para congelar novamente

Antes de Swagger, a branch de arquitetura precisa concluir:

```text
compilação/CI                         ✅ obrigatório
BCrypt/JWT                            ✅ testes
ownership de equipe                   ✅ testes
Registration + competidores/autoria  ✅ testes
regressão Follow/Sumô                 ✅
smoke de autenticação                 ⏳ manual antes do merge
migration V5/startup                  ⏳ manual antes do merge
```

Depois do merge do PR #4, o contrato será congelado novamente e Swagger poderá documentar corretamente:

- endpoints públicos;
- endpoints do participante;
- endpoints da organização;
- Bearer JWT;
- respostas 401 e 403;
- uploads multipart;
- DTOs sanitizados do frontend público.

---

## Camunda

Camunda continua como infraestrutura validada:

```text
Engine          ✅
JobExecutor     ✅
ACT_*           ✅
REST starter    ✅
BPMN Rascomp    ⏳
```

A nova rastreabilidade da inscrição (`requestedByUser`, `reviewedByUser`, `reviewedAt`) prepara o primeiro fluxo BPMN:

```text
PARTICIPANTE envia inscrição
        -> PENDENTE
        -> tarefa da ORGANIZACAO
        -> APROVADA ou REJEITADA
```

As regras de Follow/Sumô permanecem nos services Java.

---

## Próxima etapa após o novo congelamento

```text
Swagger / OpenAPI
```

O Swagger não deve começar enquanto o PR #4 não estiver validado e mergeado.
